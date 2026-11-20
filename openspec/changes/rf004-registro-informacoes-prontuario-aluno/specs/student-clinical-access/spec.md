# Spec Delta

## Purpose

Restringe as operações clínicas do prontuário aos dispositivos da rede local autorizada e ao aluno que tem consulta ativa com o paciente no turno corrente.

## ADDED Requirements

### Requirement: Requisições clínicas devem vir de rede local autorizada
O sistema SHALL comparar o IP cliente com `app.prontuario.allowed-cidrs`, incluindo loopbacks por padrão. Só SHALL confiar em `X-Forwarded-For` quando recebido de proxy confiável; nos demais casos SHALL validar `remoteAddr`. IP não permitido SHALL receber HTTP 403 e a mensagem `Acesso restrito a dispositivos autorizados na rede local`.

#### Scenario: Requisição usa endereço permitido
- **GIVEN** o IP cliente pertence a uma faixa CIDR configurada
- **WHEN** o usuário autenticado solicita histórico, evolução ou adendo
- **THEN** a validação de rede permite que as demais regras de autorização sejam avaliadas

#### Scenario: Requisição vem de IP fora das faixas
- **GIVEN** o IP cliente não pertence a nenhuma faixa CIDR configurada
- **WHEN** o usuário solicita histórico, evolução ou adendo
- **THEN** o sistema responde HTTP 403 com a mensagem `Acesso restrito a dispositivos autorizados na rede local`

#### Scenario: Cabeçalho de encaminhamento não vem de proxy confiável
- **GIVEN** a requisição contém `X-Forwarded-For`, mas a conexão não vem de proxy confiável
- **WHEN** o sistema determina o IP cliente
- **THEN** o valor não confiável do cabeçalho é ignorado e `remoteAddr` é validado

### Requirement: Aluno só pode acessar paciente vinculado à consulta ativa do turno
O sistema SHALL exigir vínculo direto entre paciente, aluno e consulta em `CURRENT_DATE` no turno vigente. A leitura do histórico e a criação de adendos SHALL aceitar os status `AGENDADO`, `CONFIRMADA_24H` e `REALIZADO`; a criação inicial da evolução SHALL aceitar somente `AGENDADO` ou `CONFIRMADA_24H`. A flag `app.prontuario.validar-turno-estrito` controla apenas a validação do horário; negações SHALL responder HTTP 403 com mensagem clara de restrição de vínculo ou turno.

#### Scenario: Aluno está vinculado ao paciente no turno atual
- **GIVEN** existe consulta `AGENDADO` ou `CONFIRMADA_24H` para o paciente e o aluno autenticado, na data corrente e no turno compatível com o horário atual
- **WHEN** o aluno cria a evolução inicial
- **THEN** a regra de vínculo e turno autoriza a operação

#### Scenario: Aluno consulta ou adenda registro de consulta realizada
- **GIVEN** existe consulta `REALIZADO` diretamente vinculada ao paciente e ao aluno autenticado em `CURRENT_DATE` no turno compatível com o horário atual
- **WHEN** o aluno consulta o histórico ou cria adendo para o prontuário daquela consulta
- **THEN** a regra de vínculo e turno autoriza a leitura ou adendo, mas não uma segunda evolução inicial

#### Scenario: Aluno não tem consulta elegível para a operação
- **GIVEN** não existe consulta elegível que vincule o paciente ao aluno autenticado na data corrente (`AGENDADO`/`CONFIRMADA_24H` para criação ou também `REALIZADO` para leitura/adendo)
- **WHEN** o aluno tenta consultar, criar evolução ou adendo
- **THEN** o sistema responde HTTP 403 com mensagem de restrição de vínculo clínico ou turno

#### Scenario: Consulta existe, mas o horário não pertence ao turno
- **GIVEN** a consulta tem status `AGENDADO` ou `CONFIRMADA_24H`, a vaga tem `data = CURRENT_DATE`, a flag estrita está habilitada e o horário não pertence à janela de seu turno (`MANHA` 08:00–12:00 ou `TARDE` 13:00–17:00, início inclusivo e fim exclusivo)
- **WHEN** o aluno tenta acessar ou registrar o prontuário
- **THEN** o sistema responde HTTP 403 com mensagem de restrição de vínculo clínico ou turno

#### Scenario: Teste executado fora do horário comercial
- **GIVEN** a flag estrita está desabilitada e existe consulta vinculada ao aluno e paciente para `CURRENT_DATE` com status permitido para a operação
- **WHEN** o teste acessa o prontuário fora das janelas do turno
- **THEN** o horário não bloqueia a operação

### Requirement: Coordenador pode consultar histórico para auditoria
O sistema SHALL permitir que o perfil existente `COORDENADOR` consulte o histórico clínico para auditoria sem exigir vínculo próprio com o paciente, mantendo a validação de rede local. O coordenador SHALL ter acesso de leitura e não SHALL criar evoluções nem adendos por esta autorização.

#### Scenario: Coordenador consulta histórico em rede permitida
- **GIVEN** o usuário autenticado tem perfil `COORDENADOR` e o IP cliente é permitido
- **WHEN** solicita o histórico clínico do paciente
- **THEN** recebe acesso somente de leitura sem validação de consulta vinculada

#### Scenario: Coordenador tenta registrar evolução
- **GIVEN** o usuário autenticado tem perfil `COORDENADOR`
- **WHEN** envia uma evolução ou um adendo
- **THEN** o sistema responde HTTP 403
