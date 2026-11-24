# Design — RF013 Desempenho e Histórico Acadêmico

## Contexto e referências
A tela deve seguir o protótipo visual fornecido (captura de tela) e o frame Figma **“Coordenação Desempenho e histórico acadêmico”** no arquivo/link indicado pela pessoa solicitante. O acesso web ao Figma não foi possível nesta preparação; portanto, use as três capturas anexadas como referência visual verificável e, se o agente tiver integração/acesso local ao Figma, confira o frame nomeado antes de codificar.

O repositório é React no frontend e Spring Boot no backend. A área de Coordenador já existe (`frontend/src/Coordenador/PainelCoordenador.jsx`) e possui serviços de métricas (`frontend/src/services/metricasCoordenadorService.js`), endpoints `/api/coordenador/metricas` e `/api/coordenador/metricas/semestres`, protegidos por `COORDENADOR`. A tela RF013 deve ser uma funcionalidade separada; não substituir nem quebrar o dashboard de métricas existente.

## Estrutura visual a reproduzir
- Layout administrativo com sidebar da coordenação, identificação institucional e usuário no topo.
- Item de menu **Desempenho acadêmico** destacado/selecionado.
- Título **Desempenho e histórico acadêmico** e texto auxiliar curto.
- Linha de filtros: seletor **Visualização** (Aluno/Turma), campo **Matrícula do aluno ou código da turma**, seletor **Período**, botão **Consultar**.
- Cards de resumo: **Exigidos**, **Concluídos**, **Pendentes**, com valores e contexto de meta/progresso.
- Bloco **Histórico consolidado por disciplina** com colunas Disciplina/tratamento, Exigidos, Concluídos, Pendentes e Progresso; incluir total do período.
- Na visão de turma, bloco **Comparativo compacto – turma [código]** com alunos, matrícula, exigidos, concluídos, pendentes e progresso, e linha de total da turma.
- Nota de rodapé que esclareça a origem/escopo do consolidado (somente registros acadêmicos consolidados do período), sem sugerir leitura de prontuários.

## Regras de negócio
1. A consulta é exclusiva do coordenador. Professores/alunos/secretaria não podem acessar os endpoints ou dados por URL direta.
2. A tela permite duas modalidades: **Aluno** (matrícula) e **Turma** (código da turma). A modalidade selecionada determina como interpretar o campo de busca.
3. O período é obrigatório na consulta e deve respeitar os períodos acadêmicos disponíveis no sistema. Investigue se o conceito existente é semestre (`AAAA.1`/`AAAA.2`) ou outra chave e reutilize-o; não invente valores.
4. Consultar somente atendimentos/procedimentos que tenham status/condição de conclusão definidos no domínio. Pendente deve ser calculado conforme regra existente, sem contar registros cancelados, faltas ou duplicados como concluídos. Documentar a interpretação antes de implementar.
5. Aluno: exibir os números consolidados do aluno no período e linhas por disciplina/tratamento.
6. Turma: exibir consolidação da turma e uma linha por aluno para comparação, mais total da turma. Não misturar alunos de outras turmas.
7. Exigidos, concluídos, pendentes e progresso devem ser consistentes. Se a regra for `pendentes = exigidos - concluídos`, limitar a zero e documentar; progresso = concluídos/exigidos × 100, com exigidos zero resultando em 0% (não NaN/Infinity). Se os dados existentes definirem regra diferente, prevalece a regra de domínio documentada e aprovada.
8. Não inventar metas, disciplinas, alunos ou resultados. Dados vazios devem produzir estado vazio explícito.
9. Matrícula/código inválido ou não encontrado deve mostrar mensagem **“Nenhum registro foi localizado.”** sem stacktrace nem exposição de detalhes internos.
10. A consulta deve ocorrer somente após o clique em **Consultar** (ou comportamento equivalente já estabelecido no projeto). Evitar chamadas duplicadas; controlar loading, erro e cancelar/ignorar resposta obsoleta quando usuário fizer nova consulta.
11. Os dados exibidos não devem incluir conteúdo clínico/prontuário, nomes de pacientes, diagnósticos ou informações sensíveis de pacientes. Exibir somente agregados acadêmicos e identificadores/nome do aluno necessários à comparação.
12. Preserve padrões existentes de sessão, CSRF, API wrapper, tratamento de erros, componentes, estilos e navegação. Não reestruture telas não relacionadas.

