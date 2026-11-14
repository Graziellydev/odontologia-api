# Proposal

## Why

A agenda precisa representar cada cadeira, data e turno como uma vaga única e administrável. Sem um contrato dedicado para consultar, bloquear e remover vagas, a secretaria não consegue controlar a disponibilidade sem correr o risco de duplicidade ou de apagar referências históricas.

## What Changes

- Introduzir cadastro e consulta de vagas individuais por cadeira, data e turno.
- Expor operações REST sob `/api/vagas` e consulta de cadeiras sob `/api/cadeiras`, com filtros por período, cadeira, turno e situação.
- Permitir bloquear e desbloquear vagas com motivo de até 200 caracteres, respeitando o modelo atual.
- Permitir remover uma vaga somente quando nenhuma consulta estiver vinculada a ela.
- Aplicar a unicidade de `(cadeira_id, data, turno)` definida por `uk_vaga`; pedidos duplicados não devem criar uma segunda vaga.

## Capabilities

### New Capabilities
- `gestao-vagas`: Cadastro, consulta, bloqueio, desbloqueio e remoção segura de vagas individuais.

### Modified Capabilities

## Impact

- API Spring Boot: novos contratos em `/api/vagas` e `/api/cadeiras` e validações sobre as tabelas `vagas` e `cadeiras`.
- Persistência PostgreSQL: compatibilidade com `V5__create_consultas_and_transferencias.sql`, especialmente `uk_vaga`, `bloqueada` e `motivo_bloqueio`.
- Interface da secretaria: consumo futuro para administrar as vagas individuais.
