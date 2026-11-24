# Design

## Context

The schema uses notificacoes_whatsapp with patient_id, optional consulta_id, template foreign key, content, optional sender, and status constrained to PENDENTE, ENVIADO, CONFIRMADO_RESPOSTA, ERRO. V10 seeds LEMBRETE_24H, OFERTA_VAGA_DESISTENCIA, cancellation, rescheduling, satisfaction, and rescheduling invitation templates. Consultation states CONFIRMADA_24H and CANCELADO_PACIENTE already exist. resposta_recebida currently allows only digits 1–5. Queue records include urgency, rescheduling priority, and entry timestamp. Hibernate uses ddl-auto=validate.

The appointment service creates pending notification records; existing notification listing is by consultation and restricted by the Secretariat controller. The Communication menu currently displays a development notice. No OpenSpec capabilities were found. The provided image is assets/comunicacao-whatsapp.png.

## Goals / Non-Goals

**Goals:**
- Separate message generation, manual WhatsApp delivery, and inbound response processing.
- Reuse notification table, templates, history trigger, queue rules, and allocation concurrency protections.
- Limit schema changes to verified gaps.

**Non-Goals:**
- Paid or official WhatsApp sending API.
- Real patient messages in development or tests.
- Changes to clinical eligibility, queue priority, patient return, or role rules.

## Decisions

1. Reuse notificacoes_whatsapp and seeded templates instead of creating a parallel table. Resolve phone from pacientes.telefone. Snapshot it only if a historical-contact requirement is approved.
2. Treat the response CHECK as a schema gap for raw words. Normalize accepted phrases to canonical 1/2 before storing in the current narrow field. Add a bounded field/migration only if verbatim reply retention is required.
3. Normalize full phone numbers and fail closed unless there is one pending notification linked to one active consultation. Never use arbitrary suffix matching. Shared/alternate phone policy remains unresolved.
4. Process transitions transactionally and set transaction-local audit origin PACIENTE_WHATSAPP. Do not invent a human actor. Verify trigger behavior with null actor and Hibernate validation.
5. Reuse current locking/versioning and unique active-consultation index for vacancy occupancy. For quick calls, require exact equality of procedure, tooth, and class with the released consultation; order compatible demands by the existing queue ordering: urgency, waiting time, then rescheduling priority. Do not add a separate accepts-last-minute rule because the user selected the existing queue priority behavior.
6. Keep manual list/process routes SECRETARIA-only. Exempt only the webhook route from session auth and CSRF; when configured, compare X-Webhook-Token safely. No token property is currently documented; deployment policy when unset needs agreement.
7. Add Comunicação to existing FilaEspera shell and reuse agendamentoService and modal patterns. Match the supplied reference, with usable narrow-screen behavior.

## Risks / Trade-offs

- Multiple consultations for one phone → no state change and safe follow-up.
- Current response constraint only stores digits → canonical response storage, or reviewed migration if verbatim text is required.
- No official delivery provider → keep records pending/manual and display actual status.
- Token unset → unauthenticated endpoint risk; require configured secret when exposing webhook in production.
- Queue quick-call semantics unclear → reuse verified query and defer unresolved eligibility policy.
- Schema changes can break validation → test Flyway and Hibernate validation in isolated PostgreSQL.

## Migration Plan

1. No migration for LEMBRETE_24H or consultation statuses; they already exist.
2. Review JPA mappings and Flyway state before adding behavior.
3. Add migration only for confirmed gaps such as a deduplication constraint/index, raw response representation, or approved snapshot field.
4. Verify migrations, constraints and history trigger in isolated PostgreSQL with ddl-auto=validate.
5. Roll back application behavior first; use reviewed forward migrations when destructive schema rollback risks data loss.

## Open Questions

- Can a provider supply a notification identifier, or must matching use phone plus uniqueness because current payload contains only phone and message?
- Are alternate patient phones eligible, and what is the policy for shared numbers?
- Which existing query/order governs quick-call candidates, and does it enforce aceita_chamada_ultima_hora?
- Must a webhook token be configured in every deployed environment?
- Is canonical 1/2 sufficient for audit, or must the exact response phrase be retained?

