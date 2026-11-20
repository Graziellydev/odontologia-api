# Tasks

## 1. Backend: mapeamento e acesso aos dados

- [x] 1.1 Mapear `prontuarios`, `prontuario_adendos`, `paciente_anamnese` e `paciente_odontograma` somente com colunas de V6/V9; validar a inicialização com `spring.jpa.hibernate.ddl-auto=validate` contra o banco de desenvolvimento.
- [x] 1.2 Implementar consultas para vínculo aluno/paciente/consulta/turno, anamnese mais recente, odontograma, histórico de evoluções e adendos; cobrir ordenação e casos sem anamnese/histórico com testes de repositório.
- [x] 1.3 Testar o mapeamento e consultas sem criar migration ou alterar constraints e confirmar que `consulta_id` continua impedindo mais de um prontuário por consulta.

## 2. Backend: autorização de rede, vínculo e turno

- [x] 2.1 Configurar `app.prontuario.allowed-cidrs` com loopbacks e faixas locais por ambiente e `app.prontuario.validar-turno-estrito=true`; verificar o binding das propriedades e os defaults com teste de configuração.
- [x] 2.2 Implementar resolução segura de IP (`X-Forwarded-For` apenas de proxy confiável; fallback para `remoteAddr`) e matcher IPv4/IPv6 CIDR; verificar allow/deny, proxy confiável e cabeçalho forjado com testes unitários.
- [x] 2.3 Implementar autorização de consulta ativa usando `AGENDADO`/`CONFIRMADA_24H`, aluno autenticado, paciente e vaga em `CURRENT_DATE`; verificar janelas `[08:00,12:00)` e `[13:00,17:00)`, flag estrita desativada e negações 403 em testes de serviço.
- [x] 2.4 Configurar autorização de `/api/prontuarios/**` para `ALUNO` e `COORDENADOR`, restringindo coordenador a GET, e verificar as respostas JSON 403 e a mensagem exata de bloqueio de rede em testes de segurança.

## 3. Backend: histórico, evolução e adendo

- [x] 3.1 Criar DTOs de request/response e serviço transacional do histórico; verificar minimização de dados, identificação básica, anamnese latest, odontograma, consulta ativa, ordenação das evoluções e adendos em testes de serviço.
- [x] 3.2 Implementar `GET /api/prontuarios/paciente/{pacienteId}` com autorização centralizada; verificar retorno para aluno autorizado e coordenador e ausência de dados clínicos para aluno bloqueado com testes MVC.
- [x] 3.3 Implementar `POST /api/prontuarios`, derivando paciente/aluno/turma da consulta e sessão, validando procedimento, dente/classe e evolução e gerando assinatura versionada SHA-256 com timestamp ISO-8601; verificar digest, valores persistidos e conflitos de unicidade com testes de serviço.
- [x] 3.4 Implementar gravação opcional de observações do odontograma pela chave existente e conclusão atômica da consulta com `app.origem='ALUNO'` e `app.usuario_id`; verificar rollback, `REALIZADO` e autoria no `consulta_historico` com testes de integração PostgreSQL.
- [x] 3.5 Implementar `POST /api/prontuarios/{prontuarioId}/adendos` somente por INSERT e autor autenticado; verificar respostas para texto vazio/prontuário inexistente e imutabilidade original/adendo com testes de integração.

## 4. Frontend: serviços e fluxo contextual do Aluno

- [x] 4.1 Criar serviços Axios/API para carregar histórico, salvar evolução e inserir adendo, preservando status e mensagem HTTP retornados; verificar serialização dos contratos e tratamento de 403/409 em testes do serviço.
- [x] 4.2 Integrar a lista `GET /api/retornos/meus-atendimentos` ao fluxo do Painel do Aluno e oferecer ação de abrir prontuário por consulta; verificar estados de carregamento, vazio e seleção da consulta em testes de componente.
- [x] 4.3 Implementar drawer/modal amplo com resumo de paciente e atendimento, abas de evolução e histórico, formulário, controle de conclusão, histórico somente leitura e selo após confirmação; verificar conteúdo e transições das abas em testes de componente.
- [x] 4.4 Implementar modal compacto de adendo e mini-modal vermelho para bloqueios de rede/vínculo/turno sem renderizar resposta clínica negada; verificar sucesso, cancelamento e mensagens de bloqueio em testes de componente.

## 5. Frontend: fidelidade visual e proteção LGPD

- [x] 5.1 Aplicar Lato, tokens e hierarquia do frame Figma `4:46292` em CSS modular, adaptando a composição de tela ampla ao drawer contextual e dimensionando modais compactos responsivamente; verificar layout em larguras desktop e estreitas por revisão visual.
- [x] 5.2 Implementar marca d’água diagonal repetida com nome, matrícula disponível e data/hora, `pointer-events: none`, `user-select: none` na área clínica e ocultação em `@media print`; verificar interação dos controles, atualização do timestamp e CSS de impressão.

## 6. Integração

- [x] 6.1 Executar a suíte de backend e frontend e validar os fluxos completos de histórico, bloqueio de rede/turno, assinatura, conclusão e adendo contra PostgreSQL com schema existente; registrar resultados e corrigir incompatibilidades sem alterar V6/V8/V9.
- [x] 6.2 Revisar o fluxo com perfil `COORDENADOR` de auditoria, confirmar que é somente leitura e registrar no material de API/UI que o hash SHA-256 é selo temporal de integridade, não assinatura qualificada.
