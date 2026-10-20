CREATE TABLE procedimentos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE,
    especialidade VARCHAR(60)  NOT NULL,
    exige_dente   BOOLEAN NOT NULL DEFAULT TRUE,
    exige_classe  BOOLEAN NOT NULL DEFAULT FALSE,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE disciplinas (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE,
    periodo_curso INT NOT NULL CHECK (periodo_curso BETWEEN 1 AND 12),
    ativo         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE disciplina_procedimentos (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    disciplina_id      BIGINT NOT NULL REFERENCES disciplinas(id),
    procedimento_id    BIGINT NOT NULL REFERENCES procedimentos(id),
    restricao_dente    VARCHAR(10) NOT NULL DEFAULT 'QUALQUER',
    classes_permitidas VARCHAR(3)[],
    quantidade_exigida INT NOT NULL DEFAULT 0,
    ativo              BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE turmas (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id),
    semestre_letivo VARCHAR(6) NOT NULL,
    codigo_turma    VARCHAR(50) NOT NULL,
    professor_nome  VARCHAR(150),
    ativa           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE matriculas_turma (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turma_id  BIGINT NOT NULL REFERENCES turmas(id),
    aluno_id  BIGINT NOT NULL REFERENCES usuarios(id),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);

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

CREATE TABLE triagem_itens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    triagem_id      BIGINT NOT NULL REFERENCES triagens(id),
    procedimento_id BIGINT NOT NULL REFERENCES procedimentos(id),
    dente           VARCHAR(2),
    classe          VARCHAR(3),
    observacao      TEXT
);