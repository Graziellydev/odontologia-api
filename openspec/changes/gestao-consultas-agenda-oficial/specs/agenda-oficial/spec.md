# Spec Delta

## Purpose

Oferece à secretaria uma visão semanal confiável da agenda e operações oficiais para criar, remarcar e cancelar consultas, conectando cada atendimento à fila de espera e à vaga da cadeira.

## ADDED Requirements

### Requirement: Consultar grade semanal da agenda
O sistema SHALL fornecer a grade dos sete dias da semana para o período selecionado, filtrável por data, disciplina e disponibilidade, identificando cadeira, turno, paciente, aluno, procedimento e estado do atendimento conforme aplicável. A interface SHALL permitir alcançar os dias de fim de semana mantendo a hierarquia visual do Figma.

#### Scenario: Grade semanal com consulta
- **WHEN** a secretaria consulta uma semana contendo consulta ativa
- **THEN** a grade exibe a cadeira e o turno com os dados do atendimento e seu status

#### Scenario: Grade semanal com vaga livre
- **WHEN** a secretaria consulta uma cadeira e turno sem consulta ativa e sem bloqueio
- **THEN** a grade identifica o espaço como “Vaga livre” e permite iniciar agendamento

#### Scenario: Filtro de disciplina e disponibilidade
- **WHEN** a secretaria informa filtros válidos de disciplina ou disponibilidade
- **THEN** a grade retorna somente os itens correspondentes sem alterar consultas

### Requirement: Criar consulta vinculada à fila de espera
O sistema SHALL permitir à secretaria criar uma consulta para uma demanda elegível da fila, selecionando aluno, turma, cadeira, data e turno. Caso a combinação de cadeira, data e turno ainda não exista em vagas, o sistema SHALL criá-la automaticamente antes de associar a consulta.

#### Scenario: Criar consulta e vaga sob demanda
- **WHEN** a secretaria agenda uma demanda elegível em uma cadeira, data e turno sem vaga cadastrada
- **THEN** o sistema cria a vaga e a consulta vinculada à vaga e à entrada da fila

#### Scenario: Reutilizar vaga existente sem consulta ativa
- **WHEN** a secretaria agenda em uma vaga existente, desbloqueada e sem consulta ativa
- **THEN** o sistema associa a nova consulta à vaga existente

#### Scenario: Impedir conflito de cadeira
- **WHEN** uma operação concorrente tenta criar ou agendar a mesma cadeira, data e turno
- **THEN** somente uma vaga é mantida conforme `uk_vaga` e a outra operação recebe conflito sem duplicar agendamento

#### Scenario: Impedir conflito de paciente ou aluno
- **WHEN** o paciente ou aluno já possui consulta ativa no mesmo dia e turno
- **THEN** o sistema rejeita o agendamento e preserva a fila e as consultas existentes

### Requirement: Remarcar consulta preservando origem
O sistema SHALL remarcar uma consulta ativa criando uma nova consulta vinculada à consulta original e a uma vaga disponível, preservando o registro original no histórico.

#### Scenario: Remarcação concluída
- **WHEN** a secretaria remarca uma consulta ativa para uma vaga disponível
- **THEN** o sistema preserva a consulta original, cria uma nova consulta com referência à origem e atualiza a agenda

#### Scenario: Destino indisponível
- **WHEN** a secretaria tenta remarcar para uma vaga bloqueada, ocupada ou inexistente
- **THEN** o sistema rejeita a operação e mantém ativa a consulta original

### Requirement: Cancelar consulta e devolver demanda à fila
O sistema SHALL permitir à secretaria cancelar uma consulta ativa vinculada à fila, preservar seu registro e devolver a demanda à fila conforme suas regras de elegibilidade.

#### Scenario: Cancelamento e retorno à fila
- **WHEN** a secretaria cancela uma consulta ativa com demanda de fila vinculada
- **THEN** o sistema preserva o histórico, marca a consulta como cancelada, libera a vaga para agendamento futuro e devolve a demanda à fila

#### Scenario: Consulta sem vínculo com fila
- **WHEN** a secretaria tenta cancelar uma consulta sem demanda de fila vinculada
- **THEN** o sistema rejeita a devolução automática e mantém o estado da consulta

### Requirement: Exigir perfil de secretaria nas operações oficiais
Consultar a grade e criar, remarcar ou cancelar consultas pela agenda oficial SHALL exigir sessão autorizada de secretaria.

#### Scenario: Acesso por perfil não autorizado
- **WHEN** uma pessoa sem perfil de secretaria solicita uma operação da agenda oficial
- **THEN** o sistema nega a operação e não altera agenda ou fila
