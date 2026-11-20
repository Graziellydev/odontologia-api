# Design

## Context

See `proposal.md` and the four capability specs for product behavior. The project has Spring Boot/JPA with `spring.jpa.hibernate.ddl-auto=validate`, an existing `consultas` model and query for the authenticated student's day appointments (`GET /api/retornos/meus-atendimentos`), and a React/Vite student area. There is no existing prontuário implementation.

The deployed schema is the source of truth. V6 defines `prontuarios` with required patient, student, consultation, class, procedure, clinical evolution, signature and timestamp columns; `consulta_id` is unique. V8 blocks UPDATE/DELETE on both `prontuarios` and `prontuario_adendos` and records consultation status changes through `trg_consulta_historico`. V9 permits multiple `paciente_anamnese` rows per patient and makes the patient/tooth/face odontogram key unique. No migration or entity field may be invented to satisfy this change.

The inspected Figma node `4:46292` is a 1440×1704 student clinical evolution screen: 248 px profile navigation, 1192 px work area, 82 px institutional bar, 40 px content padding, patient/appointment header, 73 px green authorization notice, patient and appointment summary, two tabs, chronological read-only history, evolution panels and a repeated diagonal session watermark. The frame uses Lato, background `#F7F8FA`, text `#263238`/`#657278`, orange `#C65117`, border `#E3E8EB`, green `#EAF5EE`/`#287252` and white panels with 8–12 px corner radii. It depicts a wide page, not a drawer. Its block-demo buttons expose network/turn states and an addendum action exists, but the inspected node does not provide standalone modal frames or exact modal dimensions. The requested contextual drawer and compact modal states therefore adapt this hierarchy and tokens; their geometry must be responsive and is not claimed as a measured Figma value.

The current `Perfil` enum and `usuarios.perfil` constraint accept `ALUNO`, `COORDENADOR` and `SECRETARIA`, not `ADMINISTRADOR`. Audit read access uses the existing `COORDENADOR` role, as decided for this proposal.

## Goals / Non-Goals

**Goals:**

- Add the read and write behavior using entities/repositories mapped exactly to V6/V9 and existing consultation data.
- Make IP, consultation link and current-turn checks server-side and unavoidable for all clinical endpoints.
- Keep evolution/adendum writes append-only and status updates attributable to the student in the database trigger.
- Reuse the existing student's today-attendance source and integrate the record in the existing student workflow.
- Match the Figma visual language while implementing the user-requested drawer/modal interaction and visual LGPD safeguards.

**Non-Goals:**

- Add columns, tables, constraints, or database roles; alter Flyway history; or change the meaning of existing constraints.
- Claim a certificate-backed or legally qualified digital signature. The requested SHA-256 value is an application integrity/authorship seal.
- Make an adendum an edit to the signed evolution or offer coordinators write access.
- Treat client-side watermarking or print hiding as a substitute for server authorization.

## Decisions

1. **Map existing columns and use consultation as the authority for ownership.** Create JPA mappings/projections for `prontuarios`, `prontuario_adendos`, `paciente_anamnese` and `paciente_odontograma` using only columns in V6/V9. Load the consultation by `consultaId`; derive patient, student and class from that row and the authenticated user rather than trusting client-supplied ownership fields. Require the request procedure to be non-null and validate it against the consultation's procedure when one is present. Because V6 requires `procedimento_id` on the prontuário while V5 allows it to be null on a consultation, reject a consultation with no resolvable procedure rather than storing a null or adding schema. Return conflict if a consultation already has a prontuário. Map database constraint/unique failures to stable 400/409 responses.

2. **Centralize clinical authorization before returning data or mutating it.** Protect `/api/prontuarios/**` in `SecurityConfig` with the `ALUNO`/`COORDENADOR` authority boundary, then apply the local-network validator before controller/service work. For students, authorization checks the consultation/patient ID, authenticated student ID, `vagas.data = CURRENT_DATE`, and strict time window. Initial evolution creation accepts `AGENDADO` and `CONFIRMADA_24H`; history reads and addenda also accept directly linked `REALIZADO` consultations for the same date/turn. For coordinators, only GET history is permitted and local-network validation still applies. Parse a forwarded IP only from a configured trusted proxy; otherwise use servlet `remoteAddr`, so an arbitrary client cannot bypass CIDR checks by forging `X-Forwarded-For`. Serialize 403 messages with the project's JSON error response convention.

3. **Keep temporal and IP rules explicit and configurable.** Bind `app.prontuario.allowed-cidrs` and `app.prontuario.validar-turno-estrito` from `application.properties`; include loopback ranges in the default CIDR list and provision clinic LAN ranges per environment. Interpret `CURRENT_DATE` using the configured database/application clinic timezone (São Paulo for this project). When strict mode is enabled, use half-open intervals `[08:00,12:00)` for `MANHA` and `[13:00,17:00)` for `TARDE`; when disabled, skip only the wall-clock check, never network, current-date, status or ownership checks. A later deployment can configure trusted proxy addresses alongside the CIDRs if the application sits behind a reverse proxy.

