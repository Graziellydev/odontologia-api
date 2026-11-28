# Tasks

## 1. Migration e validação local

- [x] 1.1 Gerar hashes com o `BCryptPasswordEncoder` da aplicação para as três senhas de teste e criar `V19__seed_usuarios_iniciais.sql` com as 13 identidades e `ON CONFLICT DO NOTHING`; conferir colunas, perfis e tamanhos dos valores contra V1/V2.
- [x] 1.2 Criar ou complementar testes para confirmar as 13 combinações email/matrícula, as contagens 2/10/1, `ativo=true`, correspondência das senhas via `PasswordEncoder.matches` e comportamento sem sobrescrita em conflito; executar os testes do backend.

## 2. Documentação de acesso

- [x] 2.1 Criar `usuarios_sistema.md` na raiz com a tabela dos 13 usuários, perfil, nome, e-mail, matrícula e senha, instrução de login e aviso de uso exclusivo para teste; verificar que cada linha corresponde à especificação e à migration.

## 3. Aplicação e verificação no Supabase

- [x] 3.1 Verificar a conexão e validar o histórico Flyway do Supabase; carregar DB_URL, DB_USERNAME e DB_PASSWORD apenas em variáveis do processo, sem exibir valores, gravar credenciais ou alterar `.env`.
- [x] 3.2 Aplicar V19 no Supabase sem executar `clean`; consultar os 13 registros por e-mail/matrícula e conferir exatamente dois SECRETARIA, dez ALUNO e um COORDENADOR, com todos os hashes BCrypt correspondentes às senhas definidas.
- [x] 3.3 Reexecutar a validação/aplicação idempotente e confirmar que a quantidade de registros e os dados existentes permanecem iguais; registrar comando e resultado sem credenciais.
