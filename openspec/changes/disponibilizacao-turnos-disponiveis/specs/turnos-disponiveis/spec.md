# Spec Delta

## Purpose

Permite à secretaria preparar disponibilidade de atendimento para as cadeiras ativas e consultar, ao longo de uma semana ou período, cada turno livre, ocupado ou bloqueado.

## ADDED Requirements

### Requirement: Gerar turnos para todas as cadeiras ativas no período
O sistema SHALL permitir gerar vagas de MANHA (08h às 12h) e TARDE (13h às 17h) para cada cadeira ativa em todas as datas do período solicitado, inclusive fins de semana. A repetição da geração SHALL respeitar `uk_vaga (cadeira_id, data, turno)` sem criar duplicatas.

#### Scenario: Gerar período completo
- **WHEN** a secretaria gera turnos para um período válido
- **THEN** o sistema cria uma vaga por cadeira ativa, data e turno em todos os dias do período

#### Scenario: Cadeira inativa
- **WHEN** a geração inclui datas em que uma cadeira está inativa
- **THEN** o sistema não cria vagas para essa cadeira

#### Scenario: Repetir geração
- **WHEN** a secretaria gera novamente um período já parcialmente ou totalmente preparado
- **THEN** o sistema mantém uma única vaga por chave cadeira, data e turno e informa o resultado sem erro por duplicidade

### Requirement: Consultar grade semanal por cadeira e turno
O sistema SHALL apresentar uma grade para cada dia da semana, inclusive sábados e domingos, com linhas ou cartões de MANHA (08h às 12h) e TARDE (13h às 17h), identificando o estado de cada cadeira ativa.

#### Scenario: Consulta ativa no turno
- **WHEN** existe consulta ativa associada à vaga da cadeira e turno
- **THEN** a grade apresenta o atendimento e seu estado em vez de “Vaga livre”

#### Scenario: Sem consulta ativa e sem bloqueio
- **WHEN** uma cadeira ativa não possui consulta ativa no dia e turno e não há bloqueio
- **THEN** a grade exibe “Vaga livre” mesmo se ainda não existir linha correspondente em `vagas`

#### Scenario: Turno bloqueado
- **WHEN** a vaga da cadeira e turno está bloqueada
- **THEN** a grade identifica o turno como bloqueado e não permite agendamento

### Requirement: Bloquear ou excluir turno sem consulta
O sistema SHALL permitir bloquear um turno somente quando nenhuma consulta, ativa ou histórica, estiver vinculada à vaga, mediante motivo de até 200 caracteres. A exclusão SHALL seguir a mesma condição de ausência de referências.

#### Scenario: Bloquear turno livre
- **WHEN** a secretaria bloqueia um turno sem qualquer consulta vinculada com motivo válido
- **THEN** a grade exibe o motivo e o turno deixa de ser oferecido como livre

#### Scenario: Impedir bloqueio de turno com consulta vinculada
- **WHEN** a secretaria tenta bloquear um turno referenciado por qualquer consulta, inclusive cancelada
- **THEN** o sistema rejeita a alteração e preserva a vaga e o histórico

#### Scenario: Excluir turno sem referências
- **WHEN** a secretaria exclui uma vaga sem consultas vinculadas
- **THEN** o sistema remove a vaga e a grade continua exibindo o espaço como “Vaga livre”

#### Scenario: Impedir exclusão com consulta histórica
- **WHEN** a secretaria tenta excluir uma vaga que já foi referenciada por consulta, mesmo cancelada
- **THEN** o sistema preserva a vaga e informa conflito

### Requirement: Restringir manutenção da disponibilidade à secretaria
Gerar, consultar, bloquear ou excluir turnos pela gestão de disponibilidade SHALL exigir sessão autorizada de secretaria.

#### Scenario: Acesso não autorizado
- **WHEN** uma pessoa sem perfil de secretaria solicita uma operação protegida
- **THEN** o sistema nega a operação sem alterar a disponibilidade
