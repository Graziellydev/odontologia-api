# Spec Delta

## Purpose

Integra a leitura e o registro de prontuário ao fluxo contextual do Painel do Aluno e reduz a exposição casual de dados clínicos na tela e em impressão.

## ADDED Requirements

### Requirement: Prontuário abre no contexto do Painel do Aluno
A interface SHALL abrir o prontuário a partir do contexto do paciente/consulta do turno em um drawer ou modal amplo, sem exigir navegação para uma página isolada. A tela SHALL apresentar a identificação do paciente e atendimento, abas `Evolução clínica` e `Histórico de procedimentos`, histórico anterior somente para leitura, formulário de evolução, controle para concluir atendimento e acesso a adendos após salvar.

#### Scenario: Aluno abre prontuário a partir do paciente do turno
- **GIVEN** o Painel do Aluno exibe um paciente associado a consulta do turno
- **WHEN** o aluno seleciona abrir o prontuário
- **THEN** a interface abre o drawer/modal amplo contextual com identificação, consulta e abas de evolução e histórico

#### Scenario: Aluno alterna abas
- **GIVEN** o prontuário está aberto
- **WHEN** seleciona `Histórico de procedimentos` ou `Evolução clínica`
- **THEN** o conteúdo correspondente é exibido e o histórico permanece somente para leitura

#### Scenario: Evolução foi salva
- **GIVEN** o servidor confirmou a criação da evolução
- **WHEN** a interface apresenta o registro salvo
- **THEN** exibe o selo `Assinatura Digital Temporal Não Editável` e os dados de autoria e data retornados pelo servidor

### Requirement: Adendo abre em modal compacto
A interface SHALL permitir adicionar texto complementar por meio de um modal compacto `+ Adicionar Adendo`, vinculado ao prontuário já assinado. O formulário SHALL deixar claro que o adendo não altera o texto original e, após sucesso, SHALL exibi-lo no histórico associado ao registro.

#### Scenario: Usuário abre o formulário de adendo
- **GIVEN** há uma evolução assinada exibida
- **WHEN** seleciona `+ Adicionar Adendo`
- **THEN** abre um modal compacto com campo de texto, ação de salvar e opção de cancelar

#### Scenario: Adendo foi salvo
- **GIVEN** o texto do adendo foi aceito pelo servidor
- **WHEN** o modal é concluído
- **THEN** o novo adendo aparece associado à evolução original e a assinatura original permanece visível e inalterada

### Requirement: Bloqueio de rede ou turno é informado visualmente
Ao receber HTTP 403 por bloqueio de rede ou vínculo/turno, a interface SHALL exibir um mini-modal compacto com tratamento visual vermelho, descrição clara do motivo recebido e ação para fechar. O estado SHALL preservar o contexto do painel sem expor dados clínicos não autorizados.

#### Scenario: Bloqueio de rede
- **GIVEN** uma operação clínica foi negada pelo controle de rede
- **WHEN** a interface recebe HTTP 403 com a mensagem de bloqueio de rede
- **THEN** exibe o mini-modal vermelho compacto com a mensagem `Acesso restrito a dispositivos autorizados na rede local`

#### Scenario: Bloqueio de vínculo ou turno
- **GIVEN** uma operação clínica foi negada pelo controle de vínculo/turno
- **WHEN** a interface recebe HTTP 403 com a mensagem de restrição
- **THEN** exibe mini-modal compacto com o motivo e não apresenta conteúdo clínico da resposta negada

### Requirement: Área clínica contém marca d’água de sessão
A área clínica SHALL exibir marca d’água repetida, diagonal, semitransparente e com `pointer-events: none`, contendo nome do aluno, matrícula (ou CPF somente quando fornecido por identidade disponível) e data/hora atuais. A marca d’água não SHALL impedir interação com campos e controles clínicos.

#### Scenario: Prontuário está visível
- **GIVEN** a área clínica está aberta
- **WHEN** o conteúdo é renderizado
- **THEN** uma marca d’água diagonal repetida identifica a sessão do aluno e o instante atual sem interceptar eventos de ponteiro

### Requirement: Proteção de seleção e impressão
A interface SHALL aplicar `user-select: none` à área clínica e ocultar o conteúdo do prontuário e a marca d’água no modo de impressão usando regra equivalente a `@media print { display: none; }`.

#### Scenario: Usuário tenta selecionar conteúdo clínico
- **GIVEN** o prontuário está aberto na tela
- **WHEN** o usuário tenta selecionar texto clínico
- **THEN** a área clínica não permite seleção de texto

#### Scenario: Navegador prepara impressão
- **GIVEN** há prontuário aberto
- **WHEN** o navegador renderiza a página para impressão
- **THEN** o conteúdo clínico e a marca d’água são ocultados
