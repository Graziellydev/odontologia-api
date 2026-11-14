# Tasks

## 1. API de consulta e cadastro de vagas

- [x] 1.1 Criar DTOs e endpoints `GET /api/cadeiras`, `POST /api/vagas`, `GET /api/vagas` e `GET /api/vagas/{id}` com validação dos filtros; verificar por testes de controller os contratos, filtros, validações e autorização de secretaria.
- [x] 1.2 Implementar consultas de vagas com dados identificáveis da cadeira e validação de cadeira ativa; verificar por testes de serviço casos válidos, cadeira inexistente/inativa e resultado vazio.
- [x] 1.3 Tratar duplicidade da constraint `uk_vaga` como HTTP 409 inclusive sob concorrência; verificar por teste de integração que nunca há mais de uma vaga para cadeira, data e turno.

## 2. Bloqueio e remoção protegida

- [x] 2.1 Implementar bloqueio e desbloqueio transacionais com motivo de até 200 caracteres e integração à disponibilidade de agendamento; verificar por testes que motivos inválidos não alteram dados e vagas bloqueadas não são reservadas.
- [x] 2.2 Implementar remoção somente para vagas sem consultas referenciadas e traduzir conflito de chave estrangeira; verificar por teste de integração que consultas canceladas também impedem a remoção.
- [x] 2.3 Documentar os endpoints, filtros e respostas de erro para a secretaria; verificar que a documentação inclui os contratos e status HTTP implementados.
