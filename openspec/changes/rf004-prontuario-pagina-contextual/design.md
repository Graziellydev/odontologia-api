# Design

## Context

See [proposal.md](proposal.md) and the four capability specs for behavior. The existing RF004 implementation has server-side endpoints and a `ProntuarioDigital` drawer; the requested frame replaces only that presentation with a full contextual page. `RetornoService` already owns RF003 completion: it verifies the student, sets `app.usuario_id` and `app.origem='ALUNO'`, changes the consultation to `REALIZADO`, and conditionally creates a queue entry.

The database schema is fixed. V6 defines `prontuarios` and `prontuario_adendos`; `consulta_id` is unique. V8 blocks UPDATE/DELETE on both clinical tables and writes `consulta_historico`; V9 supplies appendable anamnese and the unique odontogram key `(paciente_id, dente, COALESCE(face_dente, 'GERAL'))`. Mapping must remain compatible with `ddl-auto=validate` and requires no migration.

Figma frame `4:46292` is 1440×1704: 248 px navigation, 1192 px work area, 82 px institutional bar, 40 px page padding and a 1112 px content column. Its evolution grid is 354 px chronological history plus 738 px current record, separated by 20 px; the lower closing card spans 1112 px. The reference defines page states, not standalone alert/addendum overlay measurements.

## Goals / Non-Goals

**Goals:**

- Replace the drawer with a page-level student clinical context while retaining existing protected API contracts where their behavior still satisfies the new specs.
- Coordinate one consultation across history, evolution save and RF003 closure without duplicate status changes or orphaned queue state.
- Make server authorization the source of truth and keep RNF003 scoped as visual exposure reduction.
- Preserve frame geometry and tokens on desktop, then collapse the two-column clinical layout predictably on narrow screens.

**Non-Goals:**

- Alter Flyway migrations, physical clinical constraints, or append-only trigger behavior.
- Create a legally qualified signature, expose patient PII in coordinator metrics, or allow coordinator clinical writes.
- Treat the demonstrative Figma links as test-only bypasses of the API security rules.

## Decisions

### 1. Replace the drawer state with a contextual page state

The student shell will own a selected `AtendimentoDoTurnoDTO`/consultation context. Selecting a patient transitions from `Pacientes do turno` to the record page; the back action clears the selected context and restores the turn list. This follows the measured reference hierarchy and avoids a full-screen drawer competing with the shell navigation.

The prior alternative was to resize the drawer. It was rejected because the Figma frame includes the persistent sidebar, institutional bar, 40 px page padding and the full-width closing card, which are page-level relationships.

### 2. Preserve the REST boundary and add only data needed by the page

`GET /api/prontuarios/paciente/{pacienteId}` remains the source for patient/history/odontogram data and the selected appointment. The page passes the `consultaId` already returned by the turn list to `POST /api/prontuarios`; creation derives ownership from the consultation. The existing retorno endpoint remains the source for `Necessita Retorno` and `Tratamento Concluído` rather than duplicating queue logic in the prontuário endpoint.

The alternative of a combined mega-endpoint was rejected because it would duplicate RF003 rules and make independently recoverable operations inseparable. The UI will sequence save and closure: it must surface failure of either operation and refresh the context after success.

### 3. Keep authorization tiered by operation and consultation status

The network filter executes before any clinical response. A central authorization service then checks the authenticated student, patient, current date, configured turn, and allowed status. Creation accepts only open statuses; read/addendum extend to same-turn `REALIZADO`. The coordinator is routed only to GET and is denied every mutation.

`X-Forwarded-For` is consumed only behind configured trusted proxies, with `remoteAddr` as the fallback. This prevents a client forged header from overriding source address.

### 4. Treat evolution save and completion as bounded transactions

The evolution service owns canonical text normalization, signature generation, initial record insert, optional odontogram observation, and optional `REALIZADO` transition. Before status change it sets transaction-local trigger variables. A database error rolls back this unit.

RF003 closure remains a distinct transaction when invoked through its own endpoint. The interface must not call RF003 after an evolution save that already completed the consultation. For the page flow, the implementation will use a single explicit choice: save-only, save-and-complete, or after-save RF003 closure, and will reject incompatible duplicate closure attempts.

### 5. Implement the Figma page as modular React CSS

The desktop page uses the measured content widths: 1112 px available clinical column, `354px minmax(0, 738px)` evolution grid and 20 px gaps. Panels are white with `#E3E8EB` borders and 12 px radius; controls retain 8 px radius. At narrow widths, history and form stack and the patient summary/closing columns wrap without horizontal data loss.

The only overlays are behavioral adaptations not measured in the frame: compact alert treatment for 403 responses and an addendum dialog. Their tokens follow the frame (`#FFF0EF`/`#B43B39` for denial; `#FFFFFF`/`#E3E8EB` for edit), while actual API 403 remains authoritative.

### 6. Keep visual LGPD controls contained and functional

The record page renders a non-interactive watermark layer over the clinical surface. The repeating text uses authenticated name, available matricula/registro and a timer-refreshed local timestamp. `user-select: none` applies to read-only clinical blocks only, leaving inputs, textareas, buttons and accessibility controls usable. `@media print` hides the page clinical region.

## Risks / Trade-offs

- [Figma has no measured standalone modal frames] → Use token-consistent responsive overlays and document their geometry as adaptation, not a claimed exact measurement.
- [Save and closure can both move a consultation to `REALIZADO`] → Model mutually exclusive UI paths and refresh status after each successful mutation.
- [The database accepts several anamnese rows] → Select the latest deterministically and allow an empty response block.
- [Visual safeguards can be bypassed in the browser] → Enforce network, role and consultation authorization server-side and minimize payload fields.
- [Current active code and the new page may coexist during rollout] → Remove the drawer entry point in the implementation change only after the replacement page and back navigation are working.

## Migration Plan

No database migration is required. Deploy the backend contract fixes and authorization checks first, retaining current endpoint paths. Deploy the page and replace the drawer entry point in the same frontend release. Rollback restores the previous frontend entry point; clinical records and audit history remain valid because their schema and append-only behavior are unchanged.
