# RF010 — Validação de Solicitações dos Alunos

- **Sprint:** 2.
- **Etapa:** especificação com Spec-Driven Development.
- **Data:** 02/11/2026.
- **Situação:** proposta para revisão da equipe; DP-01 e DP-04 resolvidas e demais decisões pendentes identificadas na seção 10.
- **Entrega desta etapa:** somente este documento. Os comportamentos futuros descritos abaixo não representam funcionalidades já implementadas.

## 1. Objetivo

Permitir que a secretaria consulte, aprove, recuse e revise solicitações de pacientes feitas pelos alunos, com justificativa obrigatória para recusa e rastreabilidade das decisões. O aluno deve acompanhar o resultado das próprias solicitações e compreender o motivo de uma recusa.

A interface da secretaria deve integrar o fluxo existente e reutilizar a estrutura visual da Fila de Espera. A aprovação exige a seleção de uma entrada compatível da fila de espera e a aprovação e a alocação do paciente ocorrem na mesma operação, conforme o comportamento atual do backend.

## 2. Usuários e permissões

| Usuário | Acesso previsto no RF010 |
| --- | --- |
| Secretaria autenticada, com autoridade SECRETARIA | Consultar solicitações de alunos nos estados previstos, analisar os dados, aprovar, recusar, consultar o histórico e reverter decisões conforme as regras validadas. |
| Aluno autenticado, com autoridade ALUNO | Consultar apenas as próprias solicitações, visualizar o estado atual, a decisão e a justificativa da recusa. |
| Coordenador e outros perfis | Não recebem permissão para aprovar, recusar ou reverter neste requisito. |
| Usuário não autenticado | Não pode acessar os dados nem executar as operações do RF010. |

A autorização deve ser verificada no backend em cada operação. Ocultar botões no frontend não substitui essa verificação. A identidade do aluno e da secretaria responsável deve vir da autenticação, sem aceitar um identificador enviado pelo cliente como prova de identidade.

## 3. Contexto observado no projeto

### 3.1. Módulo Spring Boot de solicitações

O módulo existente está em [solicitacoes](../../src/main/java/br/com/unicatolica/odontologia_api/solicitacoes). O controller utiliza a base `/api/solicitacoes-pacientes` e expõe:

| Operação existente | Acesso atual | Comportamento observado |
| --- | --- | --- |
| POST na base | ALUNO | Cria solicitação do aluno autenticado em PENDENTE_APROVACAO. |
| GET /minhas | ALUNO | Retorna as solicitações do aluno autenticado, da mais recente para a mais antiga. |
| GET /pendentes | SECRETARIA | Retorna somente PENDENTE_APROVACAO, da mais antiga para a mais recente. |
| PATCH /{solicitacaoId}/aprovar | SECRETARIA | Exige filaEsperaId e aprova com alocação do paciente. |
| PATCH /{solicitacaoId}/recusar | SECRETARIA | Exige justificativa e registra a resposta da secretaria. |

O service considera PENDENTE_APROVACAO e PENDENTE_FILA elegíveis para as operações atuais de aprovação e recusa. Não foram encontradas no módulo operações de listagem de aprovadas/recusadas para a secretaria, consulta de histórico de decisões ou reversão.

As respostas atuais usam a entidade SolicitacaoPaciente, que contém identificadores de aluno, turma, procedimento e paciente, sem os respectivos nomes nem o semestre. A listagem prevista no RF010 depende de enriquecer esses dados em etapa futura; os catálogos atuais do aluno não constituem uma listagem geral para a secretaria.

A entidade mantém a última análise, a resposta e uma versão para controle de concorrência. Esses campos isolados não preservam a sequência de decisões.

### 3.2. Persistência: V4 e V12

A [V4](../../src/main/resources/db/migration/V4__create_fila_and_solicitacoes.sql) cria a fila e as solicitações, com estes estados:

| Estado persistido | Significado observado |
| --- | --- |
| PENDENTE_APROVACAO | Solicitação aguardando análise. |
| PENDENTE_FILA | Estado existente no banco; o backend o trata como pendente para decidir, mas a tela do aluno o apresenta como “Aprovada — aguardando paciente”. Classificação a validar em DP-02. |
| APROVADO_E_ALOCADO | Solicitação aprovada com paciente associado. |
| RECUSADO | Solicitação recusada. |