## Contrato/API — decisão antes da implementação
Investigue o modelo atual e proponha o contrato mínimo necessário no `design.md` antes de implementar. Endpoint(s) devem ser sob `/api/coordenador/...`, autorizados com `COORDENADOR`. A API deve aceitar modalidade, identificador (matrícula ou código de turma) e período, validando parâmetros.

A resposta deve fornecer os cards e o histórico por disciplina/tratamento; para turma, também alunos e total. Use DTOs explícitos e evite retornar entidades JPA. Não crie endpoint, nomes de campos ou tabelas por suposição. Se o backend não possuir vínculos suficientes para apurar turma/aluno/periodo, descreva a lacuna e só proponha migration após confirmar necessidade.

## Estratégia de implementação
1. Explore código, entidades, enums/status, metas acadêmicas, consultas, migrations e testes. Mapear de onde vêm matrícula, turma, disciplina, procedimento exigido, concluído e pendente.
2. Documentar no design a regra de contagem e contrato API, incluindo exemplos de payloads/respostas e cenários de vazio.
3. Implementar query eficiente (evitar N+1 e consultas individuais por aluno na visão de turma); agregações podem ser feitas no banco quando viável.
4. Implementar serviço frontend e tela reutilizando layout da área coordenador; adicionar menu e roteamento/estado conforme padrão existente.
5. Testar autorização, validação, cálculo, filtros, dados vazios, erros e regressões.
6. Fazer revisão visual com o protótipo e screenshots.

## Exploração inicial do código e do banco

### Fontes existentes confirmadas
- `usuarios.matricula_ou_registro` identifica o aluno; `matriculas_turma` relaciona aluno e turma e possui unicidade por `(turma_id, aluno_id)`.
- `turmas` contém `codigo_turma`, `semestre_letivo` e `disciplina_id`. O período segue o formato `AAAA.1`/`AAAA.2` e já é listado pela área de coordenação.
- A constraint `uk_turma` é `(disciplina_id, semestre_letivo, codigo_turma)`, não torna o código único no período.
- `disciplina_procedimentos` é a fonte de metas: somente vínculos ativos com `quantidade_exigida > 0` entram em `v_progresso_metas_aluno`; a constraint garante uma meta por disciplina/procedimento.
- A view `v_progresso_metas_aluno` calcula a quantidade concluída contando linhas de `prontuarios` por aluno, turma e procedimento. Cada prontuário é assinado e `consulta_id` é único; a view calcula pendência como `GREATEST(quantidade_exigida - quantidade_concluida, 0)`.
- `MetaAlunoService` soma os totais limitando o concluído por procedimento à quantidade exigida, mantém pendência não negativa e define percentual como zero quando não há exigidos. A resposta por procedimento, porém, preserva a quantidade bruta da view.
- O fluxo clínico salva primeiro o prontuário assinado com `concluirAtendimento=false`; o encerramento ocorre em uma chamada separada e muda `consultas.status_consulta` para `REALIZADO`. A coordenação decidiu preservar para RF013 a regra acadêmica já usada em HU03: prontuário assinado conta como concluído, sem exigir `REALIZADO`.
- A view atual não filtra status da consulta. Para atender à RF013, sem modificar a view nem HU03, a consulta RF013 deve excluir prontuários vinculados a consultas `CANCELADO_SECRETARIA`, `CANCELADO_PACIENTE`, `FALTOU_PACIENTE` ou `FALTOU_ALUNO`; um prontuário assinado associado a consulta aberta continua contando. `prontuarios.consulta_id` único, `matriculas_turma(turma_id, aluno_id)` e `disciplina_procedimentos(disciplina_id, procedimento_id)` impedem duplicação pelos vínculos dessas relações.
- Exigidos vêm das metas ativas da disciplina da turma; o período do consolidado é o `semestre_letivo` da turma, não a data clínica do prontuário.
- Não é necessário criar migration para representar aluno, vínculo, turma, disciplina, meta ou período. A consulta deve retornar apenas agregados acadêmicos e identificação de aluno necessária; não deve projetar campos de `prontuarios` nem de pacientes.

