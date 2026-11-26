# Spec Delta

## Purpose

Permite à clínica adequar as janelas de atendimento do prontuário por ambiente, sem alterar ou recompilar a aplicação.

## ADDED Requirements

### Requirement: Janelas de turno configuráveis
O sistema SHALL ler os limites de início e fim dos turnos MANHA e TARDE da configuração da aplicação, com valores padrão 07:30–12:00 e 13:30–18:00.

#### Scenario: Acesso dentro da janela configurada
- **WHEN** um aluno vinculado acessa um prontuário durante a janela configurada do turno da consulta
- **THEN** o acesso é autorizado, desde que as demais regras clínicas sejam atendidas

#### Scenario: Acesso fora da janela configurada
- **WHEN** um aluno vinculado acessa um prontuário fora da janela configurada do turno da consulta
- **THEN** o sistema responde com acesso negado

### Requirement: Validação estrita preservada
O sistema SHALL aplicar as janelas configuradas somente quando a validação estrita de turno estiver habilitada.

#### Scenario: Validação estrita desabilitada
- **WHEN** a validação estrita estiver desabilitada e a consulta for clinicamente elegível
- **THEN** o horário corrente não bloqueia o acesso ao prontuário
