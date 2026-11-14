# Design

## Context

See `proposal.md` and `specs/turnos-disponiveis/spec.md`. The V5 schema stores one `vagas` row per chair, date and `MANHA`/`TARDE` turn, with `uk_vaga` as the uniqueness guard. It has no start/end time columns; the two labels map to fixed presentation times (08:00–12:00 and 13:00–17:00). The existing repository reserves available vacancy rows for appointments. The separate `gestao-vagas` change defines the shared secretary operations for listing, blocking and deleting individual vacancies.

## Goals / Non-Goals

**Goals:**
- Generate an idempotent set of vacancy rows for active chairs across every date in a requested interval, weekends included.
- Render all seven days in the weekly grid, with accessible horizontal scrolling on narrow layouts while preserving the Figma card hierarchy.
- Derive “Vaga livre” for unblocked, unoccupied chair/turn combinations even if no vacancy row has been generated.

**Non-Goals:**
- Store a time range per vacancy or change the V5 schema.
- Duplicate vacancy CRUD, blocking or deletion rules from `gestao-vagas`.

## Decisions

- Add a batch generation operation under `/api/vagas/geracoes` accepting start and end dates. Generate both fixed turn values for every active chair and calendar date inclusively. Return counts for created and already-existing combinations so repeating the request is safe and observable.
- Use a set-based database insert with `ON CONFLICT (cadeira_id, data, turno) DO NOTHING`. This delegates concurrent idempotency to `uk_vaga` and avoids loading a chair/date/turn loop into application memory. Keep generation transactional so a failed request does not leave an unexplained partial range.
- Build the weekly grid as a read model from active chairs crossed with requested dates and the two canonical turns, left joining vacancies and active consultations. Classify a cell as occupied when it has an active consultation, blocked when its vacancy is blocked, and otherwise “Vaga livre”. Canceled consultations do not occupy a cell, but their referenced vacancy remains protected from deletion by the shared vacancy rules.
- Keep `turno` as the canonical persisted value and derive display times from a single mapping in API/UI. Adding time columns was considered and rejected because V5 already models only morning/afternoon and a schema change would introduce duplicate sources of truth.
- Reuse the `gestao-vagas` blocking/deletion service and its rules. Blocking an ungenerated combination first materializes the vacancy in the same transaction, then applies the reason; deletion is allowed only with no consultation references. This keeps generated and manually managed vacancies consistent.
- Require secretary authorization for batch generation and grid reads. The frontend shows Monday through Sunday in a horizontally navigable week view; the attached image remains the style reference for filters, cards, states and morning/afternoon sections.

## Risks / Trade-offs

- [A long period can generate many rows] → Use one set-based statement and return aggregate counts; apply request-size validation based on operational limits if measured database load requires it.
- [An active chair can become inactive after generation] → The grid uses current `cadeiras.ativa` as the inclusion rule; existing vacancy and consultation history remains stored.
- [Generation and booking can create the same vacancy concurrently] → Let the unique key arbitrate; on-conflict generation is a successful no-op for that combination.
- [Weekly grid results can become stale between read and booking] → Revalidate blocked state, chair activity, slot occupancy and queue eligibility inside each write transaction.

## Migration Plan

No schema migration is expected. Deploy after the shared vacancy management contract is available, then enable the generation action and seven-day grade in the frontend. Roll back the application release without removing generated rows; the rows are valid vacancy data and can continue to be used by the booking flow.