4. **Query the history without widening personally identifiable data.** The history response includes only basic patient identity needed to orient the treatment, not CPF, contact/address or the patient's other demographic fields. Query the most recently updated anamnese row because V9 does not enforce a single row per patient; break ties with `criado_em`. Return all current odontogram rows, chronological evolutions sorted newest-first, and adendos nested under each record in creation order. Include the active appointment selected by the same student/date/status rules used for authorization. Keep the coordinator response read-only.

5. **Generate a deterministic, versioned server-side integrity seal.** Before persisting, serialize the consultation, procedure, optional tooth/class, trimmed clinical text, student database ID and matricula/registro in a fixed UTF-8 canonical field order. Generate one server timestamp with offset, include its ISO-8601 form and the signature format version with the SHA-256 hex digest in `assinatura_digital`. Persist the exact same timestamp and clinical values used for the digest. Never accept signature data from the browser. This provides tamper-evident content fingerprinting plus attributable application metadata; without a private signing key it is not a cryptographic identity signature.

6. **Commit the evolution, optional odontogram observation and completion atomically.** In one transaction, validate all request content, insert the prontuário and any supplied odontogram observation, and, only when `concluirAtendimento=true`, set transaction-local PostgreSQL values `app.origem='ALUNO'` and `app.usuario_id=<authenticated id>` before changing `consultas.status_consulta` to `REALIZADO`. Reuse the established `EntityManager`/`set_config(..., true)` pattern in `RetornoService`. Let V8 triggers enforce append-only clinical records and create status history. Upsert an odontogram observation on the existing patient/tooth/face unique key (treat null face as `GERAL` for key comparison); do not issue UPDATE/DELETE against prontuário or adendo tables. A failed database operation rolls back the complete transaction. Save an adendum in its own insert-only transaction with `autor_id` from the authenticated user.

7. **Integrate in the current panel and adapt Figma components to modular CSS.** Add the patient-of-the-turn entry point to the existing `PainelAluno` flow, sourcing today's appointments through the established retorno service API. Open the broad record drawer/modal from that context; retain the reference's summary header, two tabs, chronological history, form panels, button hierarchy, spacing, colors and typography. Use a smaller centered modal for `+ Adicionar Adendo` and a compact red alert for HTTP 403 network/turn blocks, using existing red tokens `#B43B39` and `#FFF0EF`. The inspected Figma node does not establish those modal dimensions, so size them to content with viewport bounds and internal scrolling rather than reproducing an unobserved pixel measurement. Keep component styles local to the student module; do not introduce Tailwind.

8. **Treat visual safeguards as deterrents and keep them scoped.** Overlay repeated diagonal watermark text derived from the logged-in user's display name and `matricula_ou_registro` (the current `usuarios` schema has no student CPF field) plus the current date/time. Set `pointer-events:none` and apply `user-select:none` only to the clinical content region, leaving buttons and form controls operable. Hide the clinical region and watermark in `@media print`. Enforce all real authorization on the server because these browser protections can be bypassed.

## Risks / Trade-offs

- [A forged or misinterpreted forwarded-IP header could defeat the network gate] → Honor forwarded addresses only from configured trusted proxies and test direct-client and proxy paths separately.
- [The PostgreSQL `CURRENT_DATE` timezone could differ from the UI clock] → Configure the clinic timezone consistently and use it for the database date and application turn clock.
- [A SHA-256 seal without a private key is not a public-key signature] → Label it as a temporal integrity/authorship seal in API/UI and do not claim non-repudiation beyond authenticated account attribution and immutable storage.
- [Existing history may include several anamnese rows and older records may have nullable clinical references] → Select the latest anamnese deterministically and make response fields nullable where the database permits it.
- [The current visual reference is a full-page screen, while the requested interaction is a drawer] → Preserve its content hierarchy and design tokens inside the contextual drawer; review modal fit at desktop and narrow widths during implementation.
- [Hiding print and selection does not prevent screenshots or developer tools] → Keep authorization and data minimization server-side; describe RNF003 as visual exposure reduction.

## Migration Plan

No database migration is planned. Map only existing tables and columns and validate mappings with the configured `ddl-auto=validate`. Deploy backend CIDR defaults plus environment-specific clinic ranges and trusted-proxy configuration before enabling the UI. Deploy the protected endpoints, then the student panel flow. Roll back by disabling/hiding the panel entry point and reverting application changes; no clinical schema rollback is needed. Existing records remain untouched.
