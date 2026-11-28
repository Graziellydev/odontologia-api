# RF010 — Checklist de implementação

Este checklist deriva de [spec.md](./spec.md) e [plan.md](./plan.md). Situação atualizada em 02/11/2026. Apenas itens com evidência de implementação ou validação foram marcados. A RF010 não está integralmente concluída.

Regras para todas as tarefas:

- Não usar nomes, contagens, pacientes, prioridades ou datas ilustrativas do Figma como dados reais.
- Reutilizar autenticação, `frontend/src/api.js`, menu, shell e estilos existentes.
- Não alterar o fluxo de criação de solicitações da Sprint 1.
- Não implementar WhatsApp, mensagens automáticas, agendamento, dia/turno ou migração nesta etapa.
- Não persistir observação da análise ou saldo acadêmico sem fonte e contrato confirmados.
- DP-02, DP-03 e DP-05 continuam pendentes.

## Tarefas

### T01 — Verificar baseline do código e dos testes

- [x] **O que fazer:** registrar o estado atual dos módulos de solicitações, fila, consultas, frontend, endpoints existentes e testes; confirmar que a análise é somente estática e não acessa o banco compartilhado.
- **Arquivos envolvidos:** `../../backend/src/main/java/.../solicitacoes/`, `../../backend/src/main/java/.../filaespera/`, `../../backend/src/main/java/.../retornos/`, `../../frontend/src/FilaEspera.jsx`, `../../frontend/src/FilaEspera.css`, `../../frontend/src/api.js`, `../../backend/src/test/`.
- **Critérios relacionados:** CA-01, CA-02, CA-03, CA-17; restrições da seção 9.
- **Verificação:** produzir uma lista de endpoints, componentes, testes existentes e lacunas; confirmar que não houve escrita, migração, comando Git ou alteração no banco.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Análise estática e baseline dos módulos, documentos e contratos feitos sem consultar o banco.

### T02 — Criar consulta administrativa enriquecida

- [x] **O que fazer:** criar a consulta administrativa no Spring Boot usando dados reais de aluno, turma, disciplina, procedimento, dente, classe, data desejada, status, semestre atual do aluno (`usuarios.semestre_atual`) e paciente associado quando houver. Preservar `PENDENTE_FILA` sem decidir sua semântica.
- **Arquivos envolvidos:** `../../backend/src/main/java/.../solicitacoes/controller/SolicitacaoPacienteController.java`, `../../backend/src/main/java/.../solicitacoes/service/SolicitacaoPacienteService.java`, `../../backend/src/main/java/.../solicitacoes/repository/SolicitacaoPacienteRepository.java`, novos DTOs em `../../backend/src/main/java/.../solicitacoes/dto/`.
- **Critérios relacionados:** RF010-01, RF010-02, RF010-03, RF010-13; CA-02, CA-03, CA-04, CA-21.
- **Verificação:** teste de consulta com relações preenchidas e ausentes; confirmar que nomes vêm das relações, o semestre vem de `usuarios.semestre_atual`, a turma permanece separada e nenhum registro fictício é criado.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Consulta com dados reais já confirmada pelo usuário. Resposta administrativa ampliada com campos existentes de decisão; testes de projeção/mapper passaram. SQL não executado no banco compartilhado nesta entrega.

### T03 — Criar `ValidacaoSolicitacoes.jsx` e estilos da Tela 1

- [x] **O que fazer:** criar a tela principal com indicadores estruturais, busca, filtros, tabela, estados de carregamento/erro/lista vazia e ações “Analisar”/consulta. Reproduzir a hierarquia visual do Figma usando classes existentes e CSS complementar escopado.
- **Arquivos envolvidos:** `../../frontend/src/ValidacaoSolicitacoes.jsx`, `../../frontend/src/ValidacaoSolicitacoes.css`, `../../frontend/src/api.js` para consumo da consulta.
- **Critérios relacionados:** RF010-10, RF010-11, RF010-12, RF010-13; CA-01, CA-02, CA-16, CA-17, CA-20, CA-21.
- **Verificação:** renderizar com respostas reais e estados de erro; confirmar que a tela não cria sidebar/topbar própria e não usa dados fixos do Figma.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Tela com dados reais confirmada pelo usuário. Falhas não são apresentadas como contagens zero ou lista vazia válida.

### T04 — Integrar “Solicitações” ao shell da secretaria

