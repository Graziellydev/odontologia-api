# Design

## Context

See `proposal.md` for motivation and `specs/gestao-vagas/spec.md` for the external contract. The V5 migration already provides `cadeiras.ativa`, `vagas.bloqueada`, `vagas.motivo_bloqueio` (200 characters), the unique key `uk_vaga (cadeira_id, data, turno)` and a foreign key from consultations to vacancies. The current backend has a `Vaga` mapping and repository used by appointment flows, but no dedicated vacancy or chair controller.

## Goals / Non-Goals

**Goals:**
- Add the secretaria API while reusing the existing tables and keeping current appointment behavior compatible.
- Make duplicate creation and destructive operations safe under concurrent requests.

**Non-Goals:**
- Add a database migration or change the V5 schema.
- Generate weekly batches or build the weekly agenda grid; those belong to the separate turn availability and official agenda changes.

## Decisions

- Keep the existing `Vaga` entity and extend the consultas feature with DTOs, repository queries, service rules and REST controllers. Add a read model for chair information rather than exposing JPA entities. This avoids a competing mapping for `vagas`; a separate vaga package was considered but would split behavior already consumed by consultation services.
- Use `GET /api/cadeiras` for active chair options, `POST /api/vagas` and `GET /api/vagas` for create/list, `GET /api/vagas/{id}` for detail, `PATCH /api/vagas/{id}/bloqueio` and `/desbloqueio` for state transitions, and `DELETE /api/vagas/{id}` for removal. Return conflict for uniqueness or linked-consultation conflicts, validation errors for invalid turn/reason/date, and not found for missing IDs.
- Validate that a chair exists and is active before creating a vacancy. Let `uk_vaga` remain the final concurrency guard; translate its violation into a stable HTTP 409 response. An application pre-check improves the message but does not replace the database constraint.
- Block and unblock under a row lock, and prevent removing a vacancy if any row in `consultas` references it, including canceled rows. The foreign key remains the last line of defense against races.
- Reuse the existing role/session guard for secretaria routes. Do not expose blocked vacancies as schedulable; preserve the booking flow's availability check.

## Risks / Trade-offs

- [Concurrent requests can race between a pre-check and insert/delete] → Keep `uk_vaga` and the foreign key authoritative, handle their constraint violations as conflicts, and perform state-changing operations transactionally.
- [Adding routes under `/api/cadeiras` can conflict with future chair administration] → Keep this scope read-only for chair selection; chair creation and editing are not part of this change.
- [A vacancy with only canceled consultation history cannot be deleted] → This intentionally preserves referential history and matches the explicit no-linked-consultation rule.

## Migration Plan

No schema migration is expected. Deploy the API after confirming the target database has the V5 columns and constraints. Rollback consists of reverting the application release; existing vacancy data remains unchanged.
