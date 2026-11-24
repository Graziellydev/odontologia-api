# Specification — desempenho-historico-academico

## Requirements

### Requirement: Consulta restrita à coordenação
O sistema MUST permitir que somente usuário autenticado com perfil COORDENADOR consulte o desempenho acadêmico. MUST negar a mesma consulta para os demais perfis no backend, mesmo que tentem acessar endpoint diretamente.

#### Scenario: Coordenador autorizado
- GIVEN uma sessão autenticada de COORDENADOR
- WHEN solicita desempenho por aluno ou turma
- THEN o sistema retorna o consolidado permitido para a consulta.

#### Scenario: Perfil não autorizado
- GIVEN sessão de usuário de outro perfil
- WHEN solicita endpoint de desempenho
- THEN o backend nega o acesso sem revelar os dados.

### Requirement: Consulta por aluno
O sistema MUST permitir ao coordenador buscar pelo número de matrícula e período acadêmico e exibir totais exigidos, concluídos, pendentes, progresso e histórico por disciplina/tratamento.

#### Scenario: Aluno localizado
- GIVEN matrícula válida e período disponível
- WHEN o coordenador consulta
- THEN o sistema exibe resumo e histórico consolidado daquele aluno e período.

#### Scenario: Aluno não localizado
- GIVEN matrícula inexistente ou sem registros no período
- WHEN o coordenador consulta
- THEN o sistema exibe “Nenhum registro foi localizado.” ou estado vazio claramente distinguido conforme contrato, sem erro técnico.

### Requirement: Consulta por turma
O sistema MUST permitir ao coordenador buscar pelo código da turma e período e exibir consolidado da turma, comparativo de alunos e total da turma.

#### Scenario: Turma localizada
- GIVEN código de turma válida e período disponível
- WHEN o coordenador consulta
- THEN o sistema exibe indicadores da turma e comparativo compacto de cada aluno pertencente a ela, sem incluir alunos de outra turma.

#### Scenario: Turma não localizada
- GIVEN código inexistente ou turma sem registros no período
- WHEN o coordenador consulta
- THEN o sistema informa que nenhum registro foi localizado ou apresenta estado vazio, conforme contrato definido, sem dados inventados.

### Requirement: Indicadores consistentes
Exigidos, concluídos, pendentes e progresso MUST ser calculados a partir da fonte de dados e regra de domínio documentadas. Progresso MUST evitar divisão por zero. Registros cancelados, faltas e duplicados MUST ser tratados segundo regras explícitas e não podem ser contabilizados como procedimentos concluídos.

#### Scenario: Período sem conclusão
- GIVEN consulta válida sem procedimento concluído no período
- WHEN o sistema consolida
- THEN concluídos é zero e a tela apresenta estado vazio/indicadores sem valores artificiais.

#### Scenario: Meta exigida igual a zero
- GIVEN total exigido igual a zero
- WHEN o progresso é calculado
- THEN percentual é 0% e não ocorre NaN ou Infinity.

### Requirement: Protótipo e estados da interface
A interface MUST seguir a estrutura e hierarquia do protótipo: filtros, cards de indicadores, histórico por disciplina/tratamento e comparativo na visão turma. MUST apresentar loading, erro, vazio, não encontrado e resultados de forma acessível e responsiva.

#### Scenario: Alternar modalidade
- GIVEN coordenador na tela RF013
- WHEN seleciona Aluno ou Turma
- THEN o campo de identificador passa a ser interpretado como matrícula ou código da turma e os resultados seguem a modalidade.

#### Scenario: Consultar e carregar resultados
- GIVEN modalidade, identificador e período válidos
- WHEN clica em Consultar
- THEN a tela mostra loading, realiza consulta e renderiza resultados reais; não consulta automaticamente enquanto o usuário não confirmar.

### Requirement: Privacidade acadêmica
A consulta MUST apresentar somente dados acadêmicos consolidados necessários à avaliação. MUST NOT apresentar nomes de pacientes, diagnósticos, conteúdo de prontuários ou detalhes clínicos.

#### Scenario: Histórico sem dados clínicos
- GIVEN consulta por aluno ou turma
- WHEN o consolidado é exibido
- THEN somente indicadores acadêmicos, disciplinas/tratamentos, nomes/matrículas dos alunos necessários e contagens são retornados.
