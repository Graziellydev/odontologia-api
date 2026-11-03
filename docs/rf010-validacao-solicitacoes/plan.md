# RF010 — Plano técnico de implementação

## 1. Objetivo e limites

Este plano descreve a implementação futura da RF010 — Validação de Solicitações dos Alunos. Nesta etapa, nenhum código é implementado. O objetivo é transformar a especificação em uma sequência técnica verificável, preservando o fluxo confirmado de aprovação com alocação.

Decisões já confirmadas:

- **DP-01:** a aprovação exige a seleção de uma entrada compatível da fila; a solicitação termina em `APROVADO_E_ALOCADO`, a entrada termina em `ALOCADO` e o paciente fica associado na mesma operação. Isso não cria agendamento.
- **DP-04:** a listagem mostra o semestre atual do aluno, usando `usuarios.semestre_atual`, e mostra a turma separadamente.

Decisões ainda pendentes e que não podem ser tratadas como regras aprovadas:

- **DP-02:** semântica, agrupamento e ações para `PENDENTE_FILA`.
- **DP-03:** destino, elegibilidade e efeitos da reversão, especialmente de uma aprovação já alocada.
- **DP-05:** formato definitivo do histórico de decisões e eventual integração com `logs_auditoria`.

Enquanto essas decisões não forem validadas, a implementação correspondente deve permanecer bloqueada. A aprovação, recusa e consulta básica podem avançar somente nos limites já confirmados pela SPEC.

## 2. Arquitetura proposta

### 2.1. Frontend

Criar dois componentes React para as duas telas oficiais:

- `frontend/src/ValidacaoSolicitacoes.jsx`: tela principal com indicadores, busca, filtros por status/disciplina, tabela de solicitações anteriores e atuais, paginação/consulta e ação “Analisar”. Os indicadores devem vir de dados reais do backend; as quantidades do Figma são ilustrativas.
- `frontend/src/AnalisarSolicitacao.jsx`: tela de análise individual, recebendo o identificador da solicitação selecionada, exibindo necessidade acadêmica, detalhes do aluno, entradas compatíveis da fila, prioridade, tempo de espera, seleção, conferência e “Aprovar e Vincular”. Deve também reservar espaço para recusa e histórico sem inventar contratos ainda não existentes.

Criar CSS complementar escopado para as duas telas, preferencialmente `frontend/src/ValidacaoSolicitacoes.css` e `frontend/src/AnalisarSolicitacao.css`. Esses arquivos devem conter apenas regras específicas de indicadores, filtros, tabela, badges, cards de análise, seleção da fila, conferência, modais e mensagens. Não devem recriar sidebar, topbar ou o layout geral.

Não duplicar sidebar, topbar, menu móvel ou rodapé. `FilaEspera.jsx` continuará sendo o shell da secretaria e renderizará `ValidacaoSolicitacoes` na área de conteúdo quando `telaAtiva` for “solicitacoes”. A tela filha deve renderizar somente cabeçalho, filtros, tabela, detalhes e modais. O item existente “Solicitações” deve deixar de exibir o aviso provisório e passar a alterar a tela ativa.

Reutilizar as classes e padrões de `FilaEspera.css`: `.aplicacao`, `.area-principal`, `.topbar`, `.conteudo`, `.cabecalho-pagina`, `.filtros`, `.tabela-container`, `.tabela-scroll`, `.botao-primario`, `.botao-secundario`, `.modal`, `.modal-fundo`, `.notificacao` e os breakpoints de 1100px, 850px e 580px. `FilaEspera.jsx` continuará sendo o shell, controlando sidebar, topbar, menu móvel, navegação para Pacientes/Fila de Espera e a tela ativa. O CSS complementar não deve recriar esses elementos nem assumir o formato de erro Axios da Fila; as chamadas novas usarão os erros com `status` fornecidos por `api.js`.

O item “Solicitações” já existente deve trocar o conteúdo provisório por `ValidacaoSolicitacoes`. A ação “Analisar” deve trocar o conteúdo para `AnalisarSolicitacao`, preservando uma forma explícita de voltar à listagem. Não abrir uma nova página com shell próprio.

Na tela de análise, a lista de demandas compatíveis deve reutilizar o contrato real de `GET /api/fila-espera`, enviando `procedimentoId`, `dente` e `classe` da solicitação e aceitando somente entradas `AGUARDANDO`. O retorno atual já contém nome do paciente, prioridade, data de entrada e dias de espera. A seleção deve ser local até a confirmação; a alocação efetiva continuará sendo feita pelo endpoint de aprovação transacional.