A V4 exige paciente associado quando o estado é APROVADO_E_ALOCADO. Na aprovação atual, a entrada da fila deve estar AGUARDANDO, ter o mesmo procedimento e dente e, se a solicitação informar classe, a mesma classe. A operação marca a entrada como ALOCADO, preenche sua saída e grava os vínculos de fila e paciente, a secretaria responsável e a data da análise na solicitação, em uma transação.

A [V12](../../src/main/resources/db/migration/V12__separar_observacao_secretaria.sql) separa:

- `observacao_secretaria` / `observacaoSecretaria`: observação enviada pelo aluno à secretaria.
- `resposta_secretaria` / `respostaSecretaria`: resposta escrita pela secretaria, incluindo o motivo da recusa.

A V12 copia observações de registros já analisados para a nova resposta quando aplicável e passa a exigir resposta não nula e não vazia para RECUSADO. Isso não reconstrói todas as decisões anteriores.

Por compatibilidade, o DTO de recusa ainda recebe o motivo no campo `observacaoSecretaria`, com limite de 5.000 caracteres; o service grava esse conteúdo, sem espaços nas extremidades, em `respostaSecretaria`. Essa diferença entre o nome do campo de entrada e o significado persistido não deve levar à sobrescrita da observação do aluno.

Existe uma tabela geral logs_auditoria na V2, mas não foi encontrado registro de decisões de solicitações nela pelo módulo analisado. O histórico de consultas presente nas V5/V8 pertence a outro fluxo e não comprova histórico de solicitações.

### 3.3. Frontend e integração

- [App.jsx](../../frontend/src/App.jsx) direciona SECRETARIA para FilaEspera e ALUNO para AplicacaoAluno.
- [FilaEspera.jsx](../../frontend/src/FilaEspera.jsx) contém o menu lateral, a barra superior e a navegação interna entre fila e pacientes. O item “Solicitações” já existe, mas atualmente apenas informa que a tela será desenvolvida.
- [FilaEspera.css](../../frontend/src/FilaEspera.css) define o layout, a paleta, os espaçamentos, a tabela, os botões, os modais e a adaptação para telas menores.
- [api.js](../../frontend/src/api.js) centraliza chamadas fetch, prefixo /api, base configurável já existente, credenciais de sessão e erros com propriedade `status`. As chamadas futuras do RF010 devem reutilizar esse contrato.
- A Fila de Espera usa hoje um serviço próprio com Axios e erros em `response.status`. Seus estilos podem ser reutilizados, mas esse formato de erro não deve ser presumido nas chamadas feitas por api.js.
- [AplicacaoAluno.jsx](../../frontend/src/Aluno/AplicacaoAluno.jsx) já consulta /minhas e apresenta o estado e a resposta da secretaria nos detalhes. Esse comportamento deve ser preservado e atualizado para refletir decisões posteriores.

A análise também considerou SecurityConfig, os modelos acadêmicos das V2/V3/V11, o catálogo de solicitações e os testes existentes do módulo, apenas por leitura.

## 4. Escopo

### 4.1. Incluído no comportamento a especificar

- Consulta de solicitações pendentes, aprovadas e recusadas pela secretaria.
- Exibição dos dados acadêmicos, clínicos e do paciente associado, quando houver.
- Aprovação conforme o fluxo atual e recusa com justificativa obrigatória.
- Acompanhamento da decisão e da justificativa pelo próprio aluno.
- Consulta do histórico de decisões e reversão pela secretaria, preservando registros anteriores.
- Autorização no backend e interface integrada ao padrão da Fila de Espera.
- Tratamento de ausência de dados, carregamento, erros e conflitos.

### 4.2. Fora do escopo

- Bot do WhatsApp.
- Envio automático de mensagens, por qualquer canal.
- Agendamento de consultas, reserva de horário ou criação de agenda.
- Formulário posterior de dia/turno.
- Redesenho da criação de solicitações do aluno, da fila ou de outros módulos.
- Separação entre aprovação e alocação; neste requisito, as duas operações permanecem conjuntas.

A data desejada já registrada é apenas informação para análise; não representa agendamento confirmado.

## 5. Requisitos funcionais

