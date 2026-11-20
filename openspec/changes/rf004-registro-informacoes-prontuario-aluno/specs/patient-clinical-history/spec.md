# Spec Delta

## Purpose

Oferece ao aluno com consulta autorizada e ao coordenador em auditoria uma visão de leitura do contexto clínico do paciente, do atendimento corrente e do histórico de evoluções.

## ADDED Requirements

### Requirement: Histórico clínico do paciente
O sistema SHALL expor `GET /api/prontuarios/paciente/{pacienteId}` com identificação básica do paciente, anamnese resumida, odontograma atual, evoluções anteriores em ordem cronológica e dados da consulta ativa do turno. Cada evolução SHALL apresentar procedimento, dente e classe quando preenchidos, texto clínico, aluno, assinatura digital armazenada e data; cada adendo SHALL aparecer associado à evolução correspondente e incluir autor, texto e data. A resposta SHALL ser somente de leitura.

#### Scenario: Aluno autorizado consulta o prontuário
- **GIVEN** o aluno autenticado está na rede permitida e tem consulta `AGENDADO`, `CONFIRMADA_24H` ou `REALIZADO` diretamente vinculada ao paciente em `CURRENT_DATE` no turno atual
- **WHEN** solicita `GET /api/prontuarios/paciente/{pacienteId}`
- **THEN** recebe identificação básica, resumo de anamnese, odontograma, histórico cronológico com adendos e dados da consulta ativa

#### Scenario: Histórico vazio
- **GIVEN** o paciente não possui evoluções anteriores, embora tenha consulta ativa vinculada
- **WHEN** o aluno consulta o endpoint
- **THEN** recebe os dados disponíveis e uma coleção de evoluções vazia

#### Scenario: Paciente sem anamnese cadastrada
- **GIVEN** não há linhas em `paciente_anamnese` para o paciente
- **WHEN** o aluno consulta o endpoint
- **THEN** o resumo de anamnese é informado como ausente sem impedir o retorno do odontograma, histórico e consulta

#### Scenario: Paciente tem mais de uma anamnese
- **GIVEN** existem várias linhas em `paciente_anamnese` para o paciente
- **WHEN** o endpoint monta o resumo
- **THEN** retorna a anamnese mais recentemente atualizada, com `criado_em` como critério secundário de ordenação, sem duplicar os demais dados do paciente

#### Scenario: Evoluções e adendos são organizados cronologicamente
- **GIVEN** o paciente possui múltiplos prontuários e adendos
- **WHEN** o aluno consulta o histórico
- **THEN** as evoluções são ordenadas por `data_registro` decrescente e os adendos são retornados dentro do prontuário a que pertencem, ordenados por `criado_em` crescente

#### Scenario: Consulta não está autorizada ao aluno
- **GIVEN** o aluno não atende às regras de rede, vínculo ou turno
- **WHEN** solicita o histórico do paciente
- **THEN** recebe HTTP 403 e nenhum dado clínico do paciente

#### Scenario: Coordenador acessa o histórico para auditoria
- **GIVEN** o usuário tem perfil `COORDENADOR` e está em rede permitida
- **WHEN** consulta o histórico
- **THEN** recebe os dados de leitura sem vínculo de atendimento
