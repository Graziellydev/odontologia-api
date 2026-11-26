# Spec Delta

## Purpose

Mantém uma representação consistente da consulta para os fluxos de agendamento e retorno, preservando a concorrência otimista da tabela clínica.

## ADDED Requirements

### Requirement: Representação única de consulta mutável
O sistema SHALL usar uma única representação versionada para ler e alterar uma mesma linha de consulta em todos os fluxos que atualizam seu status ou dados de retorno.

#### Scenario: Registro de retorno
- **WHEN** o aluno registra a conclusão de um atendimento e informa ou não a necessidade de retorno
- **THEN** a atualização aplica o status e os dados de retorno à mesma consulta com controle de versão

### Requirement: Conflito concorrente não é silencioso
O sistema SHALL rejeitar atualização de consulta cuja versão tenha sido alterada por outro fluxo desde sua leitura.

#### Scenario: Atualizações concorrentes
- **WHEN** agendamento e retorno tentam salvar versões divergentes da mesma consulta
- **THEN** apenas a atualização válida é persistida e a outra recebe erro de conflito

### Requirement: Contratos de retorno preservados
O sistema SHALL preservar as respostas e regras existentes do fluxo de retorno, incluindo a criação de uma única entrada elegível na fila quando o retorno for necessário.

#### Scenario: Retorno necessário
- **WHEN** o aluno encerra seu atendimento aberto com justificativa técnica de retorno
- **THEN** a consulta é concluída e a demanda é encaminhada para a fila sem duplicação
