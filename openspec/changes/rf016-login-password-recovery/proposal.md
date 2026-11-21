# Proposal

## Why

O login atual não reproduz o acesso institucional aprovado no Figma e não oferece recuperação de senha, embora RF016/HU23 exija acesso exclusivo para aluno, secretaria e coordenador. Uma mudança única alinha a experiência de entrada e entrega recuperação integrada ao modelo real de sessão, usuários e banco, sem revelar se uma conta existe.

## What Changes

- Refatorar a tela de login para refletir o frame Figma `4:45799`, incluindo apresentação institucional, acesso, aviso genérico e permissões dos três perfis; aceitar e-mail institucional ou matrícula como identificador de login.
- Tornar a autenticação no servidor exclusiva para `ALUNO`, `SECRETARIA` e `COORDENADOR`; a seleção visual de perfil do Figma será apenas referência e nunca autoridade para conceder acesso.
- Adicionar recuperação por e-mail institucional ou matrícula, emissão de código numérico de seis dígitos com validade de 15 minutos, armazenamento apenas de verificador e resposta pública neutra; limitar tentativas incorretas por código.
- Adicionar redefinição de senha com código válido, confirmação no cliente, BCrypt no armazenamento e invalidação após uso/expiração.
- Criar a migration Flyway V16 para `password_reset_tokens`, preservando compatibilidade com `usuarios` e `ddl-auto=validate`.
- Implementar a interface de recuperação baseada no frame `35:1616`, com estados acessíveis de envio, erro e sucesso e retorno ao login.

## Capabilities

### New Capabilities

- `institutional-login`: autenticação institucional e apresentação do login restritas aos três perfis suportados.
- `password-recovery`: solicitação, entrega, validação e consumo seguro de códigos de redefinição de senha.

### Modified Capabilities

Nenhuma. A listagem OpenSpec atual não contém especificações publicadas; esta mudança cria as duas capabilities relacionadas sem duplicar propostas existentes.

## Impact

- Front-end: `frontend/src/Login/Login.jsx`, CSS de login, `App.jsx`, estilos globais e serviço de autenticação; integração do link “Esqueci minha senha?” e do fluxo de redefinição.
- Back-end: `AutenticacaoController`, `AutenticacaoService`, nova persistência/serviço de tokens, `SecurityConfig`, configuração de e-mail e `PasswordEncoder` existente.
- Banco: migration `V16__create_password_reset_tokens.sql`; a migration V2 define `usuarios` com `id`, `email`, `senha_hash`, `perfil`, `matricula_ou_registro` e `ativo`, entre outras colunas. O enum/check atual admite somente `ALUNO`, `COORDENADOR` e `SECRETARIA`.
- Design: Figma file `K835WNYGJT3DHT3yauWRfr`, frames `4:45799` e `35:1616`. O contexto OpenSpec local descreve atores e versões de migrations divergentes do código presente; implementação e migration V15 são a referência para esta proposta.
- E-mail: o repositório não contém `spring-boot-starter-mail` nem configuração de `JavaMailSender`; adicionar suporte e usar envio quando SMTP estiver configurado. Em desenvolvimento, registrar código apenas em log estruturado do servidor, sem incluí-lo em resposta HTTP.
