# Spec Delta

## Purpose

Define o ciclo de vida administrativo de vagas individuais da clínica, vinculando cada vaga a uma cadeira, uma data e um turno e preservando a integridade das consultas relacionadas.

## ADDED Requirements

### Requirement: Cadastrar vaga individual sem duplicidade
O sistema SHALL permitir à secretaria cadastrar uma vaga para cadeira ativa, data e turno MANHA ou TARDE. Uma combinação cadeira, data e turno SHALL identificar no máximo uma vaga, conforme `uk_vaga`.

#### Scenario: Cadastro de nova vaga
- **WHEN** a secretaria cadastra uma combinação válida ainda não existente
- **THEN** o sistema cria e retorna a vaga individual

#### Scenario: Combinação já cadastrada
- **WHEN** a secretaria tenta cadastrar uma combinação cadeira, data e turno já existente
- **THEN** o sistema não cria duplicidade e informa conflito

### Requirement: Consultar vagas com filtros
O sistema SHALL listar vagas com filtros opcionais por intervalo de datas, cadeira, turno e situação de bloqueio, incluindo dados da cadeira necessários para identificação.

#### Scenario: Listagem filtrada
- **WHEN** a secretaria solicita vagas com filtros válidos
- **THEN** o sistema retorna somente as vagas que correspondem a todos os filtros informados

#### Scenario: Filtros sem resultados
- **WHEN** nenhum registro corresponde aos filtros
- **THEN** o sistema retorna uma lista vazia

### Requirement: Bloquear vaga com motivo e desbloquear vaga
O sistema SHALL permitir bloquear uma vaga com motivo não vazio de até 200 caracteres e desbloqueá-la removendo o motivo associado. Vagas bloqueadas SHALL deixar de ser oferecidas para agendamento.

#### Scenario: Bloqueio válido
- **WHEN** a secretaria bloqueia uma vaga com motivo válido
- **THEN** a vaga fica bloqueada e o motivo fica disponível na consulta da vaga

#### Scenario: Motivo inválido
- **WHEN** a secretaria tenta bloquear uma vaga com motivo ausente, vazio ou maior que 200 caracteres
- **THEN** o sistema rejeita a operação sem alterar a vaga

#### Scenario: Desbloqueio
- **WHEN** a secretaria desbloqueia uma vaga bloqueada
- **THEN** a vaga fica disponível para avaliação de agendamento e seu motivo de bloqueio é removido

### Requirement: Remover vaga sem consulta vinculada
O sistema SHALL permitir remover uma vaga somente quando nenhuma consulta, ativa ou histórica, referenciar essa vaga. Vaga referenciada SHALL ser preservada.

#### Scenario: Remoção sem referências
- **WHEN** a secretaria remove uma vaga que não possui consultas vinculadas
- **THEN** o sistema remove a vaga

#### Scenario: Remoção com histórico de consulta
- **WHEN** a secretaria tenta remover uma vaga referenciada por qualquer consulta
- **THEN** o sistema mantém a vaga e informa conflito

### Requirement: Restringir gestão de vagas à secretaria
As operações de cadastro, consulta, bloqueio, desbloqueio e remoção em `/api/vagas` e `/api/cadeiras` SHALL exigir sessão autorizada de secretaria.

#### Scenario: Acesso não autorizado
- **WHEN** uma pessoa sem perfil de secretaria acessa uma operação protegida
- **THEN** o sistema nega a operação sem alterar dados
