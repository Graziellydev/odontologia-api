# Spec Delta

## Purpose

Permite que integrantes ativos dos perfis institucionais recuperem acesso por um código temporário sem expor a existência de contas e sem armazenar ou transmitir senhas em texto claro.

## ADDED Requirements

### Requirement: Solicitação de recuperação não revela a existência da conta
O sistema SHALL aceitar e-mail institucional ou matrícula, emitir código somente para usuário ativo dos perfis permitidos e retornar HTTP 200 com mensagem neutra, independentemente de localizar uma conta elegível.

#### Scenario: Identificador corresponde a conta elegível
- **GIVEN** o identificador corresponde a uma conta ativa `ALUNO`, `SECRETARIA` ou `COORDENADOR`
- **WHEN** a pessoa solicita instruções de recuperação
- **THEN** o sistema invalida códigos anteriores ainda não utilizados, gera código aleatório de seis dígitos válido por 15 minutos, persiste somente seu verificador e inicia a entrega sem revelar o código na resposta HTTP

#### Scenario: Identificador não corresponde a conta elegível
- **GIVEN** o identificador não corresponde a conta, corresponde a conta inativa ou a perfil não permitido
- **WHEN** a pessoa solicita instruções de recuperação
- **THEN** o sistema responde HTTP 200 com a mesma mensagem neutra e não cria token recuperável

#### Scenario: SMTP indisponível ou não configurado
- **GIVEN** a conta é elegível e SMTP está indisponível ou não configurado
- **WHEN** o sistema processa a solicitação
- **THEN** a resposta pública continua neutra e o código é registrado apenas em log estruturado nos ambientes de desenvolvimento, sem ser registrado em produção

### Requirement: Código temporário é armazenado e limitado com segurança
O sistema SHALL armazenar o token associado ao usuário com validade, estado de uso e verificador não reversível, permitindo no máximo cinco validações incorretas antes de invalidá-lo.

#### Scenario: Migration de persistência é aplicada
- **GIVEN** o banco possui a versão V15 aplicada
- **WHEN** a migration V16 é executada
- **THEN** é criada `password_reset_tokens` com `id`, `usuario_id` como FK para `usuarios(id)`, `codigo_hash`, `expira_em`, `usado`, `criado_em` e contador persistente de tentativas incorretas; o mapeamento é compatível com `ddl-auto=validate`

#### Scenario: Código anterior é substituído
- **GIVEN** o usuário possui um ou mais códigos não utilizados
- **WHEN** solicita nova recuperação
- **THEN** códigos anteriores são invalidados e somente o código mais recente pode redefinir a senha

#### Scenario: Muitas tentativas incorretas
- **GIVEN** um código já acumulou cinco validações incorretas
- **WHEN** outra validação é enviada
- **THEN** o código é inválido e não pode ser usado para alterar a senha

### Requirement: Solicitações públicas são limitadas contra abuso
O sistema SHALL limitar a frequência das solicitações de emissão e validação de códigos por origem, sem variar a resposta de recuperação conforme a existência da conta.

#### Scenario: Limite de frequência excedido
- **GIVEN** a origem excedeu o limite configurado para o endpoint público
- **WHEN** envia nova solicitação de recuperação ou validação
- **THEN** a operação é limitada antes de processar credenciais e não revela se qualquer identificador corresponde a uma conta

### Requirement: Redefinição exige código vigente e identificador vinculado
O sistema SHALL validar o identificador, o código, a expiração, o estado de uso e o limite de tentativas antes de atualizar a senha do usuário correspondente.

#### Scenario: Código vigente e correto
- **GIVEN** o identificador aponta para o usuário dono do código, o código está correto, não expirou e não foi usado
- **WHEN** a pessoa solicita redefinição com uma nova senha válida
- **THEN** o sistema atualiza `usuarios.senha_hash` usando BCrypt e marca o token consumido na mesma transação

#### Scenario: Código inválido, expirado, usado ou vinculado a outra conta
- **GIVEN** pelo menos uma condição de validade do token falha
- **WHEN** a pessoa solicita redefinição
- **THEN** o sistema responde HTTP 400 com mensagem clara de código inválido ou expirado, preserva a senha e incrementa a tentativa quando aplicável

#### Scenario: Código de seis dígitos mantém zeros à esquerda
- **GIVEN** o código aleatório começa com zero
- **WHEN** é enviado e validado
- **THEN** é tratado como texto de seis caracteres, sem conversão numérica que remova zeros

### Requirement: Interface de recuperação segue o frame institucional e conclui a redefinição
A interface SHALL abrir pelo link `Esqueci minha senha?`, reproduzir o frame Figma `35:1616` na solicitação e oferecer etapa subsequente para código, nova senha e confirmação, com retorno ao login.

#### Scenario: Formulário inicial de recuperação
- **GIVEN** a pessoa seleciona `Esqueci minha senha?` no login
- **WHEN** a tela de recuperação é exibida
- **THEN** preserva a apresentação clínica à esquerda e mostra à direita `Recupere seu acesso`, explicação para Aluno/Secretaria/Coordenador, campo `E-mail usado` que aceita e-mail ou matrícula, botão `Enviar instruções`, retorno `Voltar para acessos` e aviso `Acesso exclusivo!` conforme o frame

#### Scenario: Solicitação enviada
- **GIVEN** o formulário contém identificador obrigatório válido no formato aceito
- **WHEN** a pessoa envia a solicitação
- **THEN** a ação indica carregamento sem permitir envio duplicado e, ao concluir, exibe mensagem neutra de instruções enviadas sem confirmar existência da conta

#### Scenario: Erro de validação no formulário
- **GIVEN** o identificador está ausente ou inválido
- **WHEN** a pessoa tenta enviar
- **THEN** a interface informa o campo inválido junto ao controle e não chama a API

#### Scenario: Confirmação e redefinição de senha
- **GIVEN** a pessoa recebeu instruções e está na etapa de redefinição
- **WHEN** informa identificador, código, nova senha e confirmação
- **THEN** o cliente exige todos os campos, senha com pelo menos 12 caracteres e igualdade entre senha e confirmação antes de chamar a API

#### Scenario: Redefinição concluída ou rejeitada
- **GIVEN** o servidor confirmou a alteração ou rejeitou o código
- **WHEN** a resposta é apresentada
- **THEN** sucesso informa que a senha foi redefinida e oferece retorno ao login; erro mostra mensagem clara, mantém o contexto e não expõe detalhes internos

#### Scenario: Pessoa retorna ao login
- **GIVEN** a pessoa está em qualquer etapa da recuperação
- **WHEN** seleciona `Voltar para acessos`
- **THEN** retorna ao login sem criar sessão e sem expor dados submetidos na tela
