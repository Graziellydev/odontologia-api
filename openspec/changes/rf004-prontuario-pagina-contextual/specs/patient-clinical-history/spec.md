# Spec Delta

## Purpose

Disponibiliza ao atendimento autorizado um histórico clínico cronológico e mínimo, reunindo contexto do paciente, consulta, odontograma, evoluções e adendos.

## ADDED Requirements

### Requirement: Histórico clínico autorizado é consultável por paciente
O sistema SHALL fornecer `GET /api/prontuarios/paciente/{pacienteId}` ao aluno ou coordenador autorizado, retornando identificação clínica mínima do paciente, dados da consulta contextual e registros históricos.

#### Scenario: Consulta autorizada com dados clínicos
- **WHEN** um solicitante autorizado consultar um paciente vinculado
- **THEN** a resposta SHALL incluir nome, idade, código de prontuário, disciplina, cadeira, turno, horário e status do atendimento

#### Scenario: Dados pessoais não necessários
- **WHEN** o histórico for retornado
- **THEN** a resposta SHALL omitir CPF, telefone, endereço e outros identificadores pessoais não necessários ao atendimento

### Requirement: Evoluções e adendos preservam sequência clínica
O sistema SHALL retornar evoluções imutáveis com data, procedimento, elemento, classe quando aplicável, texto clínico, aluno responsável e estado de assinatura. Os adendos SHALL ser agrupados sob seu prontuário de origem e apresentados em ordem de criação.

#### Scenario: Histórico com registros anteriores
- **WHEN** o paciente possuir evoluções anteriores
- **THEN** o sistema SHALL retorná-las em ordem cronológica definida e consistente, identificando os registros assinados

#### Scenario: Histórico sem evoluções
- **WHEN** o paciente não possuir evolução anterior
- **THEN** o sistema SHALL retornar uma coleção vazia sem erro

### Requirement: Odontograma e anamnese refletem o estado disponível
O sistema SHALL retornar o odontograma do paciente e, quando existir, a anamnese mais recente de forma determinística. A ausência de anamnese ou de elementos odontográficos SHALL ser representada sem falha na consulta.

#### Scenario: Paciente com anamnese e odontograma
- **WHEN** houver dados clínicos complementares registrados
- **THEN** a resposta SHALL conter o resumo de anamnese e os elementos do odontograma disponíveis

#### Scenario: Paciente sem anamnese
- **WHEN** não houver anamnese cadastrada
- **THEN** a resposta SHALL manter o histórico acessível e indicar a ausência do resumo

### Requirement: Aba de procedimentos apresenta dados consolidados
O sistema SHALL disponibilizar no mesmo contexto de histórico uma visão de procedimentos e odontograma adequada à aba `Histórico de procedimentos`, sem exigir nova seleção de paciente.

#### Scenario: Troca para histórico de procedimentos
- **WHEN** o usuário autorizado selecionar a aba de procedimentos
- **THEN** a interface SHALL apresentar os procedimentos e condições odontográficas retornados para o paciente atual