| Identificador | Requisito |
| --- | --- |
| RF010-01 | A secretaria deve acessar “Solicitações” pelo menu existente e consultar pendentes, aprovadas e recusadas, distinguindo os grupos e o estado de cada registro. PENDENTE_FILA não pode ser omitido; sua classificação definitiva depende de DP-02. |
| RF010-02 | Cada item da listagem deve exibir aluno, turma, semestre, procedimento, dente, classe, data desejada e paciente associado, quando houver, além da identificação e do estado da solicitação. |
| RF010-03 | A secretaria deve consultar os detalhes, incluindo a observação original do aluno, a resposta atual da secretaria, a data da solicitação e, quando existentes, o responsável e a data da análise. |
| RF010-04 | A secretaria deve aprovar solicitações elegíveis selecionando uma entrada compatível da fila de espera. A aprovação e a alocação do paciente devem ocorrer simultaneamente, resultando em APROVADO_E_ALOCADO. A aprovação não representa um agendamento; o dia e o turno serão informados pelo aluno posteriormente, em outra funcionalidade. |
| RF010-05 | A secretaria deve recusar solicitações elegíveis mediante justificativa obrigatória, validada no frontend e no backend. |
| RF010-06 | O aluno deve visualizar o estado e a decisão atuais das próprias solicitações e a justificativa quando recusadas. Uma nova consulta ou atualização deve refletir a última alteração confirmada pelo backend. |
| RF010-07 | A secretaria deve consultar, por solicitação, a sequência cronológica de decisões e reversões, identificando o que ocorreu, quando e por quem. |
| RF010-08 | A secretaria deve poder reverter decisões de aprovação e recusa elegíveis. A reversão deve gerar novo registro e preservar a decisão revertida; destino, elegibilidade e efeitos na alocação dependem de DP-03. |
| RF010-09 | Somente SECRETARIA pode aprovar, recusar ou reverter. A listagem administrativa e seu histórico também devem ser protegidos para esse perfil no RF010. |
| RF010-10 | A tela deve reutilizar o menu lateral, a barra superior, as cores, os espaçamentos e os estilos da Fila de Espera, incluindo o comportamento responsivo. |
| RF010-11 | A interface deve diferenciar carregamento, lista vazia, falha de consulta, validação rejeitada, acesso negado e conflito, permitindo nova consulta sem apresentar sucesso indevido. |
| RF010-12 | A tela principal deve apresentar indicadores de solicitações e filtros por busca, status e disciplina. Os indicadores devem ser calculados a partir de dados reais do backend; números exibidos no Figma são apenas ilustrativos. |
| RF010-13 | A tela principal deve listar requisições anteriores e atuais da secretaria, com aluno, procedimento, dente, classe, disciplina, semestre atual do aluno, turma e status, oferecendo a ação “Analisar” para solicitações que possam ser analisadas e uma ação de consulta para decisões já registradas. |
| RF010-14 | Ao selecionar “Analisar”, a secretaria deve navegar para a tela de análise da solicitação selecionada sem duplicar sidebar ou topbar, mantendo a possibilidade de retornar à listagem e continuar navegando para Pacientes e Fila de Espera. |
| RF010-15 | A tela de análise deve apresentar os dados da solicitação e do aluno, a necessidade acadêmica disponível, a lista de entradas compatíveis da fila com paciente, prioridade e tempo de espera, e permitir selecionar uma entrada antes da aprovação. A compatibilidade deve respeitar procedimento, dente e classe. |
| RF010-16 | A tela de análise pode apresentar um campo de observação da análise, mas sua persistência, autoria e limite dependem da confirmação do contrato do backend; não assumir que o DTO atual de aprovação já salva esse texto. |
| RF010-17 | Antes de aprovar, a tela deve exibir uma conferência da solicitação e da entrada selecionada. “Aprovar e Vincular” só pode concluir com uma entrada compatível selecionada e deve executar a operação confirmada em DP-01. |
| RF010-18 | A secretaria deve conseguir recusar uma solicitação elegível a partir do fluxo administrativo, mesmo que a ação não apareça nas imagens de referência, sempre com justificativa obrigatória e preservação da observação original do aluno. |
| RF010-19 | A interface deve oferecer consulta do histórico de decisões e ações de reversão quando os contratos forem definidos. A ausência dessas ações nos prints não remove os requisitos RF010-07 e RF010-08. |

### 5.1. Dados de apresentação

