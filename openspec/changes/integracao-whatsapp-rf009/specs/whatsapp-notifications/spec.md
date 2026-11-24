# Spec Delta

## Purpose

Define how the system creates, stores, lists, and presents agenda notifications prepared for WhatsApp, distinguishing manual delivery from automated generation.

## ADDED Requirements

### Requirement: Generate next-day reminders idempotently
The system SHALL create at most one 24-hour reminder per eligible consultation and target date, using consultation states and appointment data supported by the existing schema.

#### Scenario: Consultation scheduled for tomorrow
- **WHEN** the reminder process runs for a consultation scheduled on the next local calendar day in an eligible active state
- **THEN** it persists one LEMBRETE_24H linked to patient and consultation, with personalized content and the instruction to reply 1 to confirm or 2 to cancel

#### Scenario: Repeated processing
- **WHEN** the scheduled or manual process runs again for the same consultation and target date
- **THEN** it does not create a duplicate reminder

#### Scenario: Available appointment data
- **WHEN** a reminder is generated
- **THEN** it includes patient name, date, available shift and time data, and does not fabricate a time absent from the schema

### Requirement: Restrict manual reminder processing
Only an authenticated SECRETARIA user SHALL invoke manual reminder processing.

#### Scenario: Secretary runs processing
- **WHEN** a SECRETARIA user sends POST /api/notificacoes/processar-lembretes-24h
- **THEN** eligible reminders are processed and the outcome distinguishes new from existing records

#### Scenario: Other or anonymous caller
- **WHEN** a non-SECRETARIA or unauthenticated caller invokes the operation
- **THEN** the system denies the request without processing reminders

### Requirement: List notifications using filters
The system SHALL expose a Secretariat-only notification list with optional period, type, and status filters over persisted records.

#### Scenario: Matching notifications
- **WHEN** the Secretariat requests any supported combination of filters
- **THEN** only matching records are returned with fields needed by the interface

#### Scenario: No matching notifications
- **WHEN** no records match
- **THEN** the system returns an empty result without an error

### Requirement: Preserve notification persistence and delivery semantics
Messages SHALL use the existing WhatsApp notification model and SHALL NOT be represented as sent merely because a wa.me link was opened.

#### Scenario: Manual WhatsApp link
- **WHEN** the Secretariat opens WhatsApp for a notification
- **THEN** the link contains the normalized patient number and encoded persisted message, while the record retains its actual status

#### Scenario: Other generated message types
- **WHEN** cancellation, rescheduling, satisfaction, or quick-call messages are prepared
- **THEN** each uses the existing patient, consultation, template, content, and status fields; phone is resolved from patient unless snapshotting is approved

### Requirement: Preserve notification access boundaries
Notification operations SHALL remain restricted to SECRETARIA and SHALL not expose the general wait queue to ALUNO.

#### Scenario: Student access
- **WHEN** an ALUNO calls a Secretariat notification operation
- **THEN** access is denied without exposing queue or contact data
