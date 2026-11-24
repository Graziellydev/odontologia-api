# Tasks — RF013 Desempenho e Histórico Acadêmico

## 1. Exploração e definição de dados
- [x] 1.1 Inspecionar entidades, relacionamentos, enums/status, repositories, migrations e regras de metas acadêmicas; mapear aluno, turma, período, disciplina/tratamento, exigidos, concluídos e pendentes.
- [x] 1.2 Registrar no `design.md` a fonte de verdade e a regra de contagem para cada indicador, incluindo exclusões (cancelados, faltas, duplicados, registros sem conclusão) e cálculo de progresso.
- [x] 1.3 Confirmar existência de vínculo de turma e período. Se não existir, registrar a lacuna e apresentar proposta mínima; não criar migration até justificar e aprovar.

## 2. API e segurança
- [x] 2.1 Definir contrato de consulta sob `/api/coordenador/...` com modalidade ALUNO/TURMA, identificador e período; validar formato, obrigatoriedade e existência.
- [x] 2.2 Criar DTOs de request/response específicos (sem retornar entidades JPA), contendo resumo, histórico por disciplina/tratamento e, para turma, comparação por aluno e total.
- [x] 2.3 Implementar query/repository/service eficiente, evitando N+1 e consultas repetidas por aluno; usar agregação e filtros de período no banco quando apropriado.
- [x] 2.4 Implementar controller protegido por `COORDENADOR`, manter sessão/CSRF e respostas apropriadas: sucesso, inválido, não encontrado, vazio e erro interno sem detalhes sensíveis.
- [x] 2.5 Criar testes backend para autorização (coordenador permitido; demais perfis negados), validação, aluno/turma encontrada e não encontrada, período, cálculo, registros excluídos e resposta vazia.

## 3. Frontend — navegação e consulta
- [x] 3.1 Adicionar item/menu **Desempenho acadêmico** na área do coordenador sem alterar dashboard existente nem outras áreas.
- [x] 3.2 Criar tela com título, descrição, seletor Aluno/Turma, campo matrícula/código, seletor de período e botão Consultar, seguindo protótipo.
- [x] 3.3 Implementar serviço API com wrapper atual, estados loading, erro, vazio e “Nenhum registro foi localizado”; não disparar consulta até ação do usuário.
- [x] 3.4 Implementar cards Exigidos, Concluídos e Pendentes com números reais, contexto e progresso calculado corretamente (exigidos=0 => 0%).
- [x] 3.5 Implementar histórico por disciplina/tratamento com exigidos, concluídos, pendentes e progresso; incluir total do período.
- [x] 3.6 Na modalidade Turma, implementar comparativo compacto por aluno (nome/matrícula, exigidos, concluídos, pendentes, progresso) e total da turma; ocultar comparativo na modalidade Aluno.
- [x] 3.7 Garantir responsividade, acessibilidade (labels, foco, aria-live para estados), formatação pt-BR e consistência visual com o CSS/layout existente.

## 4. Qualidade e revisão
- [x] 4.1 Verificar cálculos: concluídos ≤ exigidos quando a regra aplicável assim determinar; pendentes não negativos; progresso sem divisão por zero; totais da turma coerentes com linhas exibidas.
- [x] 4.2 Verificar filtro aluno/turma e período, nova consulta, resposta obsoleta, ausência de resultados, falha de rede e sessão expirada.
- [x] 4.3 Confirmar que não são exibidos dados de prontuário/paciente nem dados fictícios; mensagens de erro não revelam dados internos.
- [ ] 4.4 Executar testes backend, testes frontend disponíveis, build frontend e validações OpenSpec; registrar comandos e resultados.
- [ ] 4.5 Revisar visual contra as capturas anexadas e, se possível, conferir frame Figma nomeado; corrigir diferenças significativas sem redesenhar componentes não relacionados.
- [x] 4.6 Confirmar que dashboard de métricas, login, secretaria, aluno e funcionalidades existentes não sofreram regressão.
