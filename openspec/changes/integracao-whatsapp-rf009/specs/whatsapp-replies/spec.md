# Spec Delta

## Purpose

Define safe processing of patient replies and quick-call acceptances against existing consultation history, queue, and vacancy rules.

## ADDED Requirements

### Requirement: Validate webhook using an external configurable token
The webhook SHALL be callable without a user session and SHALL validate X-Webhook-Token when a secret is configured outside source control.

#### Scenario: Token configured
- **WHEN** a request omits or supplies an invalid configured token
- **THEN** it is rejected before notification, consultation, or queue state changes

#### Scenario: Token not configured
- **WHEN** a validly shaped request arrives while no token is configured
- **THEN** it may be processed as an unauthenticated integration request, subject to input validation

### Requirement: Resolve each reply to one pending notification
The system SHALL normalize telephone digits and process a reply only when it identifies one unambiguous pending notification and active consultation.

#### Scenario: Unique match
- **WHEN** canonical incoming digits match exactly one eligible pending notification for an active consultation
- **THEN** the system applies the supported response to that linked consultation

#### Scenario: Ambiguous or unmatched number
- **WHEN** a number matches multiple candidates or none
- **THEN** no consultation changes and the outcome is reported for safe follow-up

#### Scenario: Brazilian country code
- **WHEN** incoming and stored numbers contain punctuation or optional DDI 55
- **THEN** comparison strips non-digits, handles DDI 55, and avoids suffix matching that could select another patient

### Requirement: Process confirmations idempotently with audit history
Recognized confirmation replies SHALL transition eligible active consultations to the existing confirmation state and preserve the audit trigger.

#### Scenario: Confirmation phrase
- **WHEN** the unique pending reminder begins with 1, CONFIRMAR, or SIM
- **THEN** the consultation becomes CONFIRMADA_24H, a response is recorded, and confirmation content is prepared without asserting WhatsApp delivery

#### Scenario: Repeated confirmation
- **WHEN** a successful confirmation is delivered again
- **THEN** no duplicate state transition, history event, or confirmation notification is created

### Requirement: Process cancellations using current rules
Recognized cancellation replies SHALL use the existing patient cancellation status and release the vacancy through established transaction and queue rules.

#### Scenario: Cancellation phrase
- **WHEN** the unique pending reminder begins with 2, CANCELAR, or NAO
- **THEN** the consultation becomes CANCELADO_PACIENTE, the vacancy is released under current rules, and queue return follows only existing behavior

#### Scenario: Quick-call follows cancellation
- **WHEN** cancellation releases an eligible vacancy
- **THEN** quick-call messages are prepared only for demands compatible under existing procedure, tooth, class, and queue criteria

### Requirement: Allocate a released vacancy at most once
Concurrent quick-call acceptances SHALL NOT allocate the same vacancy more than once.

#### Scenario: Competing acceptances
- **WHEN** multiple notified demands accept the same vacancy concurrently
- **THEN** one commits and all others receive unavailable outcomes, protected by existing lock/version and active-consultation uniqueness or an equivalent reviewed mechanism

#### Scenario: Vacancy already taken
- **WHEN** another demand wins the vacancy
- **THEN** remaining candidates receive persisted unavailability messages and cannot occupy it

### Requirement: Preserve webhook audit provenance
Webhook consultation updates SHALL use the existing history trigger with WhatsApp origin and shall not impersonate a human user.

#### Scenario: External status update
- **WHEN** a webhook transaction changes consultation status
- **THEN** transaction-local origin is PACIENTE_WHATSAPP, history is preserved, and actor remains null without an authenticated user
