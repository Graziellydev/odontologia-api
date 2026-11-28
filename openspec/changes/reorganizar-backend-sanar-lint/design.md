# Design

## Context

See `proposal.md` for motivation. Maven currently lives in the repository root. The frontend lint run reports 9 errors and 1 warning, mostly synchronous state updates inside effects, plus unused values. Patient/JPA, security, and Flyway adjustments are already represented by the separate in-progress `ajustes-tecnicos-arquitetura` change; this change consolidates those requirements with relocation and frontend cleanup.

## Goals / Non-Goals

**Goals:** isolate build roots and preserve Git history; eliminate ESLint diagnostics without suppressions that hide problems; retain PostgreSQL schema and API behavior.

**Non-Goals:** change UI styling, endpoint contracts, or database schema unless evidence requires a new V19.

## Decisions

- Move Maven files with `git mv` and run wrapper from `backend/`. Change ignore rules to cover `backend/target/`; leave frontend scripts and OpenSpec paths rooted in repository root.
- Since application config imports a root `.env`, update only the import expression to `optional:file:../.env[.properties]`. The `.env` content is not part of the change.
- Fix lint findings in source: remove unused declarations, make effects asynchronous where synchronizing with external data, derive state rather than mirroring props when appropriate, and split React component exports only where Fast Refresh requires it. Do not disable lint rules globally.
- Preserve existing tasks from `ajustes-tecnicos-arquitetura` where implementation overlaps; do not duplicate or regress those changes. Canonical consultation remains `Consulta`.
- Restore V2–V9 only after comparing Git introduction blobs and actual Flyway history checksums. A migration repair or V19 is not inferred from unavailable production evidence.

## Risks / Trade-offs

- [Wrapper or IDE scripts depend on root Maven paths] → search repository references, update documentation and commands, and verify wrapper from backend.
- [Effects refactor changes request timing] → preserve cancellation guards and assert current behavior with frontend tests.
- [Supabase history is unavailable] → leave historical SQL untouched and report that production checksum validation requires supplied database context.
- [User's pre-existing in-progress architecture work overlaps] → preserve its working-tree changes and run targeted regression tests after integration.

## Migration Plan

1. Move Maven assets into backend using `git mv`; update root ignores and relative imports.
2. Resolve frontend diagnostics incrementally, running lint and tests after related edits.
3. Complete architecture and authorization changes, then run Maven suite and Hibernate validation.
4. Compare migration history and only restore confirmed applied bytes; add V19 only for a confirmed structural delta.
5. Run frontend lint/test/build and backend `supabase-test` from the new project directory.
