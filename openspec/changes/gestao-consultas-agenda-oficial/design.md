# Design

## Context

See `proposal.md` and `specs/agenda-oficial/spec.md` for motivation and behavior. `AgendamentoController` already exposes secretaria-only list, create, availability, cancel and reschedule routes under `/api/consultas`. `AgendamentoService` uses existing rows in `vagas`, links remade consultations through `consulta_origem_id`, and returns canceled demand to the waitlist. Its create flow currently chooses an existing available chair; it does not create a missing vacancy. `AgendaSecretaria.jsx` currently renders absence management, while `AgendamentosSecretaria.jsx` and `agendamentoService.js` contain the separate scheduling UI and API client.

## Goals / Non-Goals

**Goals:**
- Present one official weekly view that reflects active appointments, blocked vacancies and available chair/turn combinations.
- Extend the current consultation flow without losing the waitlist, audit, notification and concurrency protections.
- Place the Figma agenda section at the top of `AgendaSecretaria.jsx`, followed by the existing absence workflow.

**Non-Goals:**
- Replace the waitlist model or change historical consultation records.
- Implement bulk creation of weekly turns; that is covered by `turnos-disponiveis`.

## Decisions

- Reuse `/api/vagas/grade`, introduced by `disponibilizacao-turnos-disponiveis`, to avoid a duplicate read model. The query combines active chairs, vacancies, and consultations; optional `disciplina` and `disponibilidade` filters narrow the result. A chair/date/turn without a vacancy row and without an active consultation is `LIVRE`; a blocked vacancy is shown as blocked and cannot be booked.
- Keep create, cancel and reschedule in the existing agendamento service and transaction boundary. Extend creation to accept the chosen chair, data and turn; resolve or create the matching vacancy, then insert the consultation and move the queue item in the same transaction. Preserve `uk_vaga` as the race guard and convert conflicts to HTTP 409. Reuse eligible vacancy rows after cancellation; do not delete rows needed by history.
- For rescheduling, preserve the existing consultation as canceled and create a new row linked by `consulta_origem_id`; cancellation keeps the current waitlist return rules. Keep WhatsApp notifications in the existing notification service and commit them with the operation as currently designed.
- Compose the official agenda at the top of `AgendaSecretaria.jsx` using `agendamentoService.js` and the existing scheduling behavior as the API reference. Retain the absence panels below the new section. Match the supplied Figma hierarchy: week/date and filters, weekday columns, morning/afternoon chair cards, status legend, new appointment, reschedule and cancel actions. Do not duplicate the same scheduling API client in `FaltasService.js`.
- Keep card data derivable from a single selected week and its filters. After successful mutations, refresh the grid and selected detail so the display cannot continue to show a freed or occupied chair as stale.

## Risks / Trade-offs

- [Creating a vacancy and consultation can race with a concurrent request] → Resolve by the unique key inside the same transaction and translate uniqueness conflicts to HTTP 409; never continue with a mismatched chair slot.
- [A grid query spanning chairs, vacancies, and consultations can return stale availability during booking] → Treat the grid as a snapshot only and revalidate vacancy and active-consultation constraints on every write.
- [Combining agenda and absence screens can make the parent component large] → Keep data loading and mutation behavior in focused child components or hooks while the parent owns section order and shared refresh state.
- [Canceled or rescheduled consultations must remain auditable] → Preserve original consultation rows and use status transitions and `consulta_origem_id`; do not hard-delete consultation history.

## Migration Plan

No schema change is expected: the required chair, vacancy, consultation, waitlist and origin fields already exist in the V5 schema. Deploy the API and frontend together so the weekly view and its mutation payloads agree. Roll back by reverting the application release; do not reverse consultation or vacancy history created by the feature.