O botão “Marcar Pendente de Fila” do Figma deve ser tratado como estado visual bloqueado por DP-02. Não criar chamada ou transição provisória. O campo “Observação da análise” também não deve ser enviado até existir contrato explícito, pois o DTO atual de aprovação só recebe `filaEsperaId`.

### 2.1.1. Mapeamento do Figma para a RF010

Referências visuais oficiais:

- [Solicitações dos alunos](./figma/solicitacoes-alunos.png.png) — referência da implementação de `ValidacaoSolicitacoes.jsx`.
- [Analisar e vincular paciente](./figma/analisar-vincular.png.png) — referência reservada para `AnalisarSolicitacao.jsx` nas tarefas posteriores.

- Tela principal: indicadores, busca, filtros, tabela, status e “Analisar” pertencem à consulta administrativa da RF010.
- Tela de análise: necessidade acadêmica, demanda compatível, prioridade, espera, conferência e “Aprovar e Vincular” pertencem ao fluxo de aprovação com alocação confirmado por DP-01.
- “Saldo acadêmico”/“necessidade acadêmica” precisa de uma consulta de origem nos modelos acadêmicos; não está comprovado como campo da solicitação. Até essa origem ser definida, o componente deve tratar o dado como opcional e não fabricar texto ou número.
- Recusa com justificativa, histórico e reversão continuam obrigatórios mesmo quando não aparecem nos prints; devem ser previstos em detalhes/modais, condicionados aos contratos de DP-03 e DP-05.
- “Retornos sugeridos” não deve ser implementado automaticamente. O projeto possui dados de retornos/consultas, mas a relação desse indicador com a RF010 precisa de validação de escopo e fonte.
- Textos, nomes, datas, contagens e prioridades das imagens não são fixtures nem dados iniciais do sistema.

### 2.2. Backend

Manter a base `/api/solicitacoes-pacientes` e evoluir o módulo existente. A listagem administrativa deve retornar DTOs próprios, em vez de expor diretamente `SolicitacaoPaciente`, para incluir nomes e dados relacionados sem acoplar a API à entidade de persistência.

Fluxo confirmado de aprovação:

1. Validar `SECRETARIA`, versão da solicitação e elegibilidade do estado.
2. Validar `filaEsperaId`, existência da entrada, estado `AGUARDANDO`, procedimento, dente e classe.
3. Dentro da mesma transação, marcar a entrada como `ALOCADO`, preencher a saída, associar `filaEsperaId` e `pacienteAlocadoId`, mudar a solicitação para `APROVADO_E_ALOCADO` e registrar autor/data.
4. Persistir o evento de decisão somente junto com as alterações de solicitação e fila.
5. Não criar vaga, consulta, agenda, mensagem ou formulário de dia/turno.

Fluxo de recusa:

1. Validar `SECRETARIA`, versão e estado elegível.
2. Exigir justificativa não nula, não vazia após `trim` e dentro do limite definido pela API.
3. Preservar `observacaoSecretaria` do aluno e gravar a justificativa em `respostaSecretaria`.
4. Mudar para `RECUSADO` e registrar o evento na mesma transação.

## 3. Contratos, consultas e endpoints

Os nomes abaixo são propostas de implementação e devem ser ajustados ao padrão final do projeto durante a execução.

### 3.1. DTOs e consultas

Criar DTOs de leitura separados, por exemplo:

- `SolicitacaoAdministrativaResumoDTO`: id, estado, aluno, turma, semestre atual do aluno, procedimento, dente, classe, data desejada e paciente associado.
- `SolicitacaoAdministrativaDetalheDTO`: resumo, observação do aluno, resposta da secretaria, responsável, data da solicitação e data da análise.
- `HistoricoSolicitacaoDTO`: ação, estado anterior/novo, autor, data/hora, justificativa, fila/paciente anteriores e resultantes e decisão revertida, quando aplicável.
- `AprovarSolicitacaoDTO` e `RecusarSolicitacaoDTO`: preservar os contratos atuais, acrescentando somente campos necessários e validados.
- `ReverterSolicitacaoDTO`: justificativa obrigatória; campos de destino ou confirmação de alocação somente depois da resolução de DP-03.

