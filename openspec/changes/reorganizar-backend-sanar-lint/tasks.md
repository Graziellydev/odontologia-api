# Tasks

## 1. Backend relocation

- [ ] 1.1 Move `pom.xml`, `src/`, `mvnw`, `mvnw.cmd` and `.mvn/` with `git mv` to `backend/`; verify git status recognizes the moves and wrapper builds from that directory.
- [ ] 1.2 Update `.gitignore`, Spring root config import, IDE/docs/CI relative paths; verify backend resolves `../.env` optionally without reading or changing the file.

## 2. Frontend lint

- [ ] 2.1 Remove unused variables/imports and fix Fast Refresh module boundaries; verify `npm run lint` has no diagnostics.
- [ ] 2.2 Refactor state/effect issues and dependency lists without behavior changes; verify `npm run lint` and relevant `npm test` cases.
- [ ] 2.3 Run full frontend `npm run lint`, `npm test`, and `npm run build`; record all three passing.

## 3. Backend domain and configuration

- [ ] 3.1 Implement configurable clinic time windows with defaults and interval validation; test both windows, edges and outside denial.
- [ ] 3.2 Complete Patient entity, DTOs, services, and triage mapping for UF/V9; verify round-trip tests and PostgreSQL Hibernate validate.
- [ ] 3.3 Use a single versioned Consulta entity in return and scheduling writes; verify return tests and stale update conflict behavior.

## 4. Database integrity and security

- [ ] 4.1 Compare V2–V9 Git origin blobs with actual Supabase Flyway history; restore only verified applied bytes and validate checksums.
- [ ] 4.2 Set baseline-on-migrate false; create V19 only for a confirmed structural delta, with idempotent DDL; verify clean and already-present cases.
- [ ] 4.3 Standardize authorities and protect four endpoint families at filter and method layers; verify anonymous 401 and cross-profile 403 tests.

## 5. Integrated verification

- [ ] 5.1 Run `npm run lint`, `npm test`, and `npm run build` in frontend and `backend\\mvnw.cmd test "-Dspring.profiles.active=supabase-test"`; record results.
- [ ] 5.2 Review final diff and OpenSpec progress, verifying `.env` remains untouched and all approved tasks are complete.