### Decisões aprovadas para RF013
- **Conclusão:** contar prontuários assinados, conforme a fonte acadêmica atual de HU03; não exigir que a consulta já esteja `REALIZADO`. Aplicar somente a exclusão RF013 de cancelamentos e faltas descrita acima, sem alterar o comportamento da tela/serviço de metas existente.
- **Turma:** tratar o filtro código + período como agrupador de todas as turmas disciplinares que coincidam. Consolidar a história por disciplina/procedimento e apresentar uma única linha de comparação por aluno distinto, somando as metas e os prontuários dos vínculos correspondentes.
- **Consistência dos números:** limitar concluídos por procedimento ao exigido, como `MetaAlunoService` faz nos totais; calcular pendentes como `max(exigidos - concluídos, 0)` e progresso como `concluídos / exigidos`, com zero quando exigidos for zero. Aplicar o mesmo cálculo a cada linha e aos agregados, inclusive turma, para que detalhamentos fechem com os totais.

## Contrato de API aprovado

- Períodos: reutilizar `GET /api/coordenador/metricas/semestres`; não criar uma segunda fonte para as opções.
- Consulta: `GET /api/coordenador/desempenho-historico?modalidade=ALUNO|TURMA&identificador={matricula|codigoTurma}&semestre={AAAA.1|AAAA.2}`. Todos os parâmetros são obrigatórios. A rota e o controller exigem `COORDENADOR`, mantendo sessão e CSRF existentes (GET não altera estado).
- O período deve ter formato `AAAA.1`/`AAAA.2` e constar nos períodos cadastrados. Identificador vazio ou modalidade inválida retorna 400. Período malformado retorna 400 no contrato existente; período sem cadastro retorna 404. Aluno/turma sem correspondência no período retorna 404 com `Nenhum registro foi localizado.`; correspondência existente sem metas retorna 200 com indicadores zerados e listas vazias.
- ALUNO: localizar por matrícula e perfil `ALUNO`; consolidar as matrículas e metas do aluno no semestre informado. TURMA: selecionar todas as turmas com o código e semestre informados, mesmo quando forem de disciplinas diferentes; consolidar por aluno distinto e não repetir vínculos.
- Esquema da resposta (campos acadêmicos apenas; `alunos` é vazio na modalidade ALUNO):

```json
{
  "modalidade": "ALUNO|TURMA",
  "identificador": "<matrícula ou código da turma>",
  "semestre": "<AAAA.1>",
  "nomeAluno": "<nome ou null>",
  "matriculaAluno": "<matrícula ou null>",
  "codigoTurma": "<código ou null>",
  "resumo": {
    "exigidos": "<inteiro>",
    "concluidos": "<inteiro>",
    "pendentes": "<inteiro>",
    "progressoPercentual": "<inteiro>"
  },
  "historico": [
    {
      "disciplina": "<nome>",
      "procedimento": "<nome>",
      "exigidos": "<inteiro>",
      "concluidos": "<inteiro>",
      "pendentes": "<inteiro>",
      "progressoPercentual": "<inteiro>"
    }
  ],
  "alunos": [
    {
      "nomeAluno": "<nome>",
      "matriculaAluno": "<matrícula>",
      "exigidos": "<inteiro>",
      "concluidos": "<inteiro>",
      "pendentes": "<inteiro>",
      "progressoPercentual": "<inteiro>"
    }
  ]
}
```

