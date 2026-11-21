# Autenticação e recuperação de senha

## Login

`POST /api/auth/login` recebe `identificador` (e-mail institucional ou matrícula) e `senha`. O campo antigo `email` continua aceito durante a transição. Uma autenticação recusada responde HTTP 401 com `Login ou senha inválidos`, sem indicar qual dado falhou. O servidor cria uma sessão HTTP; chamadas mutáveis do navegador devem enviar o cookie de sessão e o token CSRF obtido em `GET /api/auth/csrf` no header `X-XSRF-TOKEN`. Os únicos perfis aceitos são `ALUNO`, `SECRETARIA` e `COORDENADOR`.

## Solicitar recuperação

`POST /api/auth/esqueci-senha` recebe `{ "identificador": "email ou matrícula" }`. Para qualquer identificador bem-formado, responde HTTP 200 com a mensagem neutra `Se os dados corresponderem a uma conta elegível, enviaremos instruções para o e-mail cadastrado.` O serviço gera um código decimal de seis dígitos, guarda somente seu verifier BCrypt e expira-o após 15 minutos. Um pedido novo invalida os códigos anteriores ainda abertos. Chamadas POST precisam de CSRF. O envio não revela existência de conta.

## Redefinir senha

`POST /api/auth/redefinir-senha` recebe `{ "identificador": "email ou matrícula", "codigo": "123456", "novaSenha": "ao menos 12 caracteres" }`. A senha tem mínimo de 12 caracteres e máximo de 72 bytes UTF-8 (limite do BCrypt). Sucesso retorna HTTP 200; código desconhecido, expirado, consumido ou bloqueado retorna HTTP 400 `Código inválido ou expirado.` O código é de uso único; cinco erros consomem o token. O limite por origem e identificador pode ser ajustado com `APP_AUTH_RECOVERY_RATE_LIMIT_MAX` e `APP_AUTH_RECOVERY_RATE_LIMIT_WINDOW_SECONDS`.

## E-mail e desenvolvimento

Configure `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` e `SPRING_MAIL_FROM` no ambiente para habilitar SMTP. Sem SMTP, o padrão não registra o código. Para desenvolvimento local, `AUTH_RECOVERY_LOG_CODE=true` habilita um evento estruturado com o código somente quando o perfil ativo é `local`, `dev` ou `test`; não habilite isso em produção. Falhas de entrega não alteram a resposta neutra.
