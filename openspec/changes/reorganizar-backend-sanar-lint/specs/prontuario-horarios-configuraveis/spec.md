# Spec Delta

## Purpose

Permite ajustar por configuração as janelas de acesso clínico ao prontuário sem recompilar a aplicação.

## ADDED Requirements

### Requirement: Janelas de turno configuráveis
O sistema SHALL usar por padrão MANHA 07:30–12:00 e TARDE 13:30–18:00, aceitando override por configuração.

#### Scenario: Acesso dentro do turno
- **WHEN** aluno vinculado acessa dentro do turno configurado
- **THEN** o acesso é permitido se as outras condições clínicas forem satisfeitas

#### Scenario: Limites e fora do turno
- **WHEN** o acesso ocorre antes do início ou no fim exclusivo da janela
- **THEN** o sistema nega o acesso
