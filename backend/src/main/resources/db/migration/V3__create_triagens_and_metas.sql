-- MÓDULO ACADÊMICO E DE METAS
-- Tabela de Procedimentos:Serve para catalogar os procedimentos odontológicos disponíveis na clínica, suas especialidades e exigências técnicas.
CREATE TABLE procedimentos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE,
    especialidade VARCHAR(60)  NOT NULL,
    exige_dente   BOOLEAN NOT NULL DEFAULT TRUE,
    exige_classe  BOOLEAN NOT NULL DEFAULT FALSE,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE
);

-- MÓDULO ACADÊMICO E DE METAS
-- Tabela de Disciplinas:Serve para cadastrar as matérias clínicas ofertadas no curso de Odontologia, associadas ao período do curso.
CREATE TABLE disciplinas (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE,
    periodo_curso INT NOT NULL CHECK (periodo_curso BETWEEN 1 AND 12),
    ativo         BOOLEAN NOT NULL DEFAULT TRUE
);

-- MÓDULO ACADÊMICO E DE METAS
-- Tabela de Disciplina Procedimentos:Serve para definir as metas dos alunos, vinculando quais procedimentos e quantidades são exigidos em cada disciplina.
CREATE TABLE disciplina_procedimentos (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    disciplina_id      BIGINT NOT NULL REFERENCES disciplinas(id),
    procedimento_id    BIGINT NOT NULL REFERENCES procedimentos(id),
    restricao_dente    VARCHAR(10) NOT NULL DEFAULT 'QUALQUER',
    classes_permitidas VARCHAR(3)[],
    quantidade_exigida INT NOT NULL DEFAULT 0,
    ativo              BOOLEAN NOT NULL DEFAULT TRUE
);

-- MÓDULO ACADÊMICO E DE METAS
-- Tabela de Turmas:Serve para organizar as disciplinas oferecidas em um semestre letivo específico e relacioná-las a um professor.
CREATE TABLE turmas (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id),
    semestre_letivo VARCHAR(6) NOT NULL,
    codigo_turma    VARCHAR(50) NOT NULL,
    professor_nome  VARCHAR(150),
    ativa           BOOLEAN NOT NULL DEFAULT TRUE
);

-- MÓDULO ACADÊMICO E DE METAS
-- Tabela de Matrículas da Turma:Serve para associar quais alunos estão matriculados em quais turmas clínicas no semestre.
CREATE TABLE matriculas_turma (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turma_id  BIGINT NOT NULL REFERENCES turmas(id),
    aluno_id  BIGINT NOT NULL REFERENCES usuarios(id),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- MÓDULO DE TRIAGEM E DEMANDA
-- Tabela de Triagens:Serve para registrar o acolhimento inicial do paciente na clínica, definindo seu nível de urgência geral.
CREATE TABLE triagens (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id               BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_responsavel_id      BIGINT REFERENCES usuarios(id),
    professor_supervisor_nome VARCHAR(150),
    secretaria_lancamento_id  BIGINT REFERENCES usuarios(id),
    nivel_urgencia            VARCHAR(10) NOT NULL,
    necessidades_clinicas     TEXT NOT NULL,
    data_triagem              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- MÓDULO DE TRIAGEM E DEMANDA
-- Tabela de Itens da Triagem:Serve para detalhar os problemas odontológicos específicos levantados durante a triagem inicial do paciente.
CREATE TABLE triagem_itens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    triagem_id      BIGINT NOT NULL REFERENCES triagens(id),
    procedimento_id BIGINT NOT NULL REFERENCES procedimentos(id),
    dente           VARCHAR(2),
    classe          VARCHAR(3),
    observacao      TEXT
);