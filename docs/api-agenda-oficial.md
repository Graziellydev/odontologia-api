# Agenda oficial da secretaria

Todas as rotas exigem uma sessao autorizada de secretaria. Datas usam `YYYY-MM-DD`; a grade deve cobrir uma semana completa, de segunda-feira a domingo.

## Grade semanal

`GET /api/vagas/grade?inicio=2026-11-09&fim=2026-11-15`

Filtros opcionais: `disciplina` (nome exato) e `disponibilidade` (`LIVRE`, `OCUPADA` ou `BLOQUEADA`). Cada item representa cadeira, data e turno. A grade inclui combinacoes sem linha em `vagas`: esses itens aparecem como `LIVRE` quando nao ha consulta ativa nem bloqueio. Itens ocupados incluem dados da consulta, paciente, aluno e disciplina.

## Criar agendamento

`POST /api/consultas` recebe uma demanda elegivel da fila e a cadeira selecionada na grade:

```json
{"filaEsperaId":10,"alunoId":40,"turmaId":45,"cadeiraId":3,"data":"2026-11-12","turno":"MANHA"}
```

O servidor cria a vaga se ainda nao existir, usando `uk_vaga` como garantia de unicidade. Vaga bloqueada, ocupada ou cadeira inativa resulta em `409 Conflict`. Sucesso responde `201 Created` com o agendamento, identificador da vaga e dados da notificacao gerada. A insercao da vaga, consulta e alteracao da fila ocorrem na mesma transacao.

## Remarcar e cancelar

`POST /api/consultas/{id}/remarcar` recebe `{"cadeiraId":3,"data":"2026-11-16","turno":"TARDE"}`. O servidor cria a vaga se necessario, valida que a cadeira esta livre, marca a consulta anterior como cancelada e cria a nova consulta com `consultaOrigemId`, preservando o historico.

`PATCH /api/consultas/{id}/cancelar` marca a consulta como `CANCELADO_SECRETARIA` e devolve a entrada vinculada para a fila conforme as regras de elegibilidade. A vaga e a consulta historica nao sao removidas.

Conflitos por vaga, fila, paciente ou aluno retornam `409 Conflict`; uma consulta ou demanda inexistente retorna `404 Not Found`; payloads ou periodos invalidos retornam `400 Bad Request`.
