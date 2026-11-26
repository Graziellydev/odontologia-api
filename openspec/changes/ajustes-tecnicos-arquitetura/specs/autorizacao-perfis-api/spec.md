# Spec Delta

## Purpose

Estabelece uma convenção única de authorities e aplica autorização declarativa e de rota às APIs clínicas e acadêmicas sensíveis.

## ADDED Requirements

### Requirement: Convenção única de perfis
O sistema SHALL reconhecer e aplicar exclusivamente as authorities `SECRETARIA`, `ALUNO` e `COORDENADOR` nas regras de acesso da API.

#### Scenario: Authority legada não concede acesso
- **WHEN** uma requisição possui somente uma authority com prefixo incompatível com a convenção
- **THEN** ela não recebe acesso por equivalência implícita

### Requirement: Proteção explícita de retornos
O sistema SHALL exigir authority ALUNO para todas as rotas de retornos e validar declarativamente o mesmo perfil no controlador.

#### Scenario: Secretaria acessa retornos
- **WHEN** uma requisição autenticada como SECRETARIA acessa uma rota de retornos
- **THEN** o sistema responde com acesso proibido

### Requirement: Proteção explícita de metas e catálogo
O sistema SHALL exigir authority ALUNO para as rotas de metas e catálogo de solicitações, tanto na política de rotas quanto no controlador.

#### Scenario: Coordenador acessa metas de aluno
- **WHEN** uma requisição autenticada como COORDENADOR acessa uma rota de metas ou catálogo
- **THEN** o sistema responde com acesso proibido

### Requirement: Proteção consistente da fila de espera
O sistema SHALL exigir authority SECRETARIA para todas as rotas da fila de espera, tanto na política de rotas quanto no controlador.

#### Scenario: Aluno acessa fila de espera
- **WHEN** uma requisição autenticada como ALUNO acessa uma rota da fila de espera
- **THEN** o sistema responde com acesso proibido