- [x] **O que fazer:** substituir o aviso provisório do item “Solicitações” em `FilaEspera.jsx` pela renderização de `ValidacaoSolicitacoes`; preservar navegação para Pacientes e Fila de Espera, sidebar, topbar, menu móvel e usuário autenticado.
- **Arquivos envolvidos:** `../../frontend/src/FilaEspera.jsx`, `../../frontend/src/FilaEspera.css`, `../../frontend/src/ValidacaoSolicitacoes.jsx`.
- **Critérios relacionados:** RF010-01, RF010-10, RF010-14; CA-01, CA-22.
- **Verificação:** clicar entre Fila de Espera, Pacientes e Solicitações em viewport ampla e móvel; confirmar que existe um único shell visual.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Shell único e navegação entre solicitações, análise, fila e pacientes cobertos por testes de interface.

### T05 — Implementar busca, filtros e indicadores reais

- [ ] **O que fazer:** implementar busca por matrícula/requisição, filtros por status e disciplina e indicadores derivados das respostas do backend. Definir contrato para contagens sem esconder estados. O indicador “Retornos sugeridos” só poderá ser implementado se sua origem e escopo forem aprovados; não usar contagens do Figma.
- **Arquivos envolvidos:** `../../frontend/src/ValidacaoSolicitacoes.jsx`, `../../frontend/src/ValidacaoSolicitacoes.css`, controller/service/repository administrativos e DTOs de T02.
- **Critérios relacionados:** RF010-12, RF010-13; CA-02, CA-20, CA-21, CA-29.
- **Verificação:** testar filtros combinados, lista vazia, atualização e contagens com dados reais; confirmar que `PENDENTE_FILA` não é agrupado por suposição e que “Retornos sugeridos” não aparece sem contrato.
- **Situação:** PARCIAL.
- **Evidência/limite:** Busca, filtros básicos e indicadores reais funcionam. DP-02 e origem/escopo de retornos sugeridos continuam pendentes; sem agrupamento ou transição inventados.

### T06 — Criar `AnalisarSolicitacao.jsx` e estilos da Tela 2

- [x] **O que fazer:** criar a tela de análise com dados da solicitação/aluno, procedimento, dente, classe, disciplina, turma, semestre atual, necessidade acadêmica opcional, seleção de demanda, conferência, recusa e aprovação. Reproduzir o Figma sem inventar saldo acadêmico ou persistência da observação.
- **Arquivos envolvidos:** `../../frontend/src/AnalisarSolicitacao.jsx`, `../../frontend/src/AnalisarSolicitacao.css`, `../../frontend/src/api.js`.
- **Critérios relacionados:** RF010-03, RF010-14, RF010-15, RF010-16, RF010-17, RF010-18; CA-04, CA-22, CA-23, CA-25, CA-27.
- **Verificação:** abrir uma solicitação real, conferir os dados exibidos, selecionar uma demanda localmente e validar estados de carregamento/erro; manter observação/saldo como opcionais até seus contratos.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Validação visual anterior informada pelo usuário e navegação coberta por testes. Modais novos aguardam T14.

### T07 — Consultar demandas compatíveis da fila

- [ ] **O que fazer:** reutilizar `GET /api/fila-espera` com `procedimentoId`, `dente` e `classe` da solicitação; aceitar somente entradas `AGUARDANDO`; apresentar paciente, prioridade, data de entrada e tempo de espera na ordenação oficial retornada pelo backend.
- **Arquivos envolvidos:** `../../frontend/src/AnalisarSolicitacao.jsx`, `../../frontend/src/api.js`, `../../backend/src/main/java/.../filaespera/controller/FilaEsperaController.java`, `../../backend/src/main/java/.../filaespera/repository/FilaEsperaRepository.java`, DTOs da fila.
- **Critérios relacionados:** RF010-15, RN-02, RN-03; CA-23, CA-24, CA-26.
- **Verificação:** testar correspondência por procedimento/dente/classe, ausência de entradas, prioridade e dias de espera; confirmar que a seleção não altera a fila antes da aprovação.
- **Situação:** IMPLEMENTADA E TESTADA; REVISÃO NO NAVEGADOR PENDENTE.
- **Evidência/limite:** Testes controlados cobrem filtros, ordem, exclusão de alocados, seleção exclusiva, troca de solicitação e atualização. Mantida a restrição anterior de marcar conclusão somente após validar a seleção no navegador.

### T08 — Integrar recusa com justificativa obrigatória