| Informação | Origem ou regra |
| --- | --- |
| Aluno | Nome de usuarios, relacionado por aluno_id; manter o identificador para vinculação. |
| Turma | Código da turma e identificação da disciplina, relacionados por turma_id. |
| Semestre atual do aluno | Valor de `usuarios.semestre_atual`, relacionado por `aluno_id`. A turma deve ser exibida separadamente; `turmas.semestre_letivo` não substitui o semestre atual do aluno. |
| Procedimento | Nome relacionado por procedimento_id. |
| Dente | Valor FDI registrado na solicitação. |
| Classe | Valor registrado; quando ausente, apresentar “Não informada”. |
| Data desejada | Valor registrado, em formato brasileiro; quando ausente, apresentar “Não informada”, sem deslocar o dia por conversão de fuso horário. |
| Paciente associado | Nome relacionado por paciente_alocado_id, quando houver; caso contrário, apresentar “Sem paciente associado”. |
| Decisão atual | Estado atual e dados da última análise, separados das decisões anteriores. |

A listagem deve utilizar dados reais das relações existentes. Não deve depender apenas de códigos numéricos para representar nomes. Ausência de um dado complementar não deve excluir silenciosamente a solicitação nem produzir um nome fictício.

### 5.2. Fluxos e telas de referência

As imagens oficiais do Figma representam duas telas da mesma RF010:

Arquivos oficiais usados como referência visual:

- [Solicitações dos alunos](./figma/solicitacoes-alunos.png.png) — listagem administrativa.
- [Analisar e vincular paciente](./figma/analisar-vincular.png.png) — análise individual.

1. **Solicitações dos alunos:** página principal da secretaria, com indicadores, busca, filtros por status e disciplina, tabela de requisições e ações de análise ou consulta. A tabela deve incluir as informações acadêmicas e clínicas exigidas em 5.1. “Retornos sugeridos” aparece como indicador visual no Figma, mas sua origem no módulo de retornos/consultas precisa ser confirmada; não incluir esse indicador no escopo da RF010 nem preencher números fictícios.
2. **Analisar e vincular paciente:** página de análise individual, aberta a partir de “Analisar”, com dados da solicitação, necessidade acadêmica, demandas compatíveis da fila, prioridade, tempo de espera, seleção de uma entrada, conferência do vínculo e ação “Aprovar e Vincular”. A alocação não é agendamento; dia e turno serão informados pelo aluno posteriormente.

As duas telas devem ser conteúdo do shell existente da secretaria. A sidebar, a topbar, a navegação para Pacientes e Fila de Espera e os padrões responsivos devem continuar pertencendo a `FilaEspera.jsx`/`FilaEspera.css`; os componentes da RF010 não devem recriá-los.

O botão “Marcar Pendente de Fila” mostrado na tela de análise é uma proposta visual ainda não suportada por endpoint identificado. Ele deve permanecer como comportamento bloqueado por DP-02 até que a equipe defina sua semântica, transição, permissão e contrato. Não criar uma operação implícita para atender ao layout.

O Figma exibe textos e quantidades de exemplo, como “4”, “3”, “12”, “2” e nomes de pacientes. Esses valores não são registros do sistema e não podem ser usados como dados iniciais, fallback ou fixture de produção.

O “saldo acadêmico”/“necessidade acadêmica” exibido na tela de análise deve ser relacionado a uma fonte acadêmica real antes de ser tornado obrigatório. A solicitação atual não armazena esse texto como um campo próprio; até a definição da consulta de origem, a tela deve exibir somente dados acadêmicos comprovadamente disponíveis ou sinalizar a ausência, sem inventar o saldo.

O campo de observação da análise também exige validação: o contrato atual de aprovação recebe apenas `filaEsperaId`. Até a definição de um campo e de sua persistência, o texto pode ser apenas uma informação de interface não submetida, ou a ação deve ser omitida; nenhuma alternativa deve ser assumida como implementada.

## 6. Regras de negócio

