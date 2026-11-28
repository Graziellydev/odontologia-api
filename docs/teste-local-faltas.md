# Testar Gestão de Faltas localmente

## Requisitos

- JDK 25 (o projeto já declara Java 25).
- Node.js e npm para a interface.
- PostgreSQL local. O perfil `local` usa por padrão `localhost:5432`, banco `odontologia_local`, usuário `postgres` e senha `postgres`.
- Maven não precisa ser instalado: o projeto inclui Maven Wrapper em `mvnw.cmd`.

## Preparar e iniciar

1. Crie no PostgreSQL um banco chamado `odontologia_local`.
2. Na raiz do projeto, inicie o backend com:

   ```powershell
   cd backend
   .\mvnw.cmd "-Dspring-boot.run.profiles=local" spring-boot:run
   ```

   O Flyway executa as migrações no banco local, incluindo a correção da restrição de justificativa para faltas de aluno.
3. Em outro terminal, ainda na raiz, carregue os dados de demonstração:

   ```powershell
   psql -h localhost -U postgres -d odontologia_local -f backend/src/test/resources/sql/seed-gestao-faltas-local.sql
   ```

   Se o usuário ou senha local forem diferentes, defina `DB_USERNAME` e `DB_PASSWORD` antes de iniciar o backend e use o usuário correspondente no `psql`.
4. Em outro terminal, inicie a interface:

   ```powershell
   cd frontend
   npm.cmd install
   npm.cmd run dev
   ```

5. Entre com `secretaria.faltas@local.test` e senha `teste123`, abra **Agenda** no menu da Secretaria e use os IDs mostrados pelo script SQL.

## Fluxo que deve ser possível verificar

1. O histórico lista consultas com status `FALTOU_PACIENTE` ou `FALTOU_ALUNO`.
2. Informe o ID da consulta de demonstração, escolha quem faltou e registre a ausência. Para marcar como justificada, marque a opção e preencha a observação.
3. A consulta original passa para o status de falta e aparece no histórico. O trigger do banco registra a mudança em `consulta_historico`.
4. Selecione a linha no histórico e informe o ID da vaga livre mostrado pelo seed. O sistema cria uma nova consulta `AGENDADO`, ligada à original por `consulta_origem_id`, sem remover a original.
5. A opção `notificarWhatsapp` é recebida pela API; o envio da mensagem ainda depende de uma integração de WhatsApp, que não está definida no SDD desta tarefa.

O perfil `local` sobrescreve a conexão remota de `application.properties`. Não execute o seed de demonstração em banco compartilhado ou de produção.
