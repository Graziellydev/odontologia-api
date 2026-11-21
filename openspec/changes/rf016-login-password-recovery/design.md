# Design

## Context

See `proposal.md` for motivation and `specs/institutional-login/spec.md` plus `specs/password-recovery/spec.md` for behavior contracts.

The current application authenticates through a Spring Security HTTP session. `AutenticacaoController` exposes `/api/auth/csrf`, `/login`, `/logout` and `/me`; login currently resolves only `usuarios.email`. `SecurityConfig` permits `/api/auth/**` but keeps CSRF enabled. `Usuario` maps to the V2 `usuarios` table, whose recovery-relevant fields are `id BIGINT`, `email VARCHAR(150) UNIQUE`, `senha_hash VARCHAR(255)`, `perfil VARCHAR(30)` with a three-profile check, `matricula_ou_registro VARCHAR(50) UNIQUE` and `ativo BOOLEAN`.

Flyway migrations are present through V15, so the next migration is V16. No mail starter or mail sender configuration exists in `pom.xml` or application configuration. The existing React login uses Axios and obtains a CSRF header; `App.jsx` dispatches to areas using the profile returned by login, while `index.css` still imposes the Vite light/dark theme and global layout.

The Figma file `K835WNYGJT3DHT3yauWRfr` was inspected for both nodes at once. The 1440×1024 login and recovery frames share a 680px left presentation panel and 760px right access panel. Left content begins 56px from the edges and includes the UniCatólica signature, clinic subtitle, large two-line institutional title, supporting text, 568×380 rounded image, two side-by-side highlights and a small footer. The login form column is 544px wide, inset 108px within the right panel; its order is portal eyebrow, welcome heading/copy, three reference tabs, explanatory copy, two labeled 44px fields, recovery link, orange sign-in action, red error feedback, divider and permissions block. The recovery frame reuses the left panel and places a recovery heading/copy, one 44px identifier field, full-width send action, outlined return action and orange exclusive-access notice on the right. The visible references use warm off-white and white surfaces, dark slate text, muted gray secondary text, orange actions, soft red error feedback, thin pale borders, rounded controls and Lato-like typography. Only the login reference shows an error state; recovery success, pending and code-entry screens are not separate Figma frames and must follow the same hierarchy/tokens.

## Goals / Non-Goals

**Goals:**

- Keep the authentication/session model already used by the application while ensuring the server is authoritative for roles.
- Let a user identify themselves by institutional e-mail or matrícula on login and recovery.
- Provide single-use, expiring recovery codes with neutral public responses, protected persistence and auditable transaction boundaries.
- Keep recovery usable with configured SMTP and make local development possible without returning codes in API responses.
- Reproduce the two supplied Figma frames and extend the recovery flow coherently where the reference has no reset-completion frame.

**Non-Goals:**

- Replace HTTP sessions with JWT or change authorization rules for unrelated API resources.
- Provide self-registration, account discovery, or password reset for patient accounts or other profiles.
- Guarantee delivery when SMTP is not configured; development logging is a local diagnostic path only.
- Claim that the Figma reference defines the code-entry and new-password screen; those states are functional extensions following its visual system.

## Decisions

1. **Resolve login by one identifier while keeping server-derived roles.** Update the login request contract to accept `identificador` (e-mail or matrícula) and resolve it against `email` or `matricula_ou_registro`; if an older client still sends `email`, accept that field during transition. Normalize e-mail case and surrounding whitespace; compare matrícula after trimming. Require an active user and one of the three allowed profiles. Never use the Figma reference tab to choose an authority. This fits the current session and `LoginResponseDTO`; a new authentication provider or role-selection endpoint would duplicate existing identity state.

2. **Keep authentication and recovery endpoints public but CSRF-protected.** `/api/auth/**` is already `permitAll`; retain that rule and do not disable CSRF. Front-end POST calls use the existing CSRF bootstrap and `X-XSRF-TOKEN` handling. Public means no authenticated session is required, not that browser requests bypass CSRF.

3. **Add an additive V16 token table mapped explicitly to PostgreSQL types.** Create `password_reset_tokens` with an identity `BIGINT` primary key, `usuario_id BIGINT NOT NULL` referencing `usuarios(id)`, `codigo_hash VARCHAR(255) NOT NULL`, `expira_em TIMESTAMPTZ NOT NULL`, `usado BOOLEAN NOT NULL DEFAULT FALSE`, `criado_em TIMESTAMPTZ NOT NULL DEFAULT now()`, and `tentativas_falhas INTEGER NOT NULL DEFAULT 0`. Add an index for `(usuario_id, criado_em)` and an index/partial unique strategy that permits at most one unused token per user. Map `OffsetDateTime` to timestamp-with-time-zone and mirror nullability/length/default expectations so Hibernate `ddl-auto=validate` succeeds. The counter is an explicit security extension to the requested columns, required to enforce the five-attempt limit across restarts and multiple instances.

