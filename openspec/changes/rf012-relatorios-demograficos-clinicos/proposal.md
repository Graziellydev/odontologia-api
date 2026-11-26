# Proposta: Relatórios demográficos e clínicos (RF012)

## Motivação

A coordenação precisa consultar indicadores agregados da população atendida e da demanda por procedimentos sem expor dados pessoais. A fonte deve refletir o esquema PostgreSQL aplicado pelas migrations e compatível com `spring.jpa.hibernate.ddl-auto=validate`.

## Escopo

- Criar endpoints de leitura sob `/api/coordenador/relatorios` para perfil demográfico e demanda clínica, autorizados por `hasAuthority('COORDENADOR')`, como as rotas de coordenação já existentes.
- Usar pacientes (`dataNascimento`, `bairro`, `cidade`), consultas, vagas (`data`) e turmas (`semestreLetivo`) para os atendimentos; a especialidade é o campo textual `procedimentos.especialidade`.
- Usar as tabelas SQL `triagens` e `triagem_itens` somente por consulta SQL nativa: não há entidades JPA `Triagem` ou entidades JPA `Procedimento`/`Especialidade`. Consultas armazenam `pacienteId`, `procedimentoId`, `turmaId` e `vagaId` como IDs escalares, sem relacionamentos JPA.
- Filtrar semestre pela `turmas.semestre_letivo` ligada a `consultas.turma_id`; filtrar intervalo por `vagas.data` para consultas e `triagens.data_triagem` para triagens. `especialidadeId` e `regiao` não correspondem a identificadores/tipos presentes no modelo atual; o contrato usa `especialidade` (texto) e uma região derivada de `pacientes.cidade` até que exista catálogo territorial/especialidade.
- Fornecer distribuição nas faixas etárias `Até 17 anos`, `18 a 29 anos`, `30 a 44 anos`, `45 a 59 anos`, `60 anos ou mais`, agregação territorial e ranking de procedimentos/especialidades.
- Permitir exportação agregada CSV/JSON, sem linhas individuais ou atributos identificáveis.

## Não objetivos

- Alterar entidades, tabelas ou migrations; adicionar colunas/relacionamentos para atender ao relatório; ou depender de DDL automático além de `validate`.
- Criar filtros por identificadores de especialidade ou valores enumerados de região sem suporte no schema atual.
- Implementar telas frontend nesta change de backend.

## Impacto previsto

Backend somente: controller, serviço, DTOs e repositório de agregação SQL nativa. As consultas devem referenciar nomes reais das tabelas/colunas das migrations e não exigem mudanças no schema. Os nomes e responsabilidades planejados estão descritos ao final de `design.md`.

Especificação da capacidade: [specs/relatorios-coordenacao/spec.md](specs/relatorios-coordenacao/spec.md).
