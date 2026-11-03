# RF010 — Validação funcional e ambiente isolado

Atualizado em 02/11/2026. Continuação da implementação existente. **A RF010 não está totalmente concluída.**

## Implementado para demonstração isolada

- Recusa de PENDENTE_APROVACAO: modal, justificativa obrigatória, trim, limite de 5.000 caracteres, cancelamento e envio único. Usa PATCH /recusar com observacaoSecretaria; preserva a observação original do aluno.
- Aprovação com confirmação de solicitação e paciente, exigindo demanda compatível selecionada. Uma única chamada PATCH /aprovar envia filaEsperaId; o endpoint de alocação separado não é chamado.
- Conflito de disponibilidade: sem falso sucesso, seleção invalidada e consultas atualizadas.
- Atualização de solicitação, tabela, indicadores e vínculo a partir das respostas reais. Respostas que não confirmam id/status/vínculo não são tratadas como sucesso.
- Consulta de solicitações decididas, com resposta e data de análise e sem novas ações de decisão.
- Resposta administrativa enriquecida com observação, resposta, autoria, data da análise e fila vinculada já existentes no banco. Nenhum endpoint, coluna ou tabela novo.
- Permissão SECRETARIA no backend e controle visual correspondente no frontend.
- Navegação, busca, filtros, sidebar e topbar preservados. Falha de consulta não vira lista vazia válida nem contagens zero.
- Erros apresentados com mensagens controladas, sem exibir detalhes sensíveis retornados pelo backend.

A seleção não reserva paciente. Aprovação e alocação não criam consulta, agendamento, dia/turno ou mensagens automáticas.

## Resultados e limites

| Verificação | Resultado |
| --- | --- |
| T08 antes de T09 | 12 testes frontend e 14 testes Java passaram. |
| T09 antes da rodada final | 18 testes frontend e 22 testes Java passaram. |
| Suíte final frontend | 31 testes passaram com React, DOM e API simulada. |
| Suíte final Java de solicitações/fila | 32 testes passaram; repositórios Mockito e contexto mínimo de autorização de métodos. |
| Build frontend | npm.cmd run build passou. |
| ESLint da RF010 | Arquivos alterados sem erros nem avisos após os ajustes finais. |
| ESLint geral | Quatro erros preexistentes em src/Aluno/AplicacaoAluno.jsx; módulo de outro integrante preservado. |
| Servidor isolado por HTTP | Banner, login de teste, listagem, aprovação, recusa e rota desconhecida verificados; dados reiniciados após teste. |
| Revisão visual automática | Não executada: browser access host unavailable. T07 e T14 aguardam revisão no navegador. |

O JAVA_HOME original impediu a primeira execução Java; foi usado o JDK 25 já instalado, somente no processo de teste. Os avisos Maven de metadados 401 em repositórios opcionais e de agente Mockito não impediram BUILD SUCCESS.

**Não executados:** OdontologiaApiApplicationTests, integração transacional/concorrência real PostgreSQL e matriz HTTP completa dos filtros de sessão. O teste de contexto completo poderia conectar ao Supabase; não há banco PostgreSQL descartável configurado nesta entrega. Os mocks e o teste do mapeamento de conflito para 409 não comprovam rollback ou concorrência real em banco. T10 permanece parcial por esse motivo.

A instalação das dependências de testes informou sete alertas no npm audit (seis moderados e um alto). Não foi executado npm audit fix, que poderia alterar dependências fora do escopo.

## Reproduzir testes e build

Na pasta frontend:

```powershell
npm.cmd test
.\node_modules\.bin\eslint.cmd src/AnalisarSolicitacao.jsx src/ValidacaoSolicitacoes.jsx src/FilaEspera.jsx src/api.js src/services/solicitacaoService.js
npm.cmd run build
```