4. **Hash codes with the existing BCrypt encoder and compare only after selecting the user token.** Generate six digits with a cryptographically secure random generator and retain leading zeroes. Store `PasswordEncoder.encode(codigo)` and validate with `matches`; do not store plaintext or use a plain unsalted digest for a one-million-value code space. Invalidate earlier unused tokens transactionally before issuing a replacement. On five failed checks invalidate the token; on success update the password hash and consume the token atomically. A row lock or equivalent concurrency control prevents two simultaneous valid submissions from reusing the same code.

5. **Make the recovery request indistinguishable for account lookup outcomes.** Resolve e-mail/matrícula only to active permitted profiles, send to the e-mail stored on that account, and always return the same HTTP 200 response for unknown, inactive, unsupported and eligible identifiers. For e-mail/matrícula collision across different users, do not send and keep the same response. Do not return whether SMTP delivery succeeded. Use `JavaMailSender` and environment-based SMTP properties when configured; in development only, log a structured event containing the code for local use. Never log codes, identifiers, or message bodies in production.

6. **Throttle verification without broadening public disclosure.** Persist failed-attempt count with the token and enforce a maximum of five failures. Also apply a configurable request-rate limit to recovery and verification endpoints by source address and normalized identifier to constrain repeated code generation and distributed guessing. Do not include rate-limit details that disclose account existence in the request endpoint response.

7. **Keep screens in one login experience, with an explicit recovery state machine.** Model `login → request → code-and-new-password → completion`; provide a direct return action from each recovery state. Validate required values, 12-character minimum and password confirmation in React, while the server independently enforces the minimum and all token rules. Use the shared `api.js` helper for all POST requests so CSRF and response status handling stay consistent. Keep validation and status messaging associated with fields/regions using accessible live announcements.

8. **Scope visual changes to login/authentication and neutralize global theme inheritance.** Refactor `Login.jsx` and its CSS; add the recovery states to the same feature boundary and make `index.css` use `color-scheme: light`, Lato, and login-compatible sizing/tokens without a dark-mode media override or fixed Vite root width. Use the supplied tokens `#F7F8FA`, `#FFFFFF`, `#E3E8EB`, `#C65117`; use the established system error/success colors where applicable. Reuse the existing local asset only if it matches the Figma clinic photo; otherwise resolve the image asset during implementation rather than substituting unrelated stock art.

## Risks / Trade-offs

- [SMTP outages delay code delivery] → Keep the same neutral response, emit safe operational logs/metrics, and never return the code outside local development.
- [Six-digit codes have a small search space] → BCrypt verifier, 15-minute expiry, persisted five-failure invalidation, request throttling and single-use transactional consumption.
- [E-mail and matrícula formats can collide] → If the same normalized identifier resolves to different users, do not issue a code and preserve the generic response.
- [JPA validation rejects a type or nullability mismatch] → Match SQL `TIMESTAMPTZ` to `OffsetDateTime`, identity strategy to `BIGINT`, and explicit lengths/nullability in entity mapping; verify V16 against the PostgreSQL schema.
- [Role reference tabs may be mistaken for authorization] → Preserve Figma's caption that selection is illustrative; route only from the authenticated profile and retain server-side authorization.
- [Current global CSS can affect unrelated panels] → Prefer login-scoped selectors and remove only the Vite global constraints that conflict with application-wide behavior; check all three authenticated shells for regressions.

## Migration Plan

1. Apply Flyway V16 before starting the new application version; it only adds the token table and indexes and does not alter `usuarios`.
2. Deploy backend request compatibility for both `identificador` and the legacy `email` login field, recovery endpoints, rate limiting, and optional SMTP configuration.
3. Deploy the React login and recovery flow, then verify profile dispatch, CSRF-protected requests, mail delivery and local development delivery.
4. Roll back application binaries if necessary while leaving V16 in place. The table is additive and can remain unused; do not drop issued-token history during a production rollback. Password hashes changed by successful resets remain valid with the existing BCrypt login path.

## Open Questions

None. The existing `hero.png` is the Vite illustration and no matching clinic photo is present in the frontend assets. Implementation SHALL export the exact Figma image from the referenced institutional panel when available through the connected design file; if the node is not exportable, use an institution-approved image with the same crop/aspect ratio and record that substitution. Mail properties use Spring Boot's `SPRING_MAIL_*` environment configuration and credentials are never committed.
