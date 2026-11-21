# Tasks

## 1. Persistência Flyway e mapeamento JPA

- [ ] 1.1 Criar `V16__create_password_reset_tokens.sql` com os campos especificados, FK para `usuarios(id)`, contador de tentativas e índices; verificar migration aplicada a partir de V15 e compatibilidade de `ddl-auto=validate` em PostgreSQL.
- [ ] 1.2 Implementar entidade e repositório de tokens com consulta/bloqueio transacional, substituição de tokens anteriores e incremento de falhas; verificar constraints, expiração, estado usado e concorrência em testes de persistência.

## 2. Autenticação institucional no back-end

- [ ] 2.1 Atualizar DTO e serviço de login para aceitar e-mail ou matrícula, manter compatibilidade temporária com o campo `email`, normalizar identificadores e rejeitar conta inativa ou perfil fora da allowlist; verificar casos elegíveis, inválidos, ambíguos e mensagem indistinguível em testes de serviço.
- [ ] 2.2 Manter criação de sessão e proteção CSRF existentes, atualizar feedback genérico para `Login ou senha inválidos` e documentar o contrato do identificador; verificar testes MVC para sessão, ausência de sessão negada e campos legados/novos.

## 3. API e serviço de recuperação

- [ ] 3.1 Implementar emissão com CSPRNG de seis dígitos como texto, BCrypt verifier, validade de 15 minutos, invalidação de códigos anteriores e resposta HTTP 200 neutra; verificar usuário encontrado, inexistente, inativo, perfil não permitido e colisão de identificador em testes MVC.
- [ ] 3.2 Adicionar envio opcional com `JavaMailSender`, dependência `spring-boot-starter-mail` e configuração por ambiente, além de log estruturado somente em desenvolvimento; verificar entrega com SMTP de teste e ausência do código em respostas/logs de produção.
- [ ] 3.3 Implementar redefinição transacional por identificador/código/senha, BCrypt, consumo único, concorrência, máximo de cinco falhas e rate limit configurável por origem; verificar códigos válidos, errados, expirados, usados, bloqueados e sucesso em testes de serviço/controlador.
- [ ] 3.4 Documentar endpoints `/api/auth/esqueci-senha` e `/api/auth/redefinir-senha`, payloads, respostas neutras, CSRF, configuração de e-mail e política de senha; verificar a documentação contra os contratos dos controllers.

## 4. Refatoração visual e fluxo de login

- [ ] 4.1 Refatorar `Login.jsx` e CSS para reproduzir `4:45799`, carregar o asset institucional do Figma (ou substituto aprovado conforme design), aceitar e-mail/matrícula e exibir somente os três perfis de referência; verificar hierarquia, texto e navegação responsiva em testes de componente e revisão visual.
- [ ] 4.2 Ajustar `index.css` e estilos da tela para Lato, `color-scheme: light`, tokens do design system, foco acessível e remoção de limites globais Vite conflitantes; verificar em tema escuro e claro que campos, botões, alertas e foco permanecem legíveis.
- [ ] 4.3 Tratar carregamento, falha genérica e retorno ao painel conforme perfil real autenticado; verificar que a aba de referência não altera a autoridade, perfil fora da allowlist não entra e tentativa de área cruzada apresenta bloqueio em testes de componente/integração.

## 5. Interface de recuperação

- [ ] 5.1 Criar serviço de autenticação para solicitar e redefinir senha por `api.js`, preservando CSRF e status de erro; verificar métodos, payloads, respostas e cabeçalhos em testes do serviço.
- [ ] 5.2 Implementar a tela de solicitação fiel a `35:1616` com identificador e estados de carregamento, erro e sucesso neutro; verificar retorno ao login, acessibilidade e ausência de indicação de conta existente em testes de componente.
- [ ] 5.3 Implementar etapa de código e nova senha/confirmar senha com mínimo de 12 caracteres e confirmação idêntica; verificar validação local sem chamada à API e renderização de sucesso/erro do servidor em testes de componente.

## 6. Verificação integrada

- [ ] 6.1 Executar a suíte de backend, a suíte de frontend, build e validação OpenSpec; verificar no ambiente PostgreSQL os fluxos completos de login, emissão neutra, entrega, expiração, tentativas, redefinição e uso único.
- [ ] 6.2 Validar visualmente os dois frames nas larguras desktop e estreita, revisar acessibilidade de foco/feedback, perfis e configuração SMTP de desenvolvimento/produção; registrar os resultados e corrigir regressões antes de concluir.
