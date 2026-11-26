# Design — RF012 Relatórios demográficos e clínicos

## Fontes confirmadas no código e schema

- `model.Paciente` mapeia `pacientes`: `id`, `dataNascimento`/`data_nascimento`, `bairro`, `cidade`; há também PII que nunca deve ser projetada (`nomeCompleto`, `cpf`, telefones, endereço e prontuário).
- `consultas.entity.Consulta` mapeia `consultas` e contém IDs escalares `pacienteId`, `procedimentoId`, `turmaId`, `vagaId` e `statusConsulta`; não possui associações JPA.
- `consultas.entity.Vaga` mapeia `vagas`: `data` é `LocalDate` e fornece a data do atendimento.
- `turmas.semestre_letivo` fornece o semestre acadêmico; não há semestre em `Consulta` nem `Paciente`.
- `procedimentos` é uma tabela SQL criada na migration V3 com `id`, `nome`, `especialidade` (VARCHAR(60)), `exige_dente`, `exige_classe`, `ativo`. Não existe entidade JPA `Procedimento`; `especialidade` é texto, não uma entidade/catalogo com `especialidadeId`.
- `triagens` e `triagem_itens` existem nas migrations V3, mas não foram localizadas entidades Java nem repositório. Seus campos incluem `paciente_id`, `data_triagem` e `procedimento_id`; acesso deve ser SQL nativo/JdbcTemplate.
- `PacienteRepository` só tem operações de cadastro/busca e não implementa agregações. Para os relatórios, SQL nativo via `JdbcTemplate` evita pressupor relacionamentos JPA ausentes e usa os nomes do schema.
- `application.properties` define `spring.jpa.hibernate.ddl-auto=validate`; nenhuma entidade/tabela/coluna nova é necessária.
- `SecurityConfig` protege prefixos `/api/coordenador/`; as rotas já existentes exigem `hasAuthority("COORDENADOR")`. GET usa a política CSRF existente e continua exigindo autenticação/sessão.

Não foram encontrados `openspec/AGENTS.md` ou `openspec/project.md`; a auditoria seguiu os documentos disponíveis na change e o código/migrations do projeto.

## Decisões de domínio para a especificação

1. A origem demográfica inclui pacientes distintos presentes em consultas no recorte e, quando aplicável, pacientes distintos presentes em triagens no recorte. Uma união por `paciente_id` evita contar a mesma pessoa duas vezes. Quando semestre é informado, ele limita atendimentos via turma; triagens sem vínculo acadêmico não podem ser atribuídas a semestre com o schema atual e ficam fora desse recorte. Semestre e intervalo de datas juntos são aceitos como interseção.
2. Para idade, usar `data_nascimento` na data do atendimento (vagas.data) ou da triagem (data_triagem), e data atual apenas quando a fonte não fornecer data de evento. A idade é calculada em anos completos e distribuída nas cinco faixas fixas.
3. Territorial: `bairro` e `cidade` são os dados disponíveis. A região é derivada de `cidade`; o backend deve documentar o conjunto aceito/normalização no contrato e não simular catálogo inexistente. Nome da cidade/bairro é texto livre.
4. Demanda clínica: triagens detalhadas por `triagem_itens`/`procedimentos` representam demanda identificada; consultas com `procedimento_id` representam procedimentos agendados/realizados e não substituem os itens de triagem. O relatório deve definir explicitamente a métrica de ranking conforme o endpoint; não apresentar triagem como atendimento realizado. Especialidade é agrupamento pelo texto `procedimentos.especialidade`.
5. As contagens territoriais com menos de 3 pacientes distintos e posições além do Top 4 são consolidadas em `Outros bairros / municípios`. Todos os payloads/exportações contêm exclusivamente totais, categorias e percentuais.
6. Não há `especialidadeId`; o parâmetro especificado passa a ser `especialidade` (texto). `regiao` é texto/cidade normalizada ou `TODOS`, definido sem enumeração inventada. `semestre` corresponde ao `semestre_letivo` (`AAAA.1`/`AAAA.2`); datas usam `YYYY-MM-DD` e filtram a data clínica.

## Rotas e autorização

- `GET /api/coordenador/relatorios/demografico`
- `GET /api/coordenador/relatorios/demanda-clinica`
- `GET /api/coordenador/relatorios/demografico/exportar`
- `GET /api/coordenador/relatorios/demanda-clinica/exportar`

Os quatro endpoints exigem `hasAuthority('COORDENADOR')`; não usar `hasRole`, pois as autoridades atuais são os nomes literais `ALUNO`, `SECRETARIA`, `COORDENADOR` (com exceção pontual `ROLE_SECRETARIA` em outro fluxo). A configuração trata não autenticado como 401 e sem autoridade como 403.

## Classes previstas (nenhuma criada nesta etapa)

- `relatorios.controller.RelatorioCoordenacaoController`: quatro rotas GET, filtros e negociação CSV/JSON.
- `relatorios.service.RelatorioCoordenacaoService`: valida filtros, coordena agregações e aplica agrupamentos/percentuais/anonimização.
- `relatorios.repository.RelatorioCoordenacaoRepository`: consultas SQL nativas via `JdbcTemplate`, usando tabelas e colunas confirmadas.
- DTOs/records no pacote `relatorios.dto`: `RelatorioDemograficoResponse`, `RelatorioDemandaClinicaResponse`, `DistribuicaoItemResponse`, `FiltrosRelatorioResponse` (e projeções internas não serializadas, se necessárias).

DTOs públicos não expõem IDs de paciente, nomes, CPF, contato, endereço, prontuário, IDs de consulta ou linhas individuais. Nenhuma entidade JPA nova e nenhuma migration está prevista.

## Exportação

CSV e JSON representam os mesmos agregados do endpoint JSON com os mesmos filtros. CSV deve escapar separadores/quebras de linha; ambos devem sinalizar que o conteúdo é agregado e não inclui dados identificáveis. Exportação não emite registros de paciente.

Contrato funcional: [specs/relatorios-coordenacao/spec.md](specs/relatorios-coordenacao/spec.md).
