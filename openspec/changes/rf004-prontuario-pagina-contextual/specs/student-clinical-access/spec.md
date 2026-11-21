# Spec Delta

## Purpose

Controla o acesso clínico ao prontuário por rede institucional, vínculo de atendimento e turno, preservando a auditoria somente leitura do coordenador.

## ADDED Requirements

### Requirement: Acesso clínico exige rede local autorizada
O sistema SHALL verificar o IP efetivo de toda requisição a `/api/prontuarios/**` contra as faixas CIDR configuradas. A origem em `X-Forwarded-For` SHALL ser considerada somente quando a conexão imediata vier de proxy confiável.

#### Scenario: IP fora da rede local
- **WHEN** uma requisição ao prontuário vier de IP não pertencente às faixas permitidas
- **THEN** o sistema SHALL responder HTTP 403 com a mensagem exata `Acesso restrito a dispositivos autorizados na rede local`

#### Scenario: Cabeçalho encaminhado não confiável
- **WHEN** um cliente fora de proxy confiável enviar `X-Forwarded-For`
- **THEN** o sistema SHALL usar o endereço remoto da conexão para a decisão de rede

### Requirement: Aluno só acessa paciente do turno vinculado
O sistema SHALL exigir que o aluno autenticado tenha consulta diretamente vinculada ao paciente em vaga da data corrente e do turno aplicável. Com validação estrita, `MANHA` compreende 08:00–12:00 e `TARDE` 13:00–17:00; fora dela o acesso SHALL ser negado.

#### Scenario: Criação durante atendimento aberto
- **WHEN** o aluno criar evolução de consulta própria `AGENDADO` ou `CONFIRMADA_24H` na data e turno válidos
- **THEN** o sistema SHALL permitir a operação clínica

#### Scenario: Paciente não vinculado
- **WHEN** o aluno consultar, registrar ou complementar prontuário de paciente sem consulta própria válida
- **THEN** o sistema SHALL responder HTTP 403 sem devolver dados clínicos

#### Scenario: Leitura e adendo após realização
- **WHEN** a consulta própria estiver `REALIZADO` na mesma data e turno
- **THEN** o sistema SHALL permitir leitura do histórico e inclusão de adendo, mas não nova evolução inicial

### Requirement: Validação de turno é configurável sem remover o vínculo
O sistema SHALL disponibilizar a configuração `app.prontuario.validar-turno-estrito`. Quando desativada, apenas a janela de relógio poderá ser ignorada; rede, vínculo, data e status continuarão obrigatórios.

#### Scenario: Execução de teste fora do horário clínico
- **WHEN** a validação estrita estiver desativada e os demais requisitos forem satisfeitos
- **THEN** o sistema SHALL autorizar a operação fora das janelas de manhã e tarde

### Requirement: Coordenador possui auditoria somente leitura
O sistema SHALL permitir ao perfil `COORDENADOR` consultar histórico de prontuário autorizado pela rede, e SHALL negar a esse perfil qualquer criação de evolução, adendo ou encerramento clínico.

#### Scenario: Coordenador consulta prontuário
- **WHEN** um coordenador autorizado solicitar histórico de paciente
- **THEN** o sistema SHALL retornar somente os dados previstos para auditoria

#### Scenario: Coordenador tenta escrita clínica
- **WHEN** um coordenador enviar requisição de escrita ao prontuário
- **THEN** o sistema SHALL responder HTTP 403