- **RN-01 — Estados existentes:** preservar os valores definidos na V4. “Pendente”, “Aprovada” e “Recusada” são rótulos de apresentação; nenhum novo estado persistido é aprovado por este documento. O tratamento de PENDENTE_FILA permanece em DP-02.
- **RN-02 — Aprovação com alocação:** exigir filaEsperaId, entrada AGUARDANDO e compatibilidade de procedimento, dente e classe conforme a condição atual do service. A aprovação e a alocação ocorrem conjuntamente: a solicitação resulta em APROVADO_E_ALOCADO, o paciente selecionado fica associado à solicitação e a entrada correspondente da fila resulta em ALOCADO.
- **RN-03 — Sem paciente elegível:** na regra atual, a falta de uma entrada compatível impede concluir a aprovação. Não aprovar sem paciente nem criar automaticamente PENDENTE_FILA para contornar essa condição.
- **RN-04 — Recusa:** exigir texto com conteúdo após remoção de espaços nas extremidades, respeitando o limite atual de 5.000 caracteres da entrada. Uma observação previamente escrita pelo aluno não atende à obrigação de a secretaria informar o motivo.
- **RN-05 — Separação de textos:** preservar a observação original do aluno. A justificativa de recusa compõe a resposta da secretaria; os nomes legados do payload não mudam sua autoria.
- **RN-06 — Decisões repetidas:** uma solicitação já aprovada/alocada ou recusada não pode receber nova aprovação ou recusa direta. A revisão deve passar pelo fluxo de reversão validado; não sobrescrever a decisão anterior.
- **RN-07 — Autoria:** obter o responsável da sessão autenticada e registrar o instante da operação no servidor. Não atribuir decisões a uma secretaria escolhida pelo cliente.
- **RN-08 — Histórico preservado:** cada decisão ou reversão bem-sucedida deve acrescentar um evento persistente. Preservar o estado anterior, o novo estado, a ação, o responsável, a data/hora, a justificativa de recusa e, quando afetados, os vínculos de fila e paciente anteriores e resultantes. Uma reversão deve identificar a decisão revertida.
- **RN-09 — Estado atual e passado:** o histórico deve continuar disponível após reversões e novas decisões. Uma justificativa antiga não pode aparecer como motivo de uma decisão atual diferente. O aluno deve visualizar claramente o resultado vigente.
- **RN-10 — Reversão consistente:** não considerar a reversão concluída se houver inconsistência entre solicitação, paciente e fila. O destino, a liberação de paciente e as condições para reverter uma aprovação já utilizada em outro fluxo devem ser definidos em DP-03 antes da implementação dessa operação.
- **RN-11 — Atomicidade e concorrência:** uma operação deve confirmar integralmente sua mudança e seu registro histórico ou não produzir efeitos parciais. Alterações concorrentes ou dados desatualizados não podem duplicar alocação, gerar dois eventos para uma única confirmação ou apagar uma decisão.
- **RN-12 — Propriedade do aluno:** a consulta do aluno deve se restringir às solicitações vinculadas à sua identidade autenticada. Alterar parâmetros ou identificadores no cliente não pode permitir consultar solicitações de outro aluno.
- **RN-13 — Sem efeitos externos:** aprovar, recusar ou reverter não deve criar agendamento, enviar mensagens ou abrir formulário de dia/turno.
- **RN-14 — Dados anteriores ao histórico:** não inventar autores, datas ou eventos passados. A estratégia de apresentação/importação do legado deve explicitar os limites dos dados disponíveis, conforme DP-05.

## 7. Critérios de aceitação

Os critérios descrevem o comportamento a validar em etapas futuras. Os casos vinculados a decisões pendentes só poderão ser concluídos depois da definição correspondente.