- [x] **O que fazer:** disponibilizar a recusa no fluxo administrativo, mesmo não estando visível nos prints; validar justificativa no frontend e backend, preservar a observação do aluno e exibir a resposta da secretaria.
- **Arquivos envolvidos:** `../../frontend/src/AnalisarSolicitacao.jsx`, `../../frontend/src/ValidacaoSolicitacoes.jsx`, CSSs complementares, `../../backend/src/main/java/.../solicitacoes/controller/SolicitacaoPacienteController.java`, `../../backend/src/main/java/.../solicitacoes/service/SolicitacaoPacienteService.java`, `RecusarSolicitacaoDTO` e testes existentes.
- **Critérios relacionados:** RF010-05, RF010-18, RN-04, RN-05; CA-07, CA-08, CA-27.
- **Verificação:** testar justificativa nula, vazia, espaços, limite e chamada direta à API; confirmar `RECUSADO`, `respostaSecretaria` correta e observação do aluno intacta.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Recusa com modal, justificativa/limite/trim, envio único, erros, preservação do texto do aluno e atualização de tabela/indicadores. Testes isolados passaram; nenhuma recusa real executada.

### T09 — Integrar aprovação com alocação conjunta

- [x] **O que fazer:** ligar “Aprovar e Vincular” ao endpoint existente, exigindo seleção de uma entrada compatível e mantendo a operação transacional: solicitação `APROVADO_E_ALOCADO`, fila `ALOCADO` e paciente associado.
- **Arquivos envolvidos:** `../../frontend/src/AnalisarSolicitacao.jsx`, `../../frontend/src/api.js`, `../../backend/src/main/java/.../solicitacoes/controller/SolicitacaoPacienteController.java`, `../../backend/src/main/java/.../solicitacoes/service/SolicitacaoPacienteService.java`, `AprovarSolicitacaoDTO`, entidades/repositórios de solicitação e fila.
- **Critérios relacionados:** DP-01, RF010-04, RF010-17, RN-02, RN-03, RN-13; CA-05, CA-06, CA-25, CA-26.
- **Verificação:** aprovar com entrada compatível e conferir os três estados/vínculos; tentar aprovar sem seleção ou com incompatibilidade; confirmar ausência de consulta, agenda, mensagem e definição de dia/turno.
- **Situação:** CONCLUÍDA.
- **Evidência/limite:** Confirmação e chamada única PATCH /aprovar com filaEsperaId; conflitos invalidam seleção e recarregam dados. Sem /alocar separado ou agendamento. Testes isolados passaram; nenhuma aprovação real executada.

### T10 — Testar operações, permissões, erros e concorrência

- [ ] **O que fazer:** cobrir listagem, detalhes, fila compatível, recusa e aprovação; validar `SECRETARIA`, `ALUNO`, `COORDENADOR`, não autenticado, propriedade do aluno, `401/403/404/409`, atomicidade e duas ações concorrentes sobre a mesma versão.
- **Arquivos envolvidos:** `../../backend/src/test/`, testes de controller/service/repository de solicitações e fila, testes frontend dos dois componentes quando disponíveis.
- **Critérios relacionados:** RF010-09, RF010-11, RN-07, RN-11, RN-12; CA-10, CA-11, CA-15, CA-16, CA-26.
- **Verificação:** executar a suíte focada sem banco compartilhado; confirmar que falhas não deixam solicitação aprovada sem paciente, fila alocada sem solicitação ou sucesso indevido.
- **Situação:** PARCIAL — TESTES ISOLADOS PASSARAM.
- **Evidência/limite:** 31 testes frontend e 32 Java passaram, incluindo autorização efetiva de métodos, DTOs e tradução de conflito para 409. Faltam testes com PostgreSQL descartável para comprovar rollback/concorrência real e matriz HTTP completa dos filtros de sessão. Teste de contexto completo excluído para não acessar Supabase.

### T11 — Planejar histórico de decisões

- [ ] **O que fazer:** definir, após DP-05, tabela/contrato específico para decisões e reversões, campos, retenção, consulta cronológica, relação com `logs_auditoria`, integridade e ausência de backfill de registros antigos.
- **Arquivos envolvidos:** `../../docs/rf010-validacao-solicitacoes/spec.md`, `../../docs/rf010-validacao-solicitacoes/plan.md`, futuros DTOs/repositório/serviço de histórico e futura migração aprovada.
- **Critérios relacionados:** RF010-07, RF010-19, RN-08, RN-09, RN-14; CA-12, CA-18, CA-28.
- **Verificação:** obter decisão formal sobre o modelo; somente depois criar contrato, testes e migração. Confirmar que nenhum evento histórico anterior é inventado.
- **Situação:** BLOQUEADA POR DP-05.
- **Evidência/limite:** Sem armazenamento/contrato de histórico confirmado. Nenhuma tabela, migração, endpoint, backfill ou evento fictício criado. A última decisão exibida não é histórico completo.

