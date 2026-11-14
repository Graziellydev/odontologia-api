# Proposal

## Why

A secretaria precisa preparar a capacidade semanal de atendimento e visualizar, em uma única grade, tanto os turnos ocupados quanto os horários realmente livres. Hoje a disponibilidade depende de vagas existentes e de consultas; a geração em lote e a representação da ausência de consulta como “Vaga livre” precisam de um comportamento explícito e consistente.

## What Changes

- Gerar vagas em lote para um período ou semana, em todas as cadeiras ativas, para manhã (08h–12h) e tarde (13h–17h).
- Consultar a grade semanal por data, incluindo cadeira e turno mesmo quando ainda não houver registro de vaga, indicando “Vaga livre” quando não existir consulta ativa e a vaga não estiver bloqueada.
- Exibir consultas ativas, turnos bloqueados e turnos livres de forma distinguível.
- Permitir bloquear e excluir turnos sem consultas vinculadas; bloqueios exigem motivo.
- Tratar repetições da geração conforme a chave única `uk_vaga (cadeira_id, data, turno)`, sem gerar slots duplicados.

## Capabilities

### New Capabilities
- `turnos-disponiveis`: Geração em lote, visualização da grade semanal e gestão de turnos livres ou bloqueados.

### Modified Capabilities

## Impact

- API de agenda e disponibilidade, tabelas `cadeiras`, `vagas` e `consultas`, com compatibilidade com a constraint `uk_vaga` do V5.
- Interface da secretaria para uma grade semanal com manhã e tarde, estado de cada turno e filtros de período.
- Dependência funcional de `gestao-vagas` para as regras compartilhadas de bloqueio e exclusão de vagas, sem tornar a geração repetida fonte de duplicidades.
