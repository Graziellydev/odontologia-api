# Roteiro para implementar RF013 com Spec-Driven Development

Este pacote OpenSpec foi preparado para o repositório `odontologia-api-main`. Não comece a codificar antes de cumprir a exploração e registrar as decisões de domínio.

## Ordem obrigatória
1. Leia `proposal.md`, `design.md`, `tasks.md` e `specs/desempenho-historico-academico/spec.md por completo.
2. Explore o código existente, especialmente área de Coordenador, serviços de métricas, metas acadêmicas, entidades de atendimento/procedimento, status, turma, migrations e testes.
3. **Primeiro atualize o design** com a fonte de verdade e a regra exata de exigidos/concluídos/pendentes. Não invente tabela, endpoint, turma, metas ou status.
4. Implemente item por item de `tasks.md`, marcando checklist somente depois de executar e validar.
5. Antes de concluir, execute os testes/build e faça revisão visual com as screenshots; se houver acesso ao Figma, confira o frame “Coordenação Desempenho e histórico acadêmico”.

## Prompt sugerido para o agente de código

> Implemente a change OpenSpec `rf013-desempenho-historico-academico` seguindo Spec-Driven Development.
>
> Leia todos os artefatos da change antes de agir. Use o fluxo OpenSpec/skills disponível no repositório. Explore primeiro o sistema e confirme, com código e banco/migrations, como são representados aluno, turma, período, disciplinas/tratamentos, metas/exigidos, atendimentos, procedimentos concluídos, pendências e status. Documente as regras de domínio e contrato de API em `design.md` antes de implementar.
>
> Siga os requisitos e cenários em `specs/desempenho-historico-academico/spec.md` e o checklist em `tasks.md`. O protótipo é a referência visual: filtros Aluno/Turma, matrícula/código, período e Consultar; cards Exigidos/Concluídos/Pendentes; histórico por disciplina/tratamento; comparativo e total da turma somente na visão Turma. As screenshots anexadas estão disponíveis como referência. O frame Figma tem o nome “Coordenação Desempenho e histórico acadêmico”.
>
> Reutilize arquitetura, sessão, CSRF, autorização, API wrapper, padrões de CSS e componentes existentes. Não refatore áreas não relacionadas. Não use dados mockados, hardcoded ou do screenshot como resultado de produção. Não exponha dados de pacientes ou prontuários. Backend deve proteger os endpoints para COORDENADOR; segurança não pode depender somente de esconder a tela.
>
> Se faltar vínculo de turma, regra de meta, dado de período ou outra informação indispensável, não improvise: registre a lacuna, explique alternativas e pare para pedir decisão antes de alterar o banco ou inventar comportamento.
>
> Faça consultas eficientes para turma, evitando N+1. Implemente estados loading, erro, vazio, não encontrado, sessão expirada e consulta repetida/obsoleta. Garanta cálculos seguros (zero exigidos => 0%) e consistência dos totais.
>
> Ao finalizar, rode testes backend/frontend disponíveis, build e verificações OpenSpec. Registre comandos e resultados, marque tasks concluídas apenas se validadas, e faça revisão visual comparando com as screenshots/Figma. Entregue resumo dos arquivos alterados, API/contrato, regra de cálculo confirmada, testes executados e limitações.
