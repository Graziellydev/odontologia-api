# Proposal

## Why

O backend possui divergências entre código, schema e política de segurança que comprometem a operação clínica e a validação do Hibernate: turnos estão compilados no serviço, campos já criados no banco não são mapeados, e duas entidades atualizam `consultas` com versões concorrentes. Além disso, migrations históricas V2–V9 foram alteradas após sua criação, o que torna o checksum do Flyway incompatível com ambientes já migrados.

## What Changes

- Tornar configuráveis por propriedades os limites de início e fim dos turnos de prontuário, preservando a validação estrita opcional já existente.
- Completar o contrato de paciente para cadastrar, editar e detalhar `uf` e todos os dados de identificação e condição socioeconômica da ficha física; mapear `uf` e `sexo` como `CHAR` explicitamente para Hibernate 6/PostgreSQL.
- **BREAKING** Consolidar o acesso JPA de leitura e atualização da tabela `consultas` em uma única entidade versionada, migrando o fluxo de retornos para esse contrato e removendo o mapeamento concorrente.
- Restaurar V2–V9 ao conteúdo que corresponde às revisões originalmente aplicadas, desabilitar baseline implícito do Flyway e reservar toda nova alteração estrutural para uma V19 sequencial e idempotente.
- Padronizar authorities em `SECRETARIA`, `ALUNO` e `COORDENADOR`, e aplicar defesa em profundidade nas rotas de retornos, metas, catálogo de solicitações e fila de espera.

## Capabilities

### New Capabilities

- `prontuario-horarios-configuraveis`: validação clínica de turno baseada em janelas configuradas por ambiente.
- `cadastro-paciente-ficha-fisica`: persistência e exposição integral dos dados cadastrais e da ficha física do paciente.
- `consultas-mapeamento-unificado`: acesso consistente e versionado à tabela de consultas por todos os fluxos clínicos.
- `integridade-migrations-flyway`: preservação de checksum histórico e evolução estrutural somente por migration sequencial nova.
- `autorizacao-perfis-api`: convenção única de authorities e proteção explícita das rotas clínicas e acadêmicas.

### Modified Capabilities

- Nenhuma. O repositório ainda não possui capabilities OpenSpec publicadas.

## Impact

- Configuração em `application.properties` e propriedades do prontuário; testes de fronteira de horário.
- `Paciente`, contrato HTTP atualmente exposto diretamente pelo controller, serviço/repositório de pacientes e os fluxos de triagem que usam seus dados.
- `Consulta`, `ConsultaRetorno`, seus repositories/services e testes de agendamento/retorno, preservando trigger de histórico e optimistic locking.
- Migrations `V2`–`V9`, configuração Flyway e, se a reconciliação exigir mudança de schema, uma única `V19__*.sql` idempotente.
- `SecurityConfig` e controllers de retornos, metas, catálogo de solicitações e fila de espera, com testes de autorização 401/403.
