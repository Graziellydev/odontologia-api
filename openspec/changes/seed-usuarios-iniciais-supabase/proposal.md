# Proposal

## Why

O Supabase precisa conter um conjunto previsível de contas iniciais para validar os fluxos de autenticação dos três perfis do Odonto_Uni. A migration atual V10 não cria usuários e a tabela `usuarios` exige hashes BCrypt, além de impor unicidade para e-mail e matrícula.

## What Changes

- Adicionar a migration sequencial `V19__seed_usuarios_iniciais.sql`, inserindo as 13 contas informadas com hashes BCrypt compatíveis com `BCryptPasswordEncoder` e `ON CONFLICT DO NOTHING`.
- Aplicar V19 diretamente ao Supabase usando credenciais carregadas somente na memória, sem escrever valores no `.env`; verificar os 13 registros, os perfis e a correspondência das senhas com BCrypt.
- Criar `usuarios_sistema.md` na raiz com a tabela dos 13 usuários de teste, seus dados de acesso e instruções breves de login.

## Capabilities

### New Capabilities

- `usuarios-iniciais-supabase`: provisionamento repetível e validação das contas iniciais por perfil.
- `credenciais-usuarios-teste`: documentação dos usuários e credenciais de teste para acesso manual.

### Modified Capabilities

- Nenhuma. Não há especificações principais existentes.

## Impact

Afeta `backend/src/main/resources/db/migration/`, testes de integração/autenticação, a tabela Supabase `usuarios` e o novo documento `usuarios_sistema.md` na raiz. A migration usa apenas os campos existentes (`nome`, `email`, `senha_hash`, `perfil`, `matricula_ou_registro`); semestre permanece nulo e `ativo`/timestamps usam defaults do banco.