| Critério | Cenário e resultado esperado | Referências |
| --- | --- | --- |
| CA-01 | Dada uma secretaria autenticada, ao acessar “Solicitações”, a tela abre no mesmo layout da Fila de Espera, destaca o item do menu e mantém a navegação de retorno à fila e aos pacientes. | RF010-01, RF010-10 |
| CA-02 | Havendo pendentes, aprovadas/alocadas e recusadas, a secretaria consegue consultar cada grupo e reconhecer o estado dos registros. Uma consulta sem registros apresenta mensagem de lista vazia. PENDENTE_FILA permanece visível e será agrupado conforme DP-02. | RF010-01, RF010-11 |
| CA-03 | Dada uma solicitação com dados relacionados, a listagem exibe aluno, turma separadamente, semestre atual do aluno obtido de `usuarios.semestre_atual`, procedimento, dente, classe, data desejada e paciente quando associado. Campos opcionais ausentes recebem os textos definidos em 5.1. | RF010-02 |
| CA-04 | Ao abrir detalhes, a secretaria visualiza separadamente a observação do aluno e a resposta da secretaria, além das datas e do responsável disponíveis. | RF010-03 |
| CA-05 | Dada uma solicitação elegível e uma entrada compatível AGUARDANDO, ao aprovar, a solicitação resulta em APROVADO_E_ALOCADO, o paciente selecionado fica associado, a entrada correspondente resulta em ALOCADO, e data de saída, responsável, data de análise e novo evento histórico são persistidos conjuntamente. A operação não cria agendamento nem informa dia ou turno. | RF010-04, RF010-07, RN-02, RN-13 |
| CA-06 | Se faltar filaEsperaId ou a entrada não existir, não estiver AGUARDANDO ou não cumprir a compatibilidade atual de procedimento/dente/classe, a aprovação e a alocação falham com erro adequado e não alteram a solicitação, o paciente, a fila nem o histórico de decisões. | RF010-04, RN-02, RN-03 |
| CA-07 | Dada uma solicitação elegível, ao recusar com justificativa válida, o estado passa a RECUSADO, a resposta da secretaria recebe o texto validado, a observação do aluno permanece intacta e ficam registrados responsável, data/hora e evento histórico. | RF010-05, RN-04, RN-05 |
| CA-08 | Justificativa nula, vazia, composta apenas por espaços ou acima do limite não conclui a recusa. A tentativa direta à API também é rejeitada sem alterar o estado ou criar decisão. | RF010-05, RN-04 |
| CA-09 | Após uma aprovação ou recusa confirmada, ao atualizar “Minhas requisições”, o aluno proprietário visualiza a decisão vigente e, em caso de recusa, a justificativa completa. O resultado continua disponível após nova consulta. | RF010-06 |
| CA-10 | Um aluno não consegue consultar solicitações de outro aluno por manipulação de identificadores ou parâmetros. Usuário sem autenticação não acessa dados do RF010. | RF010-06, RF010-09, RN-12 |
| CA-11 | ALUNO, COORDENADOR e outros perfis não conseguem aprovar, recusar ou reverter por chamada direta ao backend; também não acessam a listagem administrativa ou o histórico da secretaria. As tentativas não produzem mudanças. | RF010-09 |
| CA-12 | Havendo uma sequência de decisões e reversões, a secretaria consulta os eventos em ordem cronológica, com autor, data/hora, estados, justificativas aplicáveis e vínculo da reversão com a decisão revertida. Eventos anteriores continuam íntegros após novas operações. | RF010-07, RN-08, RN-09 |
| CA-13 | Dada uma recusa elegível para reversão, a secretaria executa a operação segundo DP-03. O estado resultante reflete a política validada, um novo evento é acrescentado e a recusa original e sua justificativa permanecem no histórico. O aluno passa a ver o estado vigente. | RF010-08, RF010-06, RN-09 |
| CA-14 | Dada uma aprovação com paciente alocado, a reversão respeita os efeitos e impedimentos definidos em DP-03. Se não puder preservar a consistência com a fila e eventuais usos posteriores do vínculo, é rejeitada sem efeitos parciais. | RF010-08, RN-10 |
| CA-15 | Uma nova aprovação ou recusa direta sobre solicitação já decidida é rejeitada como conflito, sem sobrescrever a decisão. Duas ações concorrentes sobre a mesma versão não podem ambas produzir decisões incompatíveis; a interface deve orientar uma nova consulta. | RN-06, RN-11, RF010-11 |
| CA-16 | Durante carregamento ou envio, a interface informa o andamento e evita envios repetidos. Falha de rede, erro de validação, acesso negado ou conflito não é mostrado como sucesso nem como lista vazia válida. | RF010-11 |
| CA-17 | Em tela ampla e em larguras menores, a tela preserva a paleta, a hierarquia, os espaçamentos, o menu móvel e o padrão de tabela/modal da Fila de Espera. A tabela permite consultar todas as colunas, com rolagem horizontal quando necessária. | RF010-10 |
| CA-18 | Solicitação anterior à criação do histórico é apresentada conforme DP-05, distinguindo fatos disponíveis de eventos sem registro, sem inventar uma sequência de decisões. | RF010-07, RN-14 |
| CA-19 | Aprovar, recusar ou reverter não envia mensagens, não cria consulta/agendamento nem solicita preenchimento posterior de dia/turno. | RN-13, escopo |
| CA-20 | Dada uma secretaria autenticada, a tela principal apresenta indicadores calculados por dados reais, busca por matrícula/requisição e filtros por status e disciplina. Os valores ilustrados no Figma não aparecem sem dados correspondentes no backend. | RF010-12, RF010-11 |
| CA-21 | A tabela da tela principal lista solicitações anteriores e atuais com aluno, procedimento, dente/classe, disciplina, status, semestre atual do aluno e turma conforme os dados disponíveis, oferecendo “Analisar” para os casos elegíveis e consulta para os demais. | RF010-02, RF010-13, DP-04 |
| CA-22 | Ao acionar “Analisar”, a secretaria abre a tela individual dentro do shell de Fila de Espera, visualiza os dados da solicitação e retorna à listagem sem perder o contexto de navegação. | RF010-14, RF010-10 |
| CA-23 | Na tela de análise, a lista de pacientes/demandas é obtida da fila real e respeita procedimento, dente e classe; cada entrada apresenta prioridade e tempo de espera retornados pelo backend, sem ordenar ou inventar critérios diferentes dos existentes. | RF010-15, RN-02, RN-03 |
| CA-24 | Sem entrada compatível, a tela informa a ausência de correspondência. “Marcar Pendente de Fila” não altera o estado enquanto DP-02 não definir endpoint, transição e permissões. | RF010-15, RN-01, DP-02 |
| CA-25 | Com entrada compatível selecionada, a tela apresenta a conferência do vínculo e “Aprovar e Vincular”. A confirmação resulta em APROVADO_E_ALOCADO, fila ALOCADO e paciente associado, sem criar agendamento. | RF010-17, RN-02, RN-13 |
| CA-26 | A tentativa de aprovar sem seleção, com entrada incompatível ou com dados desatualizados é rejeitada sem efeitos parciais e orienta nova consulta quando houver conflito. | RF010-17, RN-03, RN-11 |
| CA-27 | O fluxo administrativo oferece recusa mesmo que a ação não esteja representada nos prints. Sem justificativa válida, a recusa não é enviada; com justificativa válida, preserva a observação do aluno e registra a resposta da secretaria. | RF010-18, RF010-05, RN-04, RN-05 |
| CA-28 | O histórico e as ações de reversão são acessíveis na interface somente conforme os contratos definidos para DP-03 e DP-05. Antes dessas decisões, a tela não apresenta uma transição ou destino como se estivesse aprovado. | RF010-19, DP-03, DP-05 |
| CA-29 | O indicador “Retornos sugeridos” só é exibido se sua origem, escopo e contrato forem validados; caso contrário, a RF010 não o apresenta e não usa contagens fictícias. | RF010-12, escopo |

