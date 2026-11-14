# API de turnos disponíveis

Todas as rotas exigem sessão de secretaria. Datas usam ISO `YYYY-MM-DD`; a semana exibida começa na segunda-feira e termina no domingo.

## Gerar vagas para um período

`POST /api/vagas/geracoes`

```json
{"inicio":"2026-11-09","fim":"2026-11-15"}
```

O intervalo é inclusivo e inclui todos os dias, inclusive sábados e domingos. Para cada cadeira ativa no momento da operação, a API gera MANHA (08h–12h) e TARDE (13h–17h). A chave `(cadeira_id, data, turno)` impede duplicidade; repetições são idempotentes.

Não foi definido um limite máximo fixo para o intervalo. O volume calculado é o número de dias multiplicado pelas cadeiras ativas e pelos dois turnos.

Resposta `200 OK`:

```json
{"criadas":28,"jaExistentes":4}
```

## Consultar a grade semanal

`GET /api/vagas/grade?inicio=2026-11-09&fim=2026-11-15`

`inicio` precisa ser uma segunda-feira e `fim` o domingo correspondente. A resposta contém uma combinação por cadeira ativa, data e turno, com estado `OCUPADA`, `BLOQUEADA` ou `LIVRE`. `LIVRE` também é retornado quando não existe linha em `vagas`, desde que não haja bloqueio nem consulta ativa. Filtros opcionais: `disciplina` (nome exato) e `disponibilidade` (`LIVRE`, `OCUPADA` ou `BLOQUEADA`).

Cada item inclui cadeira, data, turno, `vagaId` quando houver registro, motivo do bloqueio e dados de consulta/aluno/paciente quando ocupada. Os horários são derivados do campo persistido `turno`.

## Bloquear turno da grade

`PATCH /api/vagas/bloqueio-turno` aceita:

```json
{"cadeiraId":3,"data":"2026-11-14","turno":"TARDE","motivo":"Manutenção"}
```

A API cria a linha de vaga quando ela não existe e bloqueia a combinação. Motivo é obrigatório, não pode exceder 200 caracteres, e a operação é recusada se qualquer consulta, inclusive histórica, estiver vinculada à vaga.

O desbloqueio usa `PATCH /api/vagas/{vagaId}/desbloqueio`. Remoção usa `DELETE /api/vagas/{vagaId}` e só é permitida quando nenhuma consulta referencia a vaga.

## Erros relevantes

- `400 Bad Request`: período inválido, semana fora de segunda a domingo, ou motivo inválido.
- `401 Unauthorized` / `403 Forbidden`: sessão ausente ou perfil sem autorização.
- `409 Conflict`: cadeira inexistente/inativa ou turno com consulta vinculada.
