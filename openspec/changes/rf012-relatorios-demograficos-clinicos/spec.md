# Relatórios Demográficos e Clínicos

## Perfil demográfico

`GET /api/coordenacao/relatorios/demografico` retorna os indicadores populacionais agregados dos pacientes únicos atendidos ou triados no recorte selecionado[cite: 7, 14]. Aceita os parâmetros opcionais `semestre` (ex.: `2026.2`), `dataInicio` (`YYYY-MM-DD`), `dataFim` (`YYYY-MM-DD`), `especialidadeId` e `regiao` (`QUIXADA_E_REGIAO`, `APENAS_QUIXADA` ou `TODOS`, padrão `QUIXADA_E_REGIAO`)[cite: 14]. Requer sessão autenticada com perfil `COORDENADOR`[cite: 7, 9]; requisições de `ALUNO`, `SECRETARIA` ou não autenticadas respondem HTTP 403 ou HTTP 401.

## Regras de anonimização e LGPD

Nenhuma resposta ou arquivo exportado inclui nome, CPF, telefone, endereço completo ou identificador de prontuário[cite: 7, 11, 14]. As idades são agrupadas estritamente nas cinco faixas exibidas no painel: `Até 17 anos`, `18 a 29 anos`, `30 a 44 anos`, `45 a 59 anos` e `60 anos ou mais`[cite: 14]. A distribuição territorial exibe até os 4 bairros mais frequentes e consolida os demais bairros, municípios externos ou origens com menos de 3 pacientes únicos em `Outros bairros / municípios` para impedir reidentificação indireta[cite: 7, 11, 14].

## Payload do perfil demográfico

Sucesso retorna HTTP 200 com JSON contendo:
- `filtrosAplicados`: `{ "semestre": "2026.2", "especialidadeId": null, "regiao": "QUIXADA_E_REGIAO" }`[cite: 14]
- `baseAnalise`: `{ "totalPacientesUnicos": 180, "descricao": "Pacientes únicos atendidos no semestre" }`[cite: 14]
- `faixaMaisFrequente`: `{ "faixa": "30 a 44 anos", "quantidade": 54, "percentual": 30.0 }`[cite: 14]
- `bairroMaisFrequente`: `{ "bairro": "Centro", "quantidade": 54, "percentual": 30.0 }`[cite: 14]
- `distribuicaoFaixaEtaria`: lista ordenada das 5 faixas com `{ "faixa": "...", "quantidade": 36, "percentual": 20.0 }`[cite: 14]
- `distribuicaoTerritorial`: lista ordenada decrescentemente com `{ "origem": "Centro", "pacientesUnicos": 54, "percentualBase": 30.0 }`, encerrando com `Outros bairros / municípios` quando aplicável[cite: 14]

## Demanda clínica

`GET /api/coordenacao/relatorios/demanda-clinica` aceita os mesmos filtros (`semestre`, `dataInicio`, `dataFim`, `especialidadeId`, `regiao`) e exige perfil `COORDENADOR`[cite: 7, 9, 14]. Retorna HTTP 200 com o total de procedimentos/solicitações analisados, o procedimento e a especialidade mais demandados, e o ranking agregado por procedimento e especialidade com `quantidade` e `percentual`[cite: 7].

## Exportação de dados agregados

`GET /api/coordenacao/relatorios/demografico/exportar` e `GET /api/coordenacao/relatorios/demanda-clinica/exportar` aceitam os filtros da consulta mais `formato` (`CSV` ou `JSON`, padrão `CSV`). Retornam HTTP 200 com `Content-Disposition: attachment` e exclusivamente os totais e percentuais agregados, acompanhados da nota de conformidade LGPD[cite: 11, 14]. Operações `GET` de leitura não exigem token CSRF, mantendo a validação do cookie de sessão. 