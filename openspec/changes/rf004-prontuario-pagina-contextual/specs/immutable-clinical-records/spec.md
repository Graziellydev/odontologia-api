# Spec Delta

## Purpose

Registra evolução clínica e adendos como eventos imutáveis, associa um selo temporal à evolução e coordena a conclusão auditável do atendimento.

## ADDED Requirements

### Requirement: Evolução clínica é registrada uma vez por consulta
O sistema SHALL aceitar `POST /api/prontuarios` com consulta, procedimento, elemento ou região, classe opcional, evolução obrigatória e opção de conclusão. A consulta, o aluno, o paciente e a turma SHALL ser derivados e validados no servidor.

#### Scenario: Registro válido de evolução
- **WHEN** o aluno autorizado enviar evolução clínica válida para consulta sem prontuário
- **THEN** o sistema SHALL criar o registro e retornar sua identificação, data e selo temporal

#### Scenario: Segunda evolução para a mesma consulta
- **WHEN** houver prontuário já vinculado à consulta
- **THEN** o sistema SHALL rejeitar a nova criação com conflito, preservando a unicidade da consulta

### Requirement: Evolução recebe selo temporal não editável
O sistema SHALL gerar no servidor uma assinatura digital temporal versionada, baseada em SHA-256 dos dados clínicos canônicos, timestamp ISO-8601 e identificação ou matrícula do aluno. O cliente SHALL não fornecer esse valor.

#### Scenario: Registro recém-salvo
- **WHEN** a evolução for persistida
- **THEN** a resposta SHALL identificar o selo `Assinatura Digital Temporal Não Editável` e os metadados de autoria e tempo

### Requirement: Prontuário e adendos são append-only
O sistema SHALL criar adendo por `POST /api/prontuarios/{prontuarioId}/adendos` com texto não vazio e autor autenticado, sem modificar o registro original. Atualização e exclusão de prontuários ou adendos SHALL permanecer bloqueadas pelas regras do banco.

#### Scenario: Adendo após assinatura
- **WHEN** o aluno vinculado inserir texto complementar em prontuário elegível
- **THEN** o sistema SHALL criar um novo adendo e manter a evolução assinada inalterada

#### Scenario: Alteração direta de registro clínico
- **WHEN** uma atualização ou exclusão for tentada sobre prontuário ou adendo
- **THEN** o banco SHALL rejeitar a operação por suas regras de imutabilidade

### Requirement: Conclusão da consulta é auditada e atômica
O sistema SHALL concluir a consulta como `REALIZADO` quando solicitado com a evolução, registrando autor e origem `ALUNO` na trilha de auditoria. Falha em qualquer etapa da gravação SHALL impedir estado clínico parcialmente salvo.

#### Scenario: Evolução com conclusão
- **WHEN** o aluno salvar evolução com `concluirAtendimento=true`
- **THEN** a consulta SHALL ficar `REALIZADO` e sua trilha SHALL registrar o aluno e a origem clínica

#### Scenario: Falha durante persistência composta
- **WHEN** a persistência de evolução, odontograma ou conclusão falhar
- **THEN** o sistema SHALL reverter as alterações da operação composta

### Requirement: Encerramento do atendimento reutiliza o contrato RF003
O sistema SHALL integrar o card de encerramento ao endpoint de retorno existente. `Necessita Retorno` SHALL exigir justificativa técnica e preservar procedimento, dente e classe na fila; `Tratamento Concluído` SHALL encerrar sem criar retorno.

#### Scenario: Retorno com justificativa
- **WHEN** o aluno encerrar selecionando `Necessita Retorno` e informar justificativa técnica
- **THEN** o sistema SHALL concluir o atendimento e incluir a demanda na fila para agendamento da secretaria

#### Scenario: Retorno sem justificativa
- **WHEN** o aluno selecionar `Necessita Retorno` sem justificativa técnica
- **THEN** o sistema SHALL rejeitar o encerramento e manter a consulta aberta
