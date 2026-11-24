# Proposal

## Why

A Secretaria precisa acompanhar lembretes, cancelamentos e respostas de pacientes em fluxo integrado e auditável com a agenda. A solução combina registros preparados para envio manual via WhatsApp, lembretes gerados pelo sistema e processamento seguro de respostas, sem pressupor envio automático por API oficial.

## What Changes

- Gerar lembretes D+1 idempotentes e permitir processamento manual apenas pela Secretaria.
- Consultar notificações por período, tipo e status, usando a tabela e templates existentes.
- Receber respostas pelo webhook e correlacioná-las sem ambiguidade a notificação e consulta.
- Aplicar confirmações/cancelamentos com histórico, auditoria, idempotência e regras existentes de vaga e fila.
- Oferecer vagas liberadas às demandas compatíveis sem permitir alocação concorrente duplicada.
- Criar a área Comunicação WhatsApp e ações demonstrativas no modal, seguindo a imagem fornecida.
- Documentar migrations apenas para incompatibilidades confirmadas.

## Capabilities

### New Capabilities
- whatsapp-notifications: Geração, consulta e acompanhamento de notificações para WhatsApp.
- whatsapp-replies: Recepção de respostas, atualização de consultas e oferta concorrente de vagas.
- secretaria-whatsapp-communication: Interface da Secretaria para mensagens, links e simulações.

### Modified Capabilities
- None. A lista de specs do projeto não contém capacidades existentes.

## Impact

Backend Spring Boot, PostgreSQL/Flyway e frontend React: agenda, fila, notificações, segurança, histórico e shell da Secretaria. A tabela existente é notificacoes_whatsapp; o telefone pode ser resolvido pelo cadastro do paciente. A imagem assets/comunicacao-whatsapp.png orienta a tela. O link wa.me não indica envio efetivo.
