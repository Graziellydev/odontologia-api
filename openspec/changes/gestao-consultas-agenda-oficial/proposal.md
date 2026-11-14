# Proposal

## Why

A secretaria precisa de uma agenda oficial que reúna a disponibilidade de cadeiras e os agendamentos, permitindo criar, consultar, remarcar e cancelar atendimentos com segurança. A tela de referência organiza a semana por dia e turno, mas a integração completa da grade com as operações de consulta ainda não está especificada como um fluxo único.

## What Changes

- Consolidar uma grade semanal filtrável por data, disciplina e disponibilidade, mostrando consultas e turnos livres.
- Criar consultas a partir da fila de espera; quando a combinação cadeira, data e turno ainda não tiver vaga cadastrada, criá-la sem violar `uk_vaga`.
- Remarcar consultas preservando o registro anterior e relacionando a consulta nova à origem.
- Cancelar consulta e devolver a demanda à fila de espera, mantendo histórico e liberando a vaga para uso futuro.
- Integrar o layout de referência do Figma ao topo de `frontend/src/Secretaria/AgendaSecretaria.jsx`, incluindo a grade semanal, formulário de novo agendamento, resumo, remarcação e cancelamento.

## Capabilities

### New Capabilities
- `agenda-oficial`: Consulta e gestão dos agendamentos da secretaria na grade semanal oficial.

### Modified Capabilities

## Impact

- Endpoints existentes em `/api/consultas` para listar, criar, remarcar e cancelar, com eventual extensão para suportar consulta por grade semanal e criação de vaga sob demanda.
- Repositórios e serviços de consulta, fila de espera, cadeiras e vagas; as tabelas existentes e a unicidade `uk_vaga` permanecem como fonte de integridade.
- Componente `frontend/src/Secretaria/AgendaSecretaria.jsx` e estilos associados para integrar a referência Figma.
- Integração com notificação WhatsApp existente, conforme o comportamento disponível no fluxo de agendamento.
