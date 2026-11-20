# Spec Delta

## Purpose

Registra a evolução clínica de uma consulta com autoria e data rastreáveis, preserva o conteúdo assinado e permite documentar complementos sem sobrescrever o registro original.

## ADDED Requirements

### Requirement: Aluno salva uma evolução clínica por consulta
O sistema SHALL expor `POST /api/prontuarios` a aluno autorizado e persistir uma única evolução por consulta. A autoria e os vínculos SHALL derivar da sessão autenticada e da consulta, e os dados SHALL respeitar o schema existente.

#### Scenario: Evolução válida é registrada
- **GIVEN** o aluno está autorizado e a consulta pertence a ele, ao paciente e ao turno atual
- **WHEN** envia `consultaId`, `procedimentoId`, `evolucaoClinica` não vazia e `concluirAtendimento`; `dente` pode ser omitido ou seguir o formato FDI de V6 e `classe` pode ser omitida ou estar entre `I` e `VI`
- **THEN** o sistema insere um registro em `prontuarios` com autoria da sessão e data gerada pelo banco

#### Scenario: Evolução clínica vazia
- **GIVEN** o aluno está autorizado
- **WHEN** envia `evolucaoClinica` ausente, vazia ou composta somente por espaços
- **THEN** o sistema responde HTTP 400 e não cria prontuário

#### Scenario: Dente ou classe viola o formato do banco
- **GIVEN** o aluno está autorizado
- **WHEN** envia dente fora do formato FDI ou classe fora de `I` a `VI`
- **THEN** o sistema responde HTTP 400 e nenhum dado parcial é gravado

#### Scenario: Já existe prontuário para a consulta
- **GIVEN** `prontuarios.consulta_id` já referencia a consulta enviada
- **WHEN** o aluno tenta registrar outra evolução para a mesma consulta
- **THEN** o sistema responde HTTP 409 e preserva o prontuário original

#### Scenario: Usuário não é aluno autorizado
- **GIVEN** o usuário não tem perfil `ALUNO` autorizado para a consulta
- **WHEN** envia `POST /api/prontuarios`
- **THEN** o sistema responde HTTP 403 e não insere o registro

### Requirement: Assinatura temporal identifica o conteúdo e o autor
Ao inserir uma evolução, o sistema SHALL gerar no servidor um selo temporal versionado em `assinatura_digital`, com hash SHA-256, timestamp ISO-8601 e identidade do aluno. O cliente não SHALL fornecer nem substituir esse selo.

#### Scenario: Assinatura é gerada no servidor
- **GIVEN** uma evolução válida será persistida
- **WHEN** o sistema calcula o selo temporal
- **THEN** a assinatura inclui formato identificável, hash SHA-256 da representação canônica UTF-8 de consulta, procedimento, dente, classe, evolução e identidade do aluno, instante ISO-8601 com fuso e matrícula ou identificação autenticada, usando os mesmos valores e instante da persistência

#### Scenario: Tentativa de enviar assinatura pelo cliente
- **GIVEN** a requisição inclui campo adicional de assinatura
- **WHEN** o servidor processa a evolução
- **THEN** ignora ou rejeita o campo adicional e gera a assinatura com valores confiáveis do servidor

### Requirement: Concluir atendimento atualiza status com origem e autoria
Quando `concluirAtendimento=true`, o sistema SHALL atualizar a consulta para `REALIZADO` na mesma transação da criação do prontuário e SHALL definir `app.origem='ALUNO'` e `app.usuario_id` para o ID do aluno autenticado antes da atualização, permitindo que `trg_consulta_historico` grave a autoria correta. Quando `false`, SHALL manter o status atual da consulta. Falha em qualquer operação SHALL reverter a transação inteira.

#### Scenario: Aluno conclui atendimento
- **GIVEN** a consulta está apta a ser concluída e a evolução é válida
- **WHEN** envia `concluirAtendimento=true`
- **THEN** o prontuário é inserido, a consulta passa a `REALIZADO` e o histórico automático identifica origem `ALUNO` e usuário autenticado

#### Scenario: Evolução sem concluir atendimento
- **GIVEN** a consulta está apta a receber evolução
- **WHEN** envia `concluirAtendimento=false`
- **THEN** o prontuário é inserido e o status da consulta permanece inalterado

#### Scenario: Erro ao atualizar consulta
- **GIVEN** a inserção da evolução foi iniciada com `concluirAtendimento=true`
- **WHEN** a atualização da consulta ou o trigger falha
- **THEN** a transação não deixa prontuário parcial nem atualização de status parcial

### Requirement: Observações opcionais do odontograma respeitam sua chave única
Se a requisição incluir observações de odontograma, o sistema SHALL atualizá-las em `paciente_odontograma` usando o paciente e os valores de dente/face enviados, respeitando `uk_paciente_dente_face` com face nula equivalente a `GERAL`, sem criar colunas ou chaves alternativas. A gravação SHALL ocorrer na mesma transação da evolução; dados inválidos SHALL impedir gravação parcial.

#### Scenario: Observação de dente é incluída
- **GIVEN** a evolução é válida e contém observação de um dente/face com formato válido
- **WHEN** o servidor salva a operação
- **THEN** a observação atual correspondente à chave paciente/dente/face é atualizada sem duplicar a linha única

#### Scenario: Observação inválida
- **GIVEN** a evolução inclui uma observação com dente ou face inválidos
- **WHEN** o servidor valida a operação
- **THEN** responde HTTP 400 e não persiste prontuário nem alteração parcial do odontograma

### Requirement: Adendo complementa sem alterar evolução assinada
O sistema SHALL expor `POST /api/prontuarios/{prontuarioId}/adendos` a aluno com vínculo direto na consulta em `CURRENT_DATE` e no turno atual, aceitando consulta `REALIZADO` além dos status ativos. O endpoint SHALL inserir apenas uma linha de adendo, sem alterar prontuário ou adendos existentes.

#### Scenario: Adendo pós-assinatura é incluído
- **GIVEN** existe prontuário e consulta `AGENDADO`, `CONFIRMADA_24H` ou `REALIZADO` diretamente vinculada ao aluno e paciente em `CURRENT_DATE` no turno atual
- **WHEN** envia texto de adendo não vazio
- **THEN** o sistema insere um adendo com autor e data gerados pelo servidor e mantém o prontuário original intacto

#### Scenario: Prontuário inexistente
- **GIVEN** o identificador não corresponde a prontuário existente
- **WHEN** o aluno envia um adendo
- **THEN** o sistema responde HTTP 404 e não cria linha de adendo

#### Scenario: Adendo vazio ou usuário não autorizado
- **GIVEN** o texto está vazio ou o usuário não satisfaz as regras de autorização
- **WHEN** solicita a inclusão do adendo
- **THEN** o sistema responde HTTP 400 para texto vazio ou HTTP 403 para acesso negado e não altera dados clínicos
