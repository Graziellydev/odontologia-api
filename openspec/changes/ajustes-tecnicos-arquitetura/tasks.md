# Tasks

## 1. Configuração de horário clínico

- [x] 1.1 Estender as propriedades de prontuário com quatro limites `LocalTime`, defaults 07:30/12:00 e 13:30/18:00, e validação de intervalo; verificar com testes de binding e configurações inválidas.
- [x] 1.2 Substituir os horários fixos da autorização de prontuário pelas janelas configuradas, mantendo `America/Sao_Paulo` e a flag de validação estrita; verificar limites inclusivo/exclusivo para MANHA e TARDE em teste unitário.
- [x] 1.3 Documentar as quatro chaves em `application.properties` e conferir que um override de perfil altera a decisão sem recompilação.

## 2. Contrato e mapeamento de paciente

- [ ] 2.1 Mapear em `Paciente` UF e todos os campos V9 com nomes, limites e tipos do schema, incluindo tipo JDBC CHAR explícito para UF e sexo; verificar com inicialização Hibernate `ddl-auto=validate` contra PostgreSQL.
- [ ] 2.2 Criar DTOs de cadastro, edição e detalhe que cubram dados cadastrais e ficha física, e adaptar controller/service para conversão sem expor campos internos; verificar contratos JSON de POST, PUT, GET por id e listagem.
- [ ] 2.3 Atualizar os fluxos de triagem que consomem ou retornam paciente para usar o contrato completo aplicável; verificar que UF e campos físicos sobrevivem ao fluxo de triagem.
- [ ] 2.4 Adicionar testes de persistência e API para criação e atualização dos campos V9/V13, incluindo `sexo` e UF; verificar ausência de regressão para cadastro mínimo existente.

## 3. Representação única de consultas

- [x] 3.1 Migrar o fluxo de retornos e seu repository para a entidade canônica `Consulta`, removendo o segundo mapeamento mutável de `consultas`; verificar compilação e ausência de outra entidade versionada para a tabela.
- [x] 3.2 Adaptar o fluxo de retorno para o enum/status e a versão da entidade canônica, preservando contexto do trigger de histórico; verificar os cenários sem retorno e com retorno na fila.
- [ ] 3.3 Traduzir conflitos de optimistic locking da consulta em resposta HTTP 409 consistente; verificar com teste transacional de duas atualizações concorrentes.
- [ ] 3.4 Executar os testes de agendamento, remarcação, falta e retorno para confirmar preservação de contratos e integridade da fila.

## 4. Integridade de migrations e Flyway

- [ ] 4.1 Comparar blobs Git de V2–V9 com a revisão de introdução `6ca080d050d07d98042976682b1867361bcd361b` e com `flyway_schema_history` do Supabase, sem ler ou modificar `.env`; registrar versões e checksums obtidos antes de qualquer restauração.
- [ ] 4.2 Restaurar V2–V9 somente aos bytes cuja correspondência com o histórico aplicado foi confirmada; verificar `git diff --check` e Flyway `validate` contra banco representativo.
- [ ] 4.3 Definir `spring.flyway.baseline-on-migrate=false` nas configurações que habilitam Flyway; verificar que um banco não vazio sem histórico não recebe baseline automático.
- [ ] 4.4 Criar exclusivamente `V19__*.sql` caso a reconciliação revele ajuste estrutural remanescente, usando DDL idempotente; verificar em schema limpo e em schema já ajustado. Não criar V19 se não houver delta estrutural.
- [ ] 4.5 Executar validação Flyway e Hibernate contra PostgreSQL/Supabase de teste com credenciais fornecidas pelo ambiente, mantendo `.env` intocado; registrar o resultado dos checksums e de `ddl-auto=validate`.

## 5. Convenção de autorização

- [ ] 5.1 Padronizar produtores e verificações de authority em `SECRETARIA`, `ALUNO` e `COORDENADOR`, removendo alternativas `ROLE_*`; verificar autenticação de cada perfil em teste de segurança.
- [ ] 5.2 Declarar regras de rota e `@PreAuthorize` equivalentes para `/api/retornos/**`, `/api/metas/**` e `/api/catalogo-solicitacoes/**` com acesso ALUNO; verificar respostas 401 para anônimo e 403 para perfil incorreto.
- [ ] 5.3 Declarar regra de rota e `@PreAuthorize` equivalente para `/api/fila-espera/**` com acesso SECRETARIA; verificar respostas 401/403 e acesso permitido da secretaria.
- [ ] 5.4 Executar a suíte de segurança e os testes de controller afetados para confirmar que validações de posse de recurso continuam como defesa adicional.

## 6. Validação integrada

- [ ] 6.1 Executar a suíte Maven pertinente e os perfis de validação PostgreSQL disponíveis, verificando build, Flyway, Hibernate e todos os cenários das cinco specs.
- [ ] 6.2 Revisar a documentação de configuração e o diff final para confirmar que somente código, properties, migrations permitidas e testes foram alterados, e que `.env` não foi lido nem modificado.
