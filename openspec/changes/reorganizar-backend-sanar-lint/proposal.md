# Proposal

## Why

O repositório mantém o Maven/Spring na raiz ao lado do frontend, o que dificulta delimitar e executar cada aplicação, enquanto `npm run lint` falha com 9 erros e 1 warning. Também permanecem tarefas de arquitetura JPA, migrations e segurança que afetam consistência dos dados e controle de acesso.

## What Changes

- **BREAKING** Mover o projeto Maven completo para `backend/`, preservando o histórico Git e mantendo `frontend/`, `openspec/` e `.env` na raiz; corrigir caminhos relativos do wrapper, build, ignore e configuração Spring.
- Corrigir todas as falhas e warnings do ESLint em `frontend/src/` e `frontend/tests/`, sem alterar comportamento funcional ou design.
- Configurar janelas clínicas do prontuário por propriedades, completar o mapeamento/contratos da ficha física de paciente e unificar o mapeamento versionado da tabela `consultas`.
- Preservar checksums das migrations V2–V9, desativar baseline automático e criar V19 somente se houver diferença estrutural confirmada.
- Padronizar authorities e proteger retornos, metas, catálogo de solicitações e fila de espera nas camadas de rota e método.

## Capabilities

### New Capabilities

- `backend-layout`: execução e configuração do serviço Maven a partir da pasta backend com recursos compartilhados na raiz.
- `frontend-lint-quality`: qualidade estática sem erros ou warnings, preservando a interface e seu comportamento.
- `prontuario-horarios-configuraveis`: janelas de acesso clínico ajustáveis por configuração.
- `cadastro-paciente-ficha-fisica`: persistência e retorno de UF e dados completos da ficha física.
- `consultas-mapeamento-unificado`: atualizações versionadas consistentes da tabela de consultas.
- `integridade-migrations-flyway`: checksums históricos estáveis e evolução sequencial do schema.
- `autorizacao-perfis-api`: autoridades padronizadas e acesso declarativo aos endpoints.

### Modified Capabilities

- Nenhuma. Não há specs principais existentes.

## Impact

Raiz do repositório, Maven wrapper e `.gitignore`; arquivos Java, properties e migrations do backend; componentes e testes do frontend. O comando de backend passa a ser executado em `backend/`; os comandos npm permanecem em `frontend/`.