Na raiz odontologia-api, executar somente testes isolados:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25'
.\mvnw.cmd '-Dtest=SolicitacaoPaciente*Test,FilaEspera*Test' test
```

Esses comandos não executam o teste de contexto completo nem iniciam Spring Boot.

## Roteiro no navegador — sem Supabase

1. Não inicie o backend. Na pasta frontend, execute `npm.cmd run dev:rf010-test`. Se a porta 5174 já estiver ativa nesta sessão, use a URL existente.
2. Abra **http://127.0.0.1:5174** e confirme o banner **AMBIENTE ISOLADO: dados em memória, sem Supabase**.
3. Use somente as credenciais fictícias **secretaria@teste.local / teste**.
4. Entre em Solicitações e abra Analisar em REQ-1. Confira os dados de teste.
5. Selecione D-101 e depois D-102: apenas uma opção deve ficar marcada, com destaque e conferência atualizados. Atualizar fila limpa a seleção.
6. Abra Recusar solicitação sem escolher paciente. Tente motivo vazio e espaços: não deve haver gravação. Preencha um motivo e confirme; confira RECUSADO, justificativa, sucesso e ausência de novas ações. Volte à listagem e confira indicadores/filtros.
7. Use Reiniciar dados de teste e entre novamente. Selecione uma demanda, abra Aprovar e Vincular e confira o paciente antes de confirmar. Verifique APROVADO_E_ALOCADO, paciente vinculado e ausência de nova aprovação.
8. Reinicie novamente. Selecione D-101 e use o botão de simulação no banner para alocá-la por outro usuário. Confirme a aprovação: deve haver conflito sem sucesso, seleção limpa e demandas recarregadas.
9. Abra Visualizar nas solicitações recusadas, aprovadas e PENDENTE_FILA. Não devem existir ações de decisão indevidas.
10. Encerre o servidor com Ctrl+C ao terminar.

O servidor isolado usa configFile: false, envDir: false, URL de API vazia definida somente nesse processo, proxy vazio e middleware local que intercepta todas as rotas /api. Rotas desconhecidas retornam 404. A política connect-src 'self' bloqueia chamadas a outros endereços. As fixtures e simulações ficam em frontend/tests, não são importadas por módulos de produção e não integram o bundle final.

**Não use a instância normal conectada ao Supabase compartilhado para estes testes de decisão.** A implementação normal usa os endpoints reais; o isolamento descrito pertence ao comando de testes.

## Pendências de decisão da equipe

| Item | Comportamento mantido |
| --- | --- |
| DP-02 / PENDENTE_FILA | Visível, sem nova semântica, agrupamento presumido ou transição. Ações de decisão desse estado e Marcar Pendente de Fila continuam indisponíveis no fluxo administrativo. |
| DP-03 / reversão | Não implementada até definir destino, elegibilidade e efeitos na alocação. |
| DP-05 / histórico | Sem migrações, tabelas, endpoints, eventos fictícios ou backfill. A última decisão exibida não é apresentada como histórico completo. |
| Saldo acadêmico | Sem consulta de origem confirmada; não calculado nem inventado. |
| Retornos sugeridos | Sem fonte/escopo confirmado; não implementado. |
| T10 / integração | Requer ambiente PostgreSQL descartável para testar rollback e concorrência real. |
| T14 / revisão final | Requer revisão visual das alterações e resolução dos requisitos obrigatórios bloqueados. |

Nenhum comando Git, migração, alteração de conexão ou operação na API/banco compartilhado foi executado.

## Arquivos modificados ou criados

- `docs/rf010-validacao-solicitacoes/tasks.md`
- `docs/rf010-validacao-solicitacoes/validacao-isolada.md`
- `frontend/package-lock.json`
- `frontend/package.json`
- `frontend/src/AnalisarSolicitacao.css`
- `frontend/src/AnalisarSolicitacao.jsx`
- `frontend/src/FilaEspera.jsx`
- `frontend/src/ValidacaoSolicitacoes.jsx`
- `frontend/src/api.js`
- `frontend/src/services/solicitacaoService.js`
- `frontend/tests/rf010-fixtures.mjs`
- `frontend/tests/rf010.test.mjs`
- `frontend/tests/servidor-isolado.mjs`
- `src/main/java/br/com/unicatolica/odontologia_api/solicitacoes/controller/SolicitacaoConflitoHandler.java`
- `src/main/java/br/com/unicatolica/odontologia_api/solicitacoes/dto/SolicitacaoAdministrativaDTO.java`
- `src/main/java/br/com/unicatolica/odontologia_api/solicitacoes/dto/SolicitacaoAdministrativaProjection.java`
- `src/main/java/br/com/unicatolica/odontologia_api/solicitacoes/repository/SolicitacaoPacienteRepository.java`
- `src/main/java/br/com/unicatolica/odontologia_api/solicitacoes/service/SolicitacaoPacienteService.java`
- `src/test/java/br/com/unicatolica/odontologia_api/solicitacoes/controller/SolicitacaoPacienteControllerTest.java`
- `src/test/java/br/com/unicatolica/odontologia_api/solicitacoes/controller/SolicitacaoPacientePermissoesTest.java`
- `src/test/java/br/com/unicatolica/odontologia_api/solicitacoes/service/SolicitacaoPacienteServiceTest.java`

Saídas geradas: frontend/dist, target e cache de dependências. Configurações de conexão, migrações, App.jsx e módulos de outros integrantes permaneceram intactos.
