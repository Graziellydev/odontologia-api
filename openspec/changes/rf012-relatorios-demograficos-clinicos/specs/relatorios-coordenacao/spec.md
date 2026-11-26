# Relatórios de coordenação (RF012)

## Perfil demográfico

`GET /api/coordenador/relatorios/demografico` retorna indicadores agregados de pacientes distintos observados em consultas e/ou triagens do recorte. Não retorna entidades nem dados de pacientes individuais.

Filtros opcionais: `semestre` (`turmas.semestre_letivo`, formato `AAAA.1` ou `AAAA.2`), `dataInicio` e `dataFim` (`YYYY-MM-DD`), `especialidade` (texto correspondente a `procedimentos.especialidade`) e `regiao` (cidade correspondente a `pacientes.cidade`; `TODOS` omite o filtro). As datas limitam `vagas.data` para consultas e `triagens.data_triagem` para triagens. `semestre` limita consultas pela turma; triagens não têm vínculo a semestre no esquema e não são incluídas quando o filtro de semestre é aplicado. Se fornecidos juntos, filtros representam interseção. `especialidadeId` não é suportado, pois o schema não contém entidade ou FK de especialidade.

Requer autoridade `COORDENADOR` via `hasAuthority('COORDENADOR')`. Usuário não autenticado recebe 401; usuário autenticado sem autoridade recebe 403. A leitura GET mantém autenticação por sessão e não altera dados.

## Anonimização e distribuição

Idade é calculada em anos completos na data do evento (data de consulta ou triagem) e agrupada em `Até 17 anos`, `18 a 29 anos`, `30 a 44 anos`, `45 a 59 anos` e `60 anos ou mais`. Valores ausentes não podem gerar uma faixa artificial. Bairro/cidade são normalizados para agregar variações de caixa e espaços.

A distribuição territorial apresenta no máximo quatro bairros individualizados. Origens fora dos quatro primeiros lugares e origens com menos de 3 pacientes distintos integram `Outros bairros / municípios`. As respostas e exportações não incluem nome, CPF, telefone, email, endereço, número de prontuário, IDs de paciente/consulta ou qualquer linha identificável.

## Resposta demográfica

Sucesso retorna HTTP 200 com agregados e filtros efetivamente aplicados. Forma conceitual:

```json
{
  "filtrosAplicados": {
    "semestre": "2026.2",
    "dataInicio": null,
    "dataFim": null,
    "especialidade": null,
    "regiao": "TODOS"
  },
  "baseAnalise": { "totalPacientesUnicos": 180, "descricao": "Pacientes únicos no recorte" },
  "faixaMaisFrequente": { "categoria": "30 a 44 anos", "quantidade": 54, "percentual": 30.0 },
  "origemMaisFrequente": { "categoria": "Centro", "quantidade": 54, "percentual": 30.0 },
  "distribuicaoFaixaEtaria": [
    { "categoria": "Até 17 anos", "quantidade": 12, "percentual": 6.7 }
  ],
  "distribuicaoTerritorial": [
    { "categoria": "Centro", "quantidade": 54, "percentual": 30.0 },
    { "categoria": "Outros bairros / municípios", "quantidade": 8, "percentual": 4.4 }
  ]
}
```

Os valores são ilustrativos. Distribuições devem ser determinísticas; denominador de percentuais é `totalPacientesUnicos`. Categoria de território representa bairro no contexto da cidade/região aplicada.

## Demanda clínica

`GET /api/coordenador/relatorios/demanda-clinica` aceita os mesmos filtros, com as mesmas regras de autorização. A demanda identificada vem dos itens de triagem (`triagem_itens.procedimento_id`); os agendamentos/atendimentos por procedimento vêm de `consultas.procedimento_id` e devem ser distinguidos no resultado, não somados como se fossem o mesmo evento. Procedimento e especialidade são agrupados por `procedimentos.nome` e `procedimentos.especialidade`; especialidade é texto simples.

O resultado contém totais agregados e rankings com categoria, quantidade e percentual, sem PII. `triagens` não possuem vínculo a `turmas` no schema; com filtro de semestre, a parte de demanda originada de triagem não é incluída. O relatório deve indicar no payload se contagens representam itens de triagem ou consultas.

## Exportação

`GET /api/coordenador/relatorios/demografico/exportar` e `GET /api/coordenador/relatorios/demanda-clinica/exportar` aceitam os filtros correspondentes e `formato=CSV|JSON` (padrão `CSV`). Retornam attachment com os mesmos agregados filtrados das rotas de consulta e nota de que o arquivo contém dados agregados sem identificadores pessoais. Não exportam registros individuais.
