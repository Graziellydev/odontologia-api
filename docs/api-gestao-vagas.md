# API de gestão de vagas

Todas as rotas abaixo exigem uma sessão ativa com perfil de secretaria. Datas usam `YYYY-MM-DD`; turnos aceitos são `MANHA` e `TARDE`.

## Cadeiras

`GET /api/cadeiras` retorna as cadeiras ativas para seleção:

```json
[{"id":3,"numero":"03","ativa":true}]
```

## Vagas

`POST /api/vagas` cria uma vaga. `cadeiraId`, `data` e `turno` são obrigatórios.

```json
{"cadeiraId":3,"data":"2026-12-01","turno":"MANHA"}
```

Retorna `201 Created`, o objeto da vaga e `Location: /api/vagas/{id}`. A resposta inclui `id`, `cadeiraId`, `cadeiraNumero`, `data`, `turno`, `bloqueada` e `motivoBloqueio`.

`GET /api/vagas` aceita filtros opcionais combináveis:

| Parâmetro | Exemplo | Uso |
|---|---|---|
| `inicio` | `2026-12-01` | Data inicial inclusiva |
| `fim` | `2026-12-31` | Data final inclusiva |
| `cadeiraId` | `3` | Identificador da cadeira |
| `turno` | `MANHA` | Turno |
| `bloqueada` | `true` | Situação de bloqueio |

`GET /api/vagas/{id}` consulta uma vaga individual.

## Bloqueio e remoção

- `PATCH /api/vagas/{id}/bloqueio` recebe `{"motivo":"Manutenção"}`. O motivo é obrigatório e limitado a 200 caracteres.
- `PATCH /api/vagas/{id}/desbloqueio` remove o bloqueio e seu motivo.
- `DELETE /api/vagas/{id}` remove uma vaga sem consultas vinculadas e retorna `204 No Content`.

## Erros relevantes

- `400 Bad Request`: turno, datas ou motivo inválidos.
- `401 Unauthorized` / `403 Forbidden`: sessão ausente ou sem perfil autorizado.
- `404 Not Found`: vaga não encontrada.
- `409 Conflict`: combinação cadeira/data/turno duplicada, cadeira inexistente/inativa, ou vaga com consultas vinculadas.

A chave `uk_vaga (cadeira_id, data, turno)` e a chave estrangeira de consultas são mantidas como proteção final para operações concorrentes.
