# Relatorio de testes e auditoria de seguranca

**Data:** 14/11/2026  
**Escopo:** testes locais da API, verificacao de seguranca e build do frontend.

## Resultado da execucao

| Area | Testes descobertos | Resultado |
|---|---:|---|
| Cadeiras, vagas, turnos e consultas | 31 | Passou; testes de servico, controller e SQL |
| Agenda, faltas e notificacoes | 12 | Passou; testes de servico |
| Autenticacao e autorizacao | 5 | Passou; BCrypt, migracao de senha legada e RBAC via MockMvc |
| Solicitacoes de pacientes | 41 | Passou; controllers, regras e permissoes |
| Fila de espera | 10 | Passou |
| Retornos | 10 | Passou |
| Metas e metricas | 24 | Passou |
| Inicializacao local do contexto | 1 | Passou usando H2 isolado |
| Auditoria de migrations/triggers no Supabase | 1 | Ignorado: `SUPABASE_TEST_JDBC_URL` nao configurada |
| **Total** | **135** | **134 executados, 134 aprovados, 0 falhas, 1 ignorado** |

Comandos executados:

- `backend/./mvnw.cmd test` — passou. Os testes que iniciam o contexto local usam o perfil `test` com H2 em memoria e sem Flyway.
- `npm.cmd run build` — passou (Vite 8.3.5).

Os testes executados localmente nao equivalem a uma bateria ponta a ponta no PostgreSQL. Os testes existentes de fluxo usam mocks e/ou H2; nao foram criados dados de teste no Supabase.

## Perfil de integracao Supabase

O perfil `supabase-test` usa `SUPABASE_TEST_JDBC_URL`, `SUPABASE_TEST_DB_USERNAME` e `SUPABASE_TEST_DB_PASSWORD` (com fallback para `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`). Ele define `spring.flyway.enabled=false`, `spring.jpa.hibernate.ddl-auto=none`, `spring.sql.init.mode=never` e `spring.flyway.clean-disabled=true`. A auditoria implementada consulta o historico Flyway e os nomes das triggers requeridas, e executa `Flyway.validate()` sem `migrate`, `clean`, DDL ou escrita de fixtures.

Para rodar essa verificacao, configure as tres variaveis `SUPABASE_TEST_*` no ambiente e execute:

```powershell
cd odontologia-api/backend
./mvnw.cmd "-Dtest=SupabaseSchemaAuditIntegrationTest" test
```

Essa verificacao foi ignorada nesta rodada porque nenhuma variavel de conexao estava configurada. Portanto, migrations V1 a V14, triggers, constraints e dados atuais do Supabase continuam sem validacao nesta execucao.

## Falhas de seguranca e correcoes aplicadas

- **Dados pessoais publicos:** `/api/pacientes/**` estava com `permitAll()` e o controller retorna entidades com dados cadastrais. Agora exige autoridade `SECRETARIA`. Foi removido tambem o `@CrossOrigin(origins = "*")` desse controller.
- **Senhas sem hash:** o login comparava a senha recebida diretamente com `senha_hash`. Agora aceita BCrypt; para compatibilidade com registros legados em texto puro, compara em tempo constante e converte o registro para BCrypt no primeiro login valido, dentro de transacao. Foram adicionados testes para hash, conversao e rejeicao de senha/conta invalida.
- **Exposicao de excecoes:** o login capturava qualquer `RuntimeException`, imprimia stack trace e respondia como erro de credencial. Agora somente credenciais invalidas viram 401; erros internos nao sao mascarados nem impressos diretamente pelo controller.
- **Respostas de autorizacao:** endpoints protegidos agora respondem 401 sem sessao e 403 para perfil sem permissao, com corpo JSON generico. Testes MockMvc verificam anonimo e perfil ALUNO nas rotas de pacientes, vagas e faltas.
- **Configuracao e arquivos locais:** `application.properties` usa `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`; `.gitignore` cobre `.env` e configuracoes locais de teste.
- **SQL da grade:** a consulta atual de `listarGradeSemanal` inclui filtros de disciplina e situacao apenas quando fornecidos. O teste `gradeSemFiltrosNaoIncluiParametrosNulosSemTipo` protege contra o SQL antigo com `? IS NULL`, que causava erro de inferencia de tipo no PostgreSQL. A consulta nao foi executada contra o Supabase nesta rodada.

## Pendencias e riscos

1. **Credenciais no historico Git:** a versao anterior de `application.properties` continha valores de conexao. Eles foram removidos da configuracao atual, mas permanecem em historico Git ja criado. Rotacione a senha do banco e quaisquer tokens afetados; se o repositorio ja foi compartilhado, trate os valores antigos como comprometidos. Nao reescrevi o historico para preservar o trabalho existente.
2. **CSRF:** a API usa sessao/cookie e ainda mantem CSRF desativado. Para producao, habilite protecao CSRF com fluxo de token integrado ao frontend e valide operacoes mutaveis entre origens.
3. **JWT e perfil ADMINISTRADOR:** a autenticacao implementada hoje usa sessao HTTP, nao JWT. O enum e a constraint de banco aceitam `ALUNO`, `COORDENADOR` e `SECRETARIA`; nao existe `ADMINISTRADOR`. Os testes nao podem afirmar suporte a esses contratos sem uma decisao de produto e alteracao de schema.
4. **Cobertura ponta a ponta pendente:** nao foram exercitados no Supabase os fluxos completos de agendamento, cancelamento, remarcacao, aprovacao/recusa, retorno, isolamento IDOR, validacao de dente, LGPD nem bloqueios das triggers de prontuario/adendo. Eles requerem credenciais de teste e fixtures relacionais controladas por transacao ou limpeza garantida.
5. **Senha legada:** a migracao para BCrypt ocorre gradualmente no login. Contas que nao autenticarem permanecem com o formato antigo ate logarem; planeje migracao/reset controlado antes da producao.

## Avaliacao

Os testes automatizados locais passaram e as duas falhas concretas de exposicao de dados e armazenamento/comparacao de senha foram corrigidas. Ainda nao e possivel classificar a API como auditada para producao: falta executar a integracao read-only no Supabase e cobrir os fluxos persistentes, IDOR, CSRF e imutabilidade no banco real.
