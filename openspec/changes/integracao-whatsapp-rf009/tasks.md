# Tasks

## 1. Backend notifications and persistence

- [x] 1.1 Confirm notification mappings against V7/V10 columns and checks; document gaps before changing mappings.
- [x] 1.2 Enable Spring scheduling and implement the daily 08:00 America/Fortaleza @Scheduled D+1 reminder job; verify date-boundary behavior with unit tests.
- [x] 1.3 Enforce one reminder per consultation/date; verify repeated and concurrent runs do not duplicate records.
- [x] 1.4 Add SECRETARIA-only manual processing and filtered listing; verify role denial and period/type/status filtering.
- [x] 1.5 Persist cancellation, rescheduling, satisfaction, and quick-call messages using existing template/content fields; verify statuses do not claim delivery.
- [x] 1.6 Add Flyway migration only for confirmed gaps; verify it and Hibernate ddl-auto=validate in isolated PostgreSQL.

## 2. Webhook, audit, and vacancy concurrency

- [x] 2.1 Add sessionless webhook and route-scoped CSRF handling; verify configured token accepts valid and rejects invalid/missing values.
- [x] 2.2 Normalize numbers and uniquely resolve pending notification; verify ambiguous/unmatched cases change no state.
- [x] 2.3 Process confirmation/cancellation transactionally with audit provenance and idempotency; verify repeat replies do not duplicate history.
- [x] 2.4 Reuse cancellation and queue behavior; match procedure, tooth, and class exactly, then follow existing queue order; verify eligibility/order against repository queries.
- [x] 2.5 Apply existing concurrency controls to vacancy acceptance; integration test proves only one concurrent acceptance wins.
- [x] 2.6 Remove secrets, phone numbers, and message bodies from logs; verify representative error logs.

## 3. Secretariat frontend

- [x] 3.1 Add Communication navigation/page following the reference; verify desktop and narrow layouts against the supplied image.
- [x] 3.2 Add filters, paginated notification table, selection, quick-call card and loading/error/empty states; verify filter refresh.
- [x] 3.3 Add conversation preview, encoded wa.me link and labeled simulations; verify webhook call and refreshed persisted state.
- [x] 3.4 Add shared link/simulation actions to appointment notification modal; verify missing phone and request failures.
- [x] 3.5 Expose only required notification DTO/service fields; verify contact data remains Secretariat-only.

## 4. Security and integration verification

- [x] 4.1 Verify ALUNO cannot access notification operations or general queue through the new routes.
- [x] 4.2 Run tests against isolated data for migrations, audit, idempotency, ambiguous matching, and competing vacancy acceptance; confirm no real WhatsApp message is sent.
- [x] 4.3 Review artifact consistency and OpenSpec validation; keep every task unchecked.