- A implementação deve usar DTOs e uma leitura agregada em lote, sem retornar entidades nem consultar aluno por aluno. A agregação considera prontuários assinados por matrícula/turma/procedimento, excluindo os quatro status cancelado/falta aprovados acima; não retorna paciente, consulta, texto clínico, assinatura digital ou prontuário.
- Resposta de não localização: `404 {"codigo":"REGISTRO_NAO_ENCONTRADO","mensagem":"Nenhum registro foi localizado."}` sem detalhes internos. Respostas 401/403 seguem a configuração de segurança.

### Referência visual pendente de validação
- O arquivo Figma foi aberto e a camada `Coordenação — Desempenho acadêmico` foi localizada na lista de camadas. O acesso anônimo não permitiu selecionar/inspecionar essa camada; o quadro geral está em escala insuficiente para validar medidas, cores e conteúdo. As capturas mencionadas pela coordenação não estão acessíveis como arquivos nesta execução. A estrutura de tela indicada na proposta e no roteiro foi registrada acima; a comparação visual de fidelidade permanece pendente.

## Exploração inicial do código e do banco

### Fontes existentes confirmadas
- `usuarios.matricula_ou_registro` identifica o aluno; `matriculas_turma` relaciona aluno e turma e possui unicidade por `(turma_id, aluno_id)`.
- `turmas` contém `codigo_turma`, `semestre_letivo` e `disciplina_id`. O período segue o formato `AAAA.1`/`AAAA.2` e já é listado pela área de coordenação.
- A constraint `uk_turma` é `(disciplina_id, semestre_letivo, codigo_turma)`, não torna o código único no período. Assim, o mesmo código pode identificar turmas de disciplinas diferentes.
- `disciplina_procedimentos` é a fonte de metas: somente vínculos ativos com `quantidade_exigida > 0` entram em `v_progresso_metas_aluno`.
- A view `v_progresso_metas_aluno` calcula a quantidade concluída contando linhas de `prontuarios` por aluno, turma e procedimento. Cada prontuário é assinado e `consulta_id` é único; a view calcula pendência como `GREATEST(quantidade_exigida - quantidade_concluida, 0)`.
- `MetaAlunoService` soma os totais limitando o concluído por procedimento à quantidade exigida, mantém pendência não negativa e define percentual como zero quando não há exigidos. A resposta por procedimento, porém, preserva a quantidade bruta da view.
- O fluxo clínico salva primeiro o prontuário assinado com `concluirAtendimento=false`; o encerramento ocorre em uma chamada separada e muda `consultas.status_consulta` para `REALIZADO`. Portanto, a view pode contar um prontuário assinado antes desse encerramento. Os dados de cancelamento e falta são status de `consultas`, mas a view atual não os filtra explicitamente.
- Não é necessário criar migration para representar aluno, vínculo, turma, disciplina, meta ou período. A consulta deve retornar apenas agregados acadêmicos e identificação de aluno necessária; não deve projetar campos de `prontuarios` nem de pacientes.

### Decisões pendentes antes da implementação
1. **Concluído e exclusões:** a fonte acadêmica atual define concluído como prontuário assinado, enquanto o fluxo também distingue atendimento encerrado (`REALIZADO`). Confirmar se RF013 deve preservar a regra da view/meta (prontuário assinado, ainda que a consulta aguarde encerramento) ou contar somente registros cuja consulta esteja `REALIZADO`. A segunda opção diverge da regra atualmente usada por HU03. Também confirmar o tratamento desejado para dados legados em que um prontuário assinado esteja associado a consulta cancelada/falta; a view não estabelece essa exclusão.
2. **Identificação de turma:** o filtro especificado contém somente código e período, mas a constraint permite colisões de código entre disciplinas. Confirmar se a busca deve agrupar todas as turmas que coincidam com código/período, se deve rejeitar código ambíguo e exigir outro filtro, ou se o domínio deve tornar o código globalmente único no período. A última alternativa requer avaliar constraint/migration e não será aplicada sem aprovação.
3. **Referência visual:** não há captura do protótipo anexada disponível no workspace/conversa. O link Figma fornecido abre o arquivo, mas o frame nomeado não pôde ser identificado/inspecionado no acesso disponível. Para validar a reprodução fiel, falta um link direto para o frame (node-id correto) ou a captura de referência.