### T12 — Planejar reversão de decisões

- [ ] **O que fazer:** definir, após DP-03, interface, endpoint, justificativa, autorização, estado de destino, liberação da fila, impedimentos por consultas/atendimentos, concorrência e comportamento para retornos, transferências e paciente reutilizado.
- **Arquivos envolvidos:** `../../docs/rf010-validacao-solicitacoes/spec.md`, `../../docs/rf010-validacao-solicitacoes/plan.md`, futuros controller/service/DTOs e consultas de `consultas`/`fila_espera`.
- **Critérios relacionados:** RF010-08, RF010-19, RN-06, RN-08, RN-10, RN-11; CA-13, CA-14, CA-15, CA-28.
- **Verificação:** registrar aprovação da equipe para cada transição e impedimento; não implementar apenas trocando o status da solicitação.
- **Situação:** BLOQUEADA POR DP-03.
- **Evidência/limite:** Destino, elegibilidade e efeitos na alocação não confirmados. Reversão não implementada nem habilitada.

### T13 — Registrar pendência de `PENDENTE_FILA`

- [ ] **O que fazer:** documentar e confirmar o significado, agrupamento, permissões e endpoint do estado `PENDENTE_FILA`, incluindo o botão “Marcar Pendente de Fila” exibido no Figma. Não criar transição implícita.
- **Arquivos envolvidos:** `../../docs/rf010-validacao-solicitacoes/spec.md`, `../../docs/rf010-validacao-solicitacoes/plan.md`, futuro controller/service somente após decisão.
- **Critérios relacionados:** RF010-01, RF010-12, RF010-15; RN-01, RN-03; CA-02, CA-24, CA-29.
- **Verificação:** obter decisão formal sobre semântica e contrato; enquanto isso, manter o estado visível sem alterar sua permissão ou comportamento por suposição.
- **Situação:** BLOQUEADA POR DP-02.
- **Evidência/limite:** PENDENTE_FILA permanece visível. Nenhuma nova transição ou ação de decisão habilitada para esse estado. Marcar Pendente de Fila continua desabilitado.

### T14 — Revisar o visual e executar testes finais

- [ ] **O que fazer:** comparar as duas telas implementadas com o Figma, validar responsividade e navegação, revisar textos para não sugerirem agendamento, confirmar dados reais e executar a suíte final da RF010.
- **Arquivos envolvidos:** `../../frontend/src/ValidacaoSolicitacoes.jsx`, `../../frontend/src/ValidacaoSolicitacoes.css`, `../../frontend/src/AnalisarSolicitacao.jsx`, `../../frontend/src/AnalisarSolicitacao.css`, `../../frontend/src/FilaEspera.jsx`, `../../frontend/src/FilaEspera.css`, `../../backend/src/test/`.
- **Critérios relacionados:** RF010-10, RF010-11, RF010-12, RF010-13, RF010-14; CA-01, CA-16, CA-17, CA-19, CA-20, CA-21, CA-22, CA-29.
- **Verificação:** revisão visual em larguras ampla e móvel, testes de navegação, estados de erro e operações; confirmar que pendências DP-02/DP-03/DP-05 não foram mascaradas como resolvidas.
- **Situação:** PENDENTE.
- **Evidência/limite:** Figma consultado, testes e build passaram. Revisão visual automática indisponível: browser access host unavailable. Usar o servidor isolado 127.0.0.1:5174 para revisar. Conclusão global depende das decisões obrigatórias restantes.

## Resumo de situação

- **Concluídas:** T01, T02, T03, T04, T06, T08 e T09.
- **Implementada/testada, aguardando seleção no navegador:** T07.
- **Parciais:** T05 e T10.
- **Bloqueadas por decisão da equipe:** T11, T12 e T13.
- **Revisão final pendente:** T14.
- **RF010 completa:** não. Histórico e reversão são obrigatórios e continuam bloqueados.

Consulte [validacao-isolada.md](./validacao-isolada.md) para resultados, comandos, arquivos alterados, roteiro e limites. Fixtures estão apenas em frontend/tests, fora da aplicação de produção.
