# Spec Delta

## Purpose

Provide the Secretariat with a communication workspace matching the supplied visual reference and integrated with existing navigation and appointment notification data.

## ADDED Requirements

### Requirement: Show the WhatsApp workspace
The Secretariat navigation SHALL open a light-theme page following assets/comunicacao-whatsapp.png in layout, hierarchy, spacing, colors, cards, badges, buttons, and conversation bubbles. The design tokens are Lato, background #F7F8FA, white cards, borders #E3E8EB, primary #C65117, green #287252/#EAF5EE, alert #FFF1E7, and color-scheme light.

#### Scenario: Open communication
- **WHEN** a SECRETARIA user selects Comunicação
- **THEN** the page shows the Secretaria label, title Comunicação WhatsApp, supplied explanatory subtitle, Gerar lembretes 24h action, and period/type/status filters

#### Scenario: Messages and quick-call cards
- **WHEN** the workspace loads
- **THEN** its two-column layout shows the upper-left Mensagens automáticas card with subtitle “Não há portal ou área de acesso do paciente”, columns Paciente / envio, Tipo, Status, active-row selection, and footer pagination/update time; the lower-left Chamada rápida · vaga de hoje card shows date/shift/chair, procedure/tooth/class, compatible-demand count, and the orange notice “A primeira aceitação compatível ocupa a vaga” followed by the reference text that other candidates receive an unavailable notice and the Secretariat is alerted when no compatible demand exists

#### Scenario: Conversation preview
- **WHEN** a notification is selected
- **THEN** the right Prévia da conversa card shows selected patient/type, green WhatsApp · acompanhamento de envio badge, date/time, sent-message and reply bubbles, Abrir no WhatsApp, Simular 1 – Confirmar, Simular 2 – Cancelar, and the green Confirmação registrada na agenda state only after a persisted confirmation; supported table types include Lembrete 24h, Cancelamento imediato, Remarcação imediata, Chamada rápida, Satisfação opcional, and Convite de remarcação; absent responses are not shown as received

### Requirement: Open a prefilled manual WhatsApp conversation
The interface SHALL create a wa.me URL using a normalized Brazilian number and safely encoded notification content.

#### Scenario: Valid patient number
- **WHEN** the Secretariat opens WhatsApp for a selected notification
- **THEN** the browser opens https://wa.me/55{digits}?text={encodedContent} with DDI 55 exactly once

#### Scenario: Missing telephone
- **WHEN** the patient has no usable telephone
- **THEN** the action is disabled or a clear error is shown and no invalid URL is created

### Requirement: Label reply controls as simulations
The UI SHALL identify simulated replies as test actions, not messages received directly from WhatsApp.

#### Scenario: Simulate a reply
- **WHEN** the user chooses Simular 1 – Confirmar or Simular 2 – Cancelar
- **THEN** the UI posts selected patient phone and reply to /api/webhooks/whatsapp, refreshes persisted state, and identifies the action as a simulation

### Requirement: Integrate appointment notification modal
The existing Notificações geradas modal SHALL offer the same link and explicit reply simulation when patient and message data are available.

#### Scenario: Modal actions
- **WHEN** the Secretariat views an eligible notification in the modal
- **THEN** it can open a prefilled WhatsApp conversation and trigger a labeled simulation using shared behavior where practical

### Requirement: Show loading and privacy states
The interface SHALL communicate loading, errors, empty results, and filter refreshes while limiting contact data to Secretariat workflows.

#### Scenario: Request state
- **WHEN** requests are pending, fail, or return no results
- **THEN** the workspace shows the matching state, keeps filters usable, and does not present stale data as current

