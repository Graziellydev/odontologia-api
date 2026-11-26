# Design

## Context

See `proposal.md` for motivation. `application.properties` currently enables `ddl-auto=validate` and `baseline-on-migrate=true`; the prontuário authorization service embeds 08:00–12:00 and 13:00–17:00. `pacientes` has V9/V13 columns absent from `Paciente`, while the controller exposes that entity directly. `Consulta` and `ConsultaRetorno` both map `consultas` and declare `@Version`.

The Git history shows all V2–V9 files changed after their initial addition. The precise baseline used by Supabase must be established from the repository revision that matches its Flyway history before any file is restored. `.env` is out of scope for all inspection and changes.

## Goals / Non-Goals

**Goals:**

- Externalize clinical time windows with typed, validated configuration.
- Make existing patient schema data round-trip through the API and pass Hibernate 6 validation.
- Establish one versioned JPA aggregate for `consultas` mutations.
- Reconcile Flyway checksums without rewriting a migration already known to be applied, except to restore its original bytes.
- Apply a single authority vocabulary at route and method layers.

**Non-Goals:**

- Changing the clinical meaning of MANHA/TARDE, database credentials, authentication/session design, or public route paths.
- Backfilling optional patient data or altering physical-fiche tables beyond columns already defined.
- Adding an unrelated database feature merely to produce V19.

## Decisions

### Bind turn windows through the prontuário configuration object

Add four `LocalTime` properties under `app.prontuario.turno-manha` and `turno-tarde`, defaulted in `application.properties` to 07:30/12:00 and 13:30/18:00. Validate `inicio < fim`; use the configured windows only in strict validation and retain the clinic time zone.

This keeps deployment-time operational changes outside the build artifact. Environment variables were not chosen as the primary contract because the requested application properties are discoverable, testable and profile-overridable.

### Introduce explicit patient request/response DTOs and a complete entity map

Map every V9/V13 patient column with exact names and lengths. Apply Hibernate's explicit fixed-character JDBC type to `uf` and `sexo`. Replace direct entity binding at patient boundaries with create, update and detail DTOs, and centralize entity mapping in the patient service; triage flows that return patient data use the same detail projection.

DTOs prevent clients from assigning audit, status, counters and internal fields while making physical-fiche coverage auditable. Continuing to bind the entity was rejected because it couples persistence fields to input and makes incomplete updates easy to miss.

### Make `Consulta` the sole mutable JPA entity for `consultas`

Move return-specific repository/service operations to `Consulta` and remove `ConsultaRetorno`. Retain the same `@Version` field and use the existing status enum in the return flow. Reads needing joined display fields remain native/projection queries; writes load the canonical entity and flush it in the transaction that sets history context.

JPA inheritance or a read-only second entity was rejected: it provides no benefit for the current write flow and risks version/mapping ambiguity. Native updates were rejected because they bypass optimistic version enforcement.

### Reconcile migrations from immutable Git evidence and evolve only through V19

For each V2–V9 file, use its introduction revision (`6ca080d050d07d98042976682b1867361bcd361b`) as the expected original bytes, then compare it with the version/checksum stored in the Supabase Flyway history before changing files. Do not edit these files to add comments, constraints or repairs. Set `spring.flyway.baseline-on-migrate=false` in base and applicable local profiles. If reconciliation identifies a genuinely missing schema correction, add one `V19__...sql` using PostgreSQL conditional DDL (`IF NOT EXISTS` and guarded constraint creation).

`repair` is not the default remedy because it would bless unverified bytes instead of restoring historical integrity. No V19 is created when no structural delta remains after reconciliation.

### Enforce canonical authorities in two layers

Use `SECRETARIA`, `ALUNO` and `COORDENADOR` as raw authorities consistently. Configure route matchers for all four endpoint groups and add matching `@PreAuthorize` declarations on their controllers. Keep resource-level checks, such as verifying the logged-in student owns a consultation, as an additional rule.

Only controller checks were rejected because coverage can be missed by a new method; only filter-chain checks were rejected because the requirement requires method-level defense as well.

## Risks / Trade-offs

- [Supabase history is unavailable or differs from inferred Git origin] → stop restoration, capture version/checksum evidence, and require confirmation before writing V2–V9.
- [Restoring historic bytes changes local development bootstrap semantics] → validate on a clean disposable database and deploy only after matching production history.
- [DTO conversion changes JSON shape] → preserve current field names and add contract tests for create, update and detail responses.
- [Concurrent return and scheduling operations collide] → surface optimistic-lock conflicts as HTTP 409 and test the losing transaction.
- [Canonical authorities expose legacy session values] → test logged-in profiles and update the authority producer within the same implementation change if needed.

## Migration Plan

1. Capture the Supabase `flyway_schema_history` versions/checksums without opening `.env`; compare them to Git blobs for V2–V9.
2. Restore only verified historical migration content, set baseline-on-migrate false, and create V19 only for a remaining structural correction.
3. Deploy to a clean validation database, run Flyway validation and Hibernate validation, then run authorization and workflow tests.
4. Deploy application and migration together. Roll back application code if needed; do not edit an applied V19—use a later corrective migration for database rollback.