## 8. Referência visual e comportamento da interface

A futura tela de solicitações deve reutilizar a estrutura `.aplicacao`, menu `.sidebar`, barra `.topbar` e área `.conteudo` da secretaria. Não criar uma experiência visual independente para o RF010.

Referências existentes em FilaEspera.css:

| Elemento | Padrão a reutilizar |
| --- | --- |
| Tipografia e cores | Inter/system-ui; textos em tons de azul/cinza; ação principal #c94c0e; destaque suave #fff0e7; superfícies brancas e fundo #f6f8fa. |
| Menu lateral | Largura principal de 226px, borda discreta e estado ativo alaranjado. |
| Barra superior | Altura principal de 69px, identificação da área e do usuário. |
| Conteúdo | Largura máxima de 1480px e espaçamento principal de 40px 36px 48px, com adaptações existentes. |
| Tabelas e formulários | Mesmos padrões de borda, raio, alinhamento, botões primário/secundário, campos e modais. |
| Responsividade | Referências de 1100px, 850px e 580px; menu móvel e rolagem horizontal de tabela conforme necessário. |

As diferenças necessárias ao conteúdo de solicitações devem se integrar a esses padrões. Não é necessário reproduzir indicadores de especialidades nem regras de prioridade da fila na listagem de solicitações.

Decisões e erros devem ser identificáveis por texto, sem depender apenas de cor. Campos obrigatórios devem ser rotulados; ações de recusa e reversão devem deixar explícita a solicitação afetada. Modal de recusa deve permitir cancelar sem alterar dados.

## 9. Restrições e limites técnicos desta etapa

