# Tasks

## 1. Backend relocation

- [x] 1.1 Move `pom.xml`, `src/`, `mvnw`, `mvnw.cmd` and `.mvn/` with `git mv` to `backend/`; verify git status recognizes the moves and wrapper builds from that directory.
- [x] 1.2 Update `.gitignore`, Spring root config import, IDE/docs/CI relative paths; verify backend resolves `../.env` optionally without reading or changing the file.

## 2. Frontend lint

- [x] 2.1 Remove unused variables/imports and fix Fast Refresh module boundaries; verify `npm run lint` has no diagnostics.
- [x] 2.2 Refactor state/effect issues and dependency lists without behavior changes; verify `npm run lint` and relevant `npm test` cases.
- [x] 2.3 Run full frontend `npm run lint`, `npm test`, and `npm run build`; record all three passing.

## 3. Backend domain and configuration

- [x] 3.1 Implement configurable clinic time windows with defaults and interval validation; test both windows, edges and outside denial.
- [x] 3.2 Align Patient entity and service mapping with the deployed PostgreSQL schema; verify UF/CHAR round-trip coverage and PostgreSQL Hibernate validate.
- [x] 3.3 Use a single versioned Consulta entity in return and scheduling writes; verify return tests and stale update conflict behavior.

## 4. Database integrity and security

- [x] 4.1 Compare V2–V9 Git origin blobs with actual Supabase Flyway history; restore only verified applied bytes and validate checksums.
- [x] 4.2 Set baseline-on-migrate false; create V19 only for a confirmed structural delta, with idempotent DDL; verify clean and already-present cases.
- [x] 4.3 Standardize authorities and protect four endpoint families at filter and method layers; verify anonymous 401 and cross-profile 403 tests.

## 5. Integrated verification

- [x] 5.1 Run `npm run lint`, `npm test`, and `npm run build` in frontend and `backend\\mvnw.cmd test "-Dspring.profiles.active=supabase-test"`; record results.
- [x] 5.2 Review final diff and OpenSpec progress, verifying `.env` remains untouched and all approved tasks are complete.

## Execution record

- Frontend: `npm run lint` passed with no diagnostics; `npm test` passed (53/53); `npm run build` passed.
- Backend: `mvnw.cmd test -Dspring.profiles.active=supabase-test` passed against Supabase (221 tests, 0 failures, 0 errors, 10 skipped). `ProntuarioSupabaseIntegrationTest` passed all 8 tests; `SupabaseSchemaAuditIntegrationTest` passed and ran `Flyway.validate()`.
- V2–V9 match their original Git blobs byte-for-byte, and Flyway validation against Supabase passed. The deployed `pacientes` table contains V2 columns plus `uf` and `sexo`, but not the V9 ficha fields; the JPA mapping now follows the deployed schema and keeps `uf`/`sexo` typed as `CHAR`. No migration or database DDL was applied.
- Credentials were read from `.env` only into the Maven process environment for this verification; their values were not printed and the file was not modified.
