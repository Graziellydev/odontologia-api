# Tarefas â€” RF012

## 1. AgregaÃ§Ãµes de leitura
- [x] 1.1 Criar `RelatorioCoordenacaoRepository` com `JdbcTemplate` e SQL nativo compatÃ­vel com tabelas/colunas das migrations; nÃ£o criar associaÃ§Ãµes JPA presumidas.
- [x] 1.2 Agregar pacientes distintos das fontes clÃ­nicas do contrato. Usar `pacientes.data_nascimento`, `bairro`, `cidade`, `consultas.paciente_id`, `consultas.procedimento_id`, `consultas.turma_id`, `consultas.vaga_id`, `vagas.data`, `turmas.semestre_letivo`, `triagens.paciente_id`, `triagens.data_triagem`, `triagem_itens.procedimento_id` e `procedimentos.nome/especialidade`.
- [x] 1.3 Implementar filtros coerentes: `semestre` (`turmas.semestre_letivo`), `dataInicio`/`dataFim` (data clÃ­nica), `especialidade` (texto de `procedimentos.especialidade`) e `regiao` (cidade derivada de `pacientes.cidade`, ou `TODOS`). Validar formatos e interseÃ§Ã£o.
- [x] 1.4 Agregar faixas de idade na data do evento, bairros/cidades e rankings de procedimentos/especialidades, evitando contar mais de uma vez o mesmo paciente na base demogrÃ¡fica.

## 2. ServiÃ§o, DTO e exportaÃ§Ã£o
- [x] 2.1 Criar records pÃºblicos sem campos de paciente individual: `RelatorioDemograficoResponse`, `RelatorioDemandaClinicaResponse`, `DistribuicaoItemResponse`, `FiltrosRelatorioResponse`.
- [x] 2.2 Criar `RelatorioCoordenacaoService` para validaÃ§Ã£o, percentuais, destaques, Top 4 territorial e agrupamento de grupos com menos de 3 pacientes distintos.
- [x] 2.3 Implementar CSV/JSON agregados com escaping CSV e nota de conteÃºdo agregado, respeitando os mesmos filtros dos GETs.

## 3. Controller e seguranÃ§a
- [x] 3.1 Criar `RelatorioCoordenacaoController` em `/api/coordenador/relatorios`, rotas GET `demografico`, `demanda-clinica` e seus sufixos `/exportar`.
- [x] 3.2 Exigir `hasAuthority('COORDENADOR')`, de acordo com `SecurityConfig`; nÃ£o alterar polÃ­tica de sessÃ£o/CSRF existente nem usar `hasRole`.

## 4. ValidaÃ§Ã£o da implementaÃ§Ã£o
- [x] 4.1 Cobrir filtros, faixas, desduplicaÃ§Ã£o, supressÃ£o territorial, exportaÃ§Ãµes sem PII e respostas 401/403.
- [ ] 4.2 Pendente: a suíte foi executada com `application-test.properties` (`ddl-auto=none`, H2); o teste Supabase está ignorado por falta de configuração externa. Validar contra o schema PostgreSQL com `ddl-auto=validate` em ambiente configurado.