Adicionar consultas de projeção no repositório de solicitações para juntar `usuarios`, `turmas`, `disciplinas`, `procedimentos` e `pacientes`. O campo de semestre deve vir de `usuarios.semestre_atual`; `turma_id` deve continuar permitindo apresentar a turma separadamente. A consulta deve preservar solicitações com relações opcionais ausentes, sem fabricar nomes.

### 3.2. Endpoints existentes a manter ou evoluir

- `GET /api/solicitacoes-pacientes/minhas`: manter escopo exclusivo do aluno autenticado e retornar o estado/decisão vigente.
- `GET /api/solicitacoes-pacientes/pendentes`: evoluir para retornar a visão administrativa pendente, sem esconder `PENDENTE_FILA`. O agrupamento e as ações de `PENDENTE_FILA` ficam bloqueados por DP-02.
- `GET /api/fila-espera`: reutilizar para carregar demandas compatíveis na Tela 2, sempre com filtros da solicitação. Não usar a listagem geral para permitir seleção de entrada incompatível.
- `PATCH /api/solicitacoes-pacientes/{id}/aprovar`: manter seleção obrigatória de `filaEsperaId` e aprovação com alocação transacional.
- `PATCH /api/solicitacoes-pacientes/{id}/recusar`: manter justificativa obrigatória e separação entre observação do aluno e resposta da secretaria.

### 3.3. Endpoints dependentes de decisões

Propor, mas não implementar antes das confirmações:

- `GET /api/solicitacoes-pacientes/{id}` para detalhes administrativos.
- `GET /api/solicitacoes-pacientes/{id}/historico` para eventos cronológicos.
- `PATCH /api/solicitacoes-pacientes/{id}/reverter` para reversão com justificativa.

Os endpoints administrativos e de histórico devem exigir `SECRETARIA`. O endpoint `/minhas` deve derivar o aluno da autenticação, nunca de um identificador enviado pelo cliente.

## 4. Histórico e reversões

### 4.1. Histórico — DP-05 pendente

A opção preferencial é uma tabela específica de histórico de solicitações, com chave da solicitação, ação, estados anterior/novo, autor, data/hora, justificativa, vínculos de fila/paciente anteriores e resultantes, versão e referência à decisão revertida. Ela permite consultas cronológicas, integridade referencial e validação de transições.

`logs_auditoria` pode receber um evento complementar para rastreabilidade geral, mas não deve substituir o histórico de domínio enquanto não houver decisão da equipe sobre estrutura, formato de detalhes, retenção e consultas. O histórico novo deve ser escrito na mesma transação da decisão. Não haverá backfill nem inferência de aprovações, recusas ou reversões anteriores à implantação.

### 4.2. Reversão — DP-03 pendente

O desenho técnico deverá separar o comportamento recomendado da autorização final:

- reversão de recusa: candidata a retornar para `PENDENTE_APROVACAO`, mantendo a recusa original como evento histórico;
- reversão de aprovação: candidata a ser permitida somente se não houver consulta, atendimento ou outro uso que torne inseguro desfazer a alocação;
- toda reversão: justificativa, autor, data/hora, versão concorrente e novo evento, sem apagar eventos anteriores.

Antes de implementar, a equipe precisa definir se a entrada `ALOCADO` volta a `AGUARDANDO`, quais estados de `consultas` bloqueiam a operação, como tratar consultas canceladas, retornos, transferências e paciente reutilizado, e se existe limite de tempo ou restrição de autoria. O bloqueio deve considerar os vínculos de paciente e `fila_espera_id` existentes em `consultas`; não é suficiente trocar o status da solicitação.

## 5. Tratamento de `PENDENTE_FILA` — DP-02 pendente

O backend atual aceita `PENDENTE_FILA` como estado elegível, mas o código analisado não o produz e a consulta administrativa atual busca apenas `PENDENTE_APROVACAO`. A tela do aluno possui um rótulo legado “Aprovada — aguardando paciente”.

O plano recomenda que a secretaria veja esse estado em grupo separado, com rótulo explícito como “Pendente na fila” ou “Pendente de alocação”, sem convertê-lo automaticamente em aprovado, recusado ou pendente comum. O valor persistido deve continuar visível para diagnóstico. A habilitação de aprovar/recusar, a ordem do grupo e a eventual transição precisam ser confirmadas antes de fechar o contrato da tela.

## 6. Arquivos previstos

### Criar

