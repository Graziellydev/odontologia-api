# Spec Delta

## Purpose

Define a entrada institucional do Odonto_Uni para os perfis autorizados e uma apresentação consistente do login, sem permitir que escolhas visuais substituam a identidade e as permissões da conta autenticada.

## ADDED Requirements

### Requirement: Acesso institucional restrito aos perfis autorizados
O sistema SHALL autenticar somente contas ativas dos perfis `ALUNO`, `SECRETARIA` e `COORDENADOR`, validando as credenciais e retornando a identidade e o perfil reais da conta.

#### Scenario: Conta ativa de perfil autorizado
- **GIVEN** uma conta ativa possui perfil `ALUNO`, `SECRETARIA` ou `COORDENADOR` e credenciais válidas
- **WHEN** a pessoa envia o formulário de login
- **THEN** o sistema cria a sessão autenticada e informa o perfil real da conta

#### Scenario: Conta inativa, perfil não permitido ou credenciais inválidas
- **GIVEN** a conta está inativa, possui perfil diferente dos três permitidos ou as credenciais não correspondem
- **WHEN** a pessoa tenta entrar
- **THEN** o sistema nega a autenticação sem criar sessão e apresenta somente a mensagem `Login ou senha inválidos`

### Requirement: Perfil exibido no login não concede autoridade
A tela SHALL apresentar somente Aluno, Coordenador e Secretaria como perfis de referência, informando que a seleção é ilustrativa e que o acesso é definido pelo vínculo da conta. A autenticação SHALL usar o perfil retornado pelo servidor, independentemente da seleção visual.

#### Scenario: Pessoa seleciona perfil de referência diferente do cadastro
- **GIVEN** a pessoa selecionou um perfil de referência diferente do perfil autorizado da conta
- **WHEN** envia credenciais válidas
- **THEN** o sistema autentica conforme o perfil real e encaminha à área correspondente a esse perfil

#### Scenario: Pessoa tenta acessar área de outro perfil
- **GIVEN** uma sessão autenticada tenta abrir uma área ou operação pertencente a outro perfil
- **WHEN** a aplicação ou API verifica a autorização
- **THEN** o acesso é negado, a sessão permanece vinculada ao perfil real e a interface informa que aquela área não está disponível para a conta

### Requirement: Tela de login segue o frame institucional
A tela de login SHALL reproduzir a hierarquia do frame Figma `4:45799`, com apresentação institucional à esquerda e formulário à direita, incluindo textos, abas de referência, campos, ação, estado de erro e bloco de permissões.

#### Scenario: Login renderizado em viewport amplo
- **GIVEN** a pessoa abre o acesso institucional em viewport amplo
- **WHEN** a tela é renderizada
- **THEN** apresenta marca e descrição da clínica, título e texto institucional, imagem de formação e cuidado, dois destaques e rodapé à esquerda; à direita apresenta identificação do portal, boas-vindas, abas de referência, campos de e-mail/matrícula e senha, recuperação, ação de entrar e permissões

#### Scenario: Login em viewport estreito
- **GIVEN** a pessoa abre o acesso em viewport estreito
- **WHEN** a tela é renderizada
- **THEN** os painéis se reorganizam sem sobreposição, corte dos campos ou perda de acesso à ação de login e à recuperação

### Requirement: Aparência do login não depende do tema do navegador
A tela SHALL manter `color-scheme: light` e usar Lato, fundo `#F7F8FA`, superfícies `#FFFFFF`, bordas `#E3E8EB` e primária `#C65117`, com controles, textos, bordas e foco legíveis em modo claro.

#### Scenario: Navegador configurado para tema escuro
- **GIVEN** o navegador ou sistema operacional prefere tema escuro
- **WHEN** a tela de login é exibida
- **THEN** a interface permanece em modo claro com os tokens definidos e sem textos invisíveis, bordas pretas ou contornos de foco escuros herdados

### Requirement: Submissão e falhas de autenticação têm feedback acessível
A interface SHALL indicar envio em andamento e bloquear submissões duplicadas. Falhas de credencial SHALL exibir mensagem genérica sem identificar se e-mail/matrícula ou senha está incorreto.

#### Scenario: Login em andamento
- **GIVEN** a pessoa enviou credenciais
- **WHEN** o servidor ainda não respondeu
- **THEN** o botão indica carregamento e não permite uma segunda submissão

#### Scenario: Login rejeitado
- **GIVEN** o servidor rejeitou a autenticação
- **WHEN** a resposta de falha chega à interface
- **THEN** a tela mostra `Login ou senha inválidos` e não distingue conta inexistente, perfil não permitido, conta inativa ou senha incorreta

#### Scenario: Perfil impede acesso a outra área
- **GIVEN** uma pessoa autenticada tenta acessar uma área protegida por outro perfil
- **WHEN** a tentativa é negada pelo sistema
- **THEN** a interface apresenta feedback de bloqueio de perfil sem expor detalhes da conta nem alterar suas permissões
