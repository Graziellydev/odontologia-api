# Spec Delta

## Purpose

Provisiona um conjunto fixo e repetível de contas iniciais do Odonto_Uni no PostgreSQL, cobrindo Secretaria, Aluno e Coordenador, com hashes de senha aceitos pelo mecanismo de autenticação.

## ADDED Requirements

### Requirement: Conjunto inicial de usuários
V19 SHALL provisionar as 13 identidades definidas nesta especificação, com perfil, nome, e-mail e matrícula correspondentes; novas contas SHALL estar ativas e usar hashes BCrypt das senhas de teste designadas por perfil.

| Perfil | Nome | E-mail | Matrícula | Senha de teste |
|---|---|---|---|---|
| SECRETARIA | Ana Paula Martins | `secretaria1@unicatolica.edu.br` | `SEC202601` | `Sec@2026` |
| SECRETARIA | Beatriz Costa Lima | `secretaria2@unicatolica.edu.br` | `SEC202602` | `Sec@2026` |
| ALUNO | Mariana Costa Silva | `mariana.costa@aluno.unicatolica.edu.br` | `20241001` | `Aluno@2026` |
| ALUNO | Lucas Mendes Oliveira | `lucas.mendes@aluno.unicatolica.edu.br` | `20241002` | `Aluno@2026` |
| ALUNO | Gabriel Santos Rocha | `gabriel.santos@aluno.unicatolica.edu.br` | `20241003` | `Aluno@2026` |
| ALUNO | Camila Ferreira Alves | `camila.ferreira@aluno.unicatolica.edu.br` | `20241004` | `Aluno@2026` |
| ALUNO | Pedro Henrique Sousa | `pedro.sousa@aluno.unicatolica.edu.br` | `20241005` | `Aluno@2026` |
| ALUNO | Larissa Almeida Nogueira | `larissa.almeida@aluno.unicatolica.edu.br` | `20241006` | `Aluno@2026` |
| ALUNO | Rafael Lima Teixeira | `rafael.lima@aluno.unicatolica.edu.br` | `20241007` | `Aluno@2026` |
| ALUNO | Juliana Ribeiro Castro | `juliana.ribeiro@aluno.unicatolica.edu.br` | `20241008` | `Aluno@2026` |
| ALUNO | Mateus Cavalcante Dias | `mateus.cavalcante@aluno.unicatolica.edu.br` | `20241009` | `Aluno@2026` |
| ALUNO | Isabela Gomes Pereira | `isabela.gomes@aluno.unicatolica.edu.br` | `20241010` | `Aluno@2026` |
| COORDENADOR | Prof. Dr. Ricardo Vasconcelos | `coordenacao.odonto@unicatolica.edu.br` | `COORD202601` | `Coord@2026` |

#### Scenario: Primeira aplicação da migration
- **WHEN** V19 é aplicada em um banco sem essas identidades
- **THEN** ficam persistidos exatamente dois usuários SECRETARIA, dez ALUNO e um COORDENADOR com os dados e hashes definidos

#### Scenario: Verificação das senhas
- **WHEN** a validação compara cada senha de teste à coluna `senha_hash` pelo codificador BCrypt da aplicação
- **THEN** cada senha correspondente é aceita e nenhuma senha é armazenada em texto puro

### Requirement: Aplicação idempotente sem sobrescrever contas
A inserção de cada identidade SHALL usar `ON CONFLICT DO NOTHING` para preservar usuários existentes e evitar duplicação por e-mail ou matrícula; a validação posterior SHALL detectar qualquer registro conflitante cujos dados não correspondam ao conjunto definido.

#### Scenario: Reaplicação sem alterações
- **WHEN** V19 é executada novamente após a carga inicial
- **THEN** não são criadas duplicatas e os dados existentes não são atualizados

#### Scenario: Identidade preexistente divergente
- **WHEN** e-mail ou matrícula de uma identidade já estiver associado a dados diferentes
- **THEN** a carga não sobrescreve essa conta e a verificação informa divergência em vez de declarar sucesso com 13 correspondências