1. Nesta etapa documental, modificar somente `docs/rf010-validacao-solicitacoes/spec.md` e `docs/rf010-validacao-solicitacoes/plan.md`, relativos à raiz da aplicação odontologia-api.
2. Não alterar arquivos Java, React, CSS, SQL, testes ou configurações.
3. Não executar migrações, SQL, conexões de escrita ou alterações no banco compartilhado.
4. Não executar comandos Git.
5. Não implementar endpoints, componentes, tabelas, histórico ou reversão nesta etapa.
6. Não executar a aplicação nem testes que possam inicializar serviços ou acessar o banco; a análise desta etapa é estática.
7. Não modificar as migrações existentes para antecipar decisões. Qualquer necessidade futura de persistência será tratada em planejamento posterior.
8. Preservar o fluxo confirmado de aprovação com alocação, sem criar aprovação sem paciente nesta etapa. A aprovação não cria agendamento nem define dia ou turno.
9. Reutilizar a autenticação e o cliente api.js existentes; não propor alteração de configuração como parte desta entrega.

A forma de armazenamento do histórico, os contratos adicionais de consulta/reversão e eventuais migrações futuras não estão implementados nem definidos por comandos neste documento. O requisito obrigatório é preservar os fatos e a consistência descritos.

## 10. Decisões pendentes para validação com a equipe

| Identificador | Questão | Evidência e impacto | Conduta até a decisão |
| --- | --- | --- | --- |
| DP-01 | **Resolvida:** aprovar deve continuar alocando um paciente imediatamente ou haverá aprovação separada da alocação? | A aprovação continuará exigindo que a secretaria selecione uma entrada compatível da fila de espera. A aprovação e a alocação ocorrerão na mesma operação, conforme o comportamento atual do backend. | Aplicar a operação conjunta: a solicitação fica APROVADO_E_ALOCADO, a entrada correspondente fica ALOCADO e o paciente selecionado fica associado à solicitação. A aprovação não é agendamento; dia e turno serão informados pelo aluno posteriormente, em outra funcionalidade. |
| DP-02 | Qual é a semântica de PENDENTE_FILA e em qual grupo ele deve aparecer? | O service permite aprovar/recusar nesse estado, GET /pendentes não o retorna e o frontend do aluno o rotula como aprovado aguardando paciente. A divergência afeta filtros, rótulos e ações permitidas. | Registrar a divergência, exigir visibilidade do estado na futura consulta e não alterar sua semântica por suposição. |
| DP-03 | Como funciona a reversão de cada decisão? Qual é o estado de destino, quem pode reverter em cada situação, existe prazo ou motivo obrigatório e o que ocorre com a alocação? | Definir se reabre em PENDENTE_APROVACAO ou restaura outro estado; se libera ou mantém paciente/fila; como tratar entrada posterior duplicada, nova alocação ou vínculo já utilizado em consulta/atendimento. Também definir se toda secretaria pode reverter decisão de outra secretaria. A restrição ao perfil SECRETARIA já está estabelecida. | Não implementar reversão antes dessas definições. Em qualquer alternativa, preservar a decisão revertida e registrar novo evento; nunca assumir que basta trocar o status. |
| DP-04 | **Resolvida:** qual semestre deve ser exibido na solicitação? | O semestre solicitado é o semestre atual do aluno, obtido de `usuarios.semestre_atual`. A turma deve ser exibida separadamente, sem usar `turmas.semestre_letivo` como substituto do semestre do aluno. O modelo existente possui `semestre_atual` em `usuarios` e a solicitação mantém `turma_id`, permitindo apresentar as duas informações com significados distintos. | Exibir “Semestre atual do aluno” a partir de `usuarios.semestre_atual` e “Turma” separadamente. Não inventar um semestre a partir da turma nem tratar o semestre letivo da turma como o valor decidido para esta informação. |
| DP-05 | Como apresentar decisões antigas e armazenar o histórico futuro? | Os campos atuais guardam apenas a última análise; a cópia realizada pela V12 não reconstitui eventos. Avaliar uso da auditoria geral ou armazenamento específico e como identificar registros legados com histórico parcial. | Não inferir eventos ausentes nem executar recuperação/migração nesta etapa. O histórico futuro deve atender RN-08 a RN-11 independentemente da solução escolhida. |

DP-01 e DP-04 estão resolvidas e devem orientar os contratos e critérios de aprovação e apresentação. As decisões DP-02, DP-03 e DP-05 continuam pendentes e bloqueiam a definição final dos contratos e critérios que dependem de sua validação. O documento não autoriza implementação nesta etapa.

