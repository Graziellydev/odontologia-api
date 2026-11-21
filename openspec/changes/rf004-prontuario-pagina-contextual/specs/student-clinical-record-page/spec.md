# Spec Delta

## Purpose

Apresenta o prontuário como página clínica contextual do aluno, fiel ao frame oficial, com estados de segurança e redução visual de exposição de dados.

## ADDED Requirements

### Requirement: Prontuário é uma página contextual do atendimento
O sistema SHALL abrir `Prontuário e evolução` a partir de `Pacientes do turno` e SHALL oferecer `← Voltar ao turno`. A página SHALL preservar a identidade visual institucional, cabeçalho de vínculo, faixa de ambiente e resumo do paciente e atendimento.

#### Scenario: Abertura por paciente do turno
- **WHEN** o aluno selecionar abrir prontuário para atendimento disponível
- **THEN** a interface SHALL apresentar a página contextual vinculada àquela consulta

#### Scenario: Retorno à lista do turno
- **WHEN** o aluno acionar `Voltar ao turno`
- **THEN** a interface SHALL retornar ao contexto de pacientes do turno sem perder a sessão autenticada

### Requirement: Página reproduz a composição clínica do frame
O sistema SHALL usar Lato, fundo `#F7F8FA`, painéis `#FFFFFF`, borda `#E3E8EB`, primária `#C65117`, verde `#287252`/`#EAF5EE`, alerta `#FFF1E7` e os cards, abas e hierarquia do frame `4:46292`.

#### Scenario: Evolução clínica em tela ampla
- **WHEN** a aba `Evolução clínica` estiver ativa em viewport amplo
- **THEN** a página SHALL mostrar histórico cronológico à esquerda e registro atual/estado assinado à direita, seguido do encerramento

#### Scenario: Histórico de procedimentos
- **WHEN** o usuário selecionar `Histórico de procedimentos`
- **THEN** a página SHALL trocar o conteúdo interno sem abandonar o prontuário atual

### Requirement: Formulário e estado assinado possuem hierarquia explícita
O sistema SHALL apresentar campos de procedimento, elemento ou região, evolução, data/hora e aluno responsável, ação `Salvar evolução` e aviso de assinatura. Após salvar, SHALL exibir o card imutável, selo verde e ação discreta `+ Adicionar adendo`.

#### Scenario: Salvar evolução
- **WHEN** o aluno salvar evolução válida
- **THEN** a página SHALL exibir o estado assinado sem oferecer edição do conteúdo original

#### Scenario: Adicionar adendo
- **WHEN** o aluno acionar `+ Adicionar adendo`
- **THEN** a interface SHALL coletar somente o texto complementar e atualizar o histórico após sucesso

### Requirement: Bloqueios clínicos são claros e não exibem conteúdo negado
O sistema SHALL representar os cenários `Ver bloqueio de rede` e `Ver bloqueio fora do turno` como estados demonstrativos da faixa de ambiente. Quando a API devolver HTTP 403, SHALL mostrar alerta de restrição e não renderizar os dados clínicos bloqueados.

#### Scenario: Bloqueio de rede
- **WHEN** a API responder o bloqueio de rede local
- **THEN** a interface SHALL informar a restrição de rede em estado de alerta

#### Scenario: Bloqueio de vínculo ou turno
- **WHEN** a API responder 403 por vínculo ou turno
- **THEN** a interface SHALL informar a restrição aplicável e preservar a área clínica vazia

### Requirement: Página aplica dissuasão visual LGPD
O sistema SHALL renderizar marca d'água diagonal repetida com nome do aluno, matrícula e data/hora atual sobre a área clínica; SHALL aplicar `pointer-events: none`, impedir seleção de conteúdo clínico e ocultar a página clínica na impressão.

#### Scenario: Área clínica exibida
- **WHEN** o prontuário estiver aberto
- **THEN** a marca d'água SHALL permanecer visível sem bloquear controles ou campos editáveis

#### Scenario: Impressão
- **WHEN** o navegador entrar em modo de impressão
- **THEN** a área de prontuário e sua marca d'água SHALL ser ocultadas
