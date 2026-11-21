# Tasks

## 1. Contratos clínicos e autorização

- [ ] 1.1 Revisar os mapeamentos e projeções de prontuário, adendo, anamnese e odontograma contra V6/V9, sem acrescentar colunas, e verificar a inicialização com `spring.jpa.hibernate.ddl-auto=validate`.
- [ ] 1.2 Centralizar a matriz de autorização por operação e status (`AGENDADO`/`CONFIRMADA_24H` para criação; também `REALIZADO` para leitura/adendo) e verificar em testes de serviço os casos de aluno vinculado, não vinculado e turno estrito.
- [ ] 1.3 Manter a resolução segura de IP por proxy confiável/CIDR e verificar com testes MockMvc o HTTP 403 e a mensagem exata de bloqueio de rede.
- [ ] 1.4 Restringir o perfil `COORDENADOR` a GET de auditoria e verificar que POST de evolução ou adendo responde 403, preservando o fluxo de sessão e CSRF do projeto.

## 2. Histórico, evolução imutável e encerramento

- [ ] 2.1 Ajustar o DTO e a consulta de `GET /api/prontuarios/paciente/{pacienteId}` para o resumo mínimo do paciente, atendimento, anamnese latest, odontograma, evoluções e adendos; verificar ordenação e ausência de CPF, telefone e endereço com testes de integração.
- [ ] 2.2 Ajustar `POST /api/prontuarios` para derivar dados da consulta, validar procedimento/dente/classe e produzir o selo SHA-256 temporal versionado; verificar assinatura, `UNIQUE (consulta_id)` e rejeições de entrada em testes.
- [ ] 2.3 Garantir que gravação de evolução, observação odontográfica opcional e conclusão solicitada ocorram atomicamente com `app.origem='ALUNO'` e `app.usuario_id`; verificar rollback e `consulta_historico` em PostgreSQL.
- [ ] 2.4 Manter `POST /api/prontuarios/{prontuarioId}/adendos` como inserção pós-assinatura e verificar texto inválido, vínculo do aluno e bloqueio de UPDATE/DELETE pelos triggers reais.
- [ ] 2.5 Integrar o estado de encerramento à operação RF003 existente, evitando conclusão duplicada; verificar `Necessita Retorno` com justificativa e fila preservada, `Tratamento Concluído` sem fila e erro para justificativa ausente.

## 3. Página contextual de prontuário

- [ ] 3.1 Substituir o ponto de entrada drawer por uma página contextual aberta de `Pacientes do turno`, mantendo `← Voltar ao turno`; verificar seleção, retorno e preservação da sessão em teste de componente.
- [ ] 3.2 Construir a estrutura medida do frame `4:46292` (shell, cabeçalho, faixa de ambiente, resumo, abas, grid 354/738 e card de encerramento) em React e CSS modular; verificar visualmente em 1440 px e em viewport estreito.
- [ ] 3.3 Conectar a aba `Evolução clínica` aos dados de histórico e ao formulário, exibindo estado salvo/assinado imutável após sucesso; verificar renderização, transição de abas e atualização após POST em testes de componente.
- [ ] 3.4 Conectar a aba `Histórico de procedimentos` ao odontograma e procedimentos consolidados; verificar estado vazio e dados disponíveis em testes de componente.
- [ ] 3.5 Implementar adendo como interação compacta e alertas de rede/vínculo/turno acionados por HTTP 403, sem renderizar conteúdo clínico negado; verificar sucesso, cancelamento e bloqueio em testes.

## 4. Proteção visual e encerramento na página

- [ ] 4.1 Aplicar marca d'água diagonal repetida com nome, matrícula/registro e relógio atualizado, sem interceptar interação; verificar `pointer-events: none` e atualização visual do timestamp.
- [ ] 4.2 Aplicar `user-select: none` somente às áreas clínicas de leitura, `color-scheme: light` e ocultação da página clínica em `@media print`; verificar que controles, campos e diálogo de adendo continuam operáveis.
- [ ] 4.3 Incorporar o card de encerramento com as opções `Necessita Retorno` e `Tratamento Concluído`, justificativa condicional e mensagens retornadas pelo RF003; verificar submissão e estados de erro/sucesso em testes de componente.

## 5. Validação integrada

- [ ] 5.1 Executar os testes backend com o perfil `supabase-test`, usando credenciais somente por variáveis de ambiente, e verificar `ddl-auto=validate`, schema Flyway, triggers de imutabilidade, auditoria e histórico.
- [ ] 5.2 Executar `npm test` e `npm run build` em `frontend/`, corrigindo falhas de compilação e regressões nos fluxos do aluno antes de concluir a mudança.
- [ ] 5.3 Revisar manualmente a página contra o frame Figma `4:46292`, incluindo dimensões medidas, tokens, abas, bloqueios, selo e encerramento; registrar diferenças justificadas apenas para overlays sem medidas no frame.

## Workflow follow-up

- Arquivar a mudança após revisão e aprovação da implementação.