- `frontend/src/ValidacaoSolicitacoes.jsx`.
- `frontend/src/ValidacaoSolicitacoes.css`.
- `frontend/src/AnalisarSolicitacao.jsx`.
- `frontend/src/AnalisarSolicitacao.css`.
- DTOs administrativos, de detalhes, histórico e reversão no pacote `solicitacoes/dto`, conforme contratos aprovados.
- Repositório/entidade/serviço de histórico específico, caso DP-05 confirme a tabela de domínio.

### Modificar

- `frontend/src/FilaEspera.jsx`: integrar a tela filha ao `telaAtiva` e ao item de menu existente.
- `src/main/java/.../solicitacoes/controller/SolicitacaoPacienteController.java`: endpoints de listagem detalhada, histórico e reversão, além da evolução dos contratos existentes.
- `src/main/java/.../solicitacoes/service/SolicitacaoPacienteService.java`: regras de aprovação, recusa, histórico e, após DP-03, reversão.
- `src/main/java/.../solicitacoes/repository/SolicitacaoPacienteRepository.java`: projeções e filtros administrativos.
- `src/main/java/.../solicitacoes/entity/SolicitacaoPaciente.java`: somente se a estratégia de persistência exigir versionamento ou relacionamentos adicionais.
- `src/main/resources/db/migration/`: nova migração para o histórico, somente após confirmação de DP-05; não alterar migrações já aplicadas.

### Reutilizar sem alteração prevista

- `frontend/src/api.js`: usar o contrato existente de `/api`, sessão, JSON e erros com `status`.
- `frontend/src/FilaEspera.css`: reutilizar os estilos compartilhados; o CSS novo conterá apenas complementos escopados.

## 7. Testes e critérios técnicos

### Backend

- permissões: ALUNO, COORDENADOR, SECRETARIA e usuário não autenticado para cada endpoint;
- propriedade: aluno não acessa solicitação de outro aluno;
- listagem: nomes, turma, `usuarios.semestre_atual`, procedimento, paciente opcional e estados corretos;
- `PENDENTE_FILA`: visibilidade sem alteração indevida de estado ou permissão;
- aprovação: entrada `AGUARDANDO`, compatibilidade, resultado `APROVADO_E_ALOCADO`, fila `ALOCADO`, paciente associado e ausência de agendamento;
- recusa: justificativa nula, vazia, espaços, limite e preservação da observação do aluno;
- atomicidade: falha em qualquer validação não altera solicitação, fila, paciente ou histórico;
- concorrência: duas aprovações para a mesma versão não alocam duas vezes; usar `@Version`/controle equivalente e verificar conflito;
- histórico: evento único por decisão confirmada, estados e vínculos preservados, sem backfill legado;
- reversão: somente depois da decisão DP-03, cobrindo impedimento por consulta/atendimento e consistência fila-paciente-solicitação.

### Frontend

- navegação entre fila, pacientes e solicitações sem duplicar shell;
- carregamento, lista vazia, erro de rede, `401/403`, `409` e validação sem falso sucesso;
- filtros por grupo e exibição separada de `PENDENTE_FILA`;
- seleção apenas de entradas compatíveis para aprovação;
- recusa e reversão com justificativa obrigatória, confirmação e cancelamento sem alteração;
- atualização após decisão, evitando duplo envio;
- tabela/modal responsivos nos breakpoints da Fila de Espera;
- aluno visualizando apenas o resultado vigente e a justificativa de recusa.

## 8. Ordem de execução e gates

1. Confirmar DP-02, DP-03 e DP-05 com a equipe; sem isso, não fechar agrupamento de `PENDENTE_FILA`, contrato de reversão ou migração/histórico.
2. Definir os DTOs e projeções de leitura, preservando DP-01 e DP-04.
3. Implementar listagem administrativa, detalhes e integração da tela ao shell existente.
4. Evoluir aprovação e recusa com testes de autorização, validação, transação e concorrência.
5. Implementar histórico específico e seus endpoints após DP-05.
6. Implementar reversões após DP-03, incluindo verificações de consultas, atendimentos, fila e paciente.
7. Executar testes focados e revisar os critérios de aceitação da SPEC antes de qualquer migração ou implantação.

Não fazem parte deste plano WhatsApp, mensagens automáticas, agendamento, formulário de dia/turno, alteração de configurações ou mudanças no banco compartilhado fora de uma migração futura formalmente aprovada.
