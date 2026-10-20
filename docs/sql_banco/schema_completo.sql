CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE OR REPLACE FUNCTION fn_set_atualizado_em() RETURNS trigger AS $$
BEGIN
    NEW.atualizado_em = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE usuarios (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                  VARCHAR(150) NOT NULL,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    senha_hash            VARCHAR(255) NOT NULL,
    perfil                VARCHAR(30)  NOT NULL CHECK (perfil IN ('ALUNO','COORDENADOR','SECRETARIA')),
    matricula_ou_registro VARCHAR(50)  UNIQUE,
    semestre_atual        INT CHECK (semestre_atual BETWEEN 1 AND 12),
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE pacientes (
    id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_prontuario          VARCHAR(30) UNIQUE,
    nome_completo              VARCHAR(150) NOT NULL,
    cpf                        VARCHAR(11)  NOT NULL UNIQUE CHECK (cpf ~ '^[0-9]{11}$'),
    data_nascimento            DATE         NOT NULL,
    telefone                   VARCHAR(20)  NOT NULL,
    telefone_alternativo       VARCHAR(20),
    email                      VARCHAR(150),
    cep                        VARCHAR(9),
    logradouro                 VARCHAR(150),
    numero_endereco            VARCHAR(20),
    complemento                VARCHAR(100),
    bairro                     VARCHAR(100) NOT NULL,
    cidade                     VARCHAR(100) NOT NULL,
    uf                         CHAR(2),
    codigo_acesso              VARCHAR(20) UNIQUE,
    mora_proximo               BOOLEAN NOT NULL DEFAULT FALSE,
    aceita_chamada_ultima_hora BOOLEAN NOT NULL DEFAULT FALSE,
    disponibilidade_horario    VARCHAR(100),
    observacao                 TEXT,
    id_externo                 VARCHAR(50),
    situacao                   VARCHAR(15) NOT NULL DEFAULT 'ATIVO' CHECK (situacao IN ('ATIVO','DESLIGADO','EXCLUIDO')),
    termo_compromisso_assinado BOOLEAN NOT NULL DEFAULT FALSE,
    data_termo_compromisso     TIMESTAMPTZ,
    faltas_justificadas        INT NOT NULL DEFAULT 0,
    faltas_nao_justificadas    INT NOT NULL DEFAULT 0,
    motivo_desligamento        TEXT,
    data_desligamento          TIMESTAMPTZ,
    desligado_por_id           BIGINT REFERENCES usuarios(id),
    rg                         VARCHAR(30),
    orgao_expedidor            VARCHAR(20),
    sexo                       CHAR(1) CHECK (sexo IN ('M', 'F')),
    raca_cor                   VARCHAR(20) CHECK (raca_cor IN ('BRANCA','PRETA','AMARELA','PARDA','INDIGENA','NAO_DECLARADO')),
    local_moradia              VARCHAR(15) CHECK (local_moradia IN ('RURAL', 'URBANA')),
    tipo_escola                VARCHAR(20) CHECK (tipo_escola IN ('PUBLICA', 'PRIVADA', 'NUNCA_ESTUDOU')),
    escolaridade_paciente      VARCHAR(40),
    escolaridade_mae           VARCHAR(40),
    escolaridade_pai           VARCHAR(40),
    renda_familiar             VARCHAR(30),
    criado_em                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em              TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE logs_auditoria (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT REFERENCES usuarios(id),
    acao        VARCHAR(100) NOT NULL,
    entidade    VARCHAR(60),
    entidade_id BIGINT,
    detalhes    TEXT,
    ip_origem   VARCHAR(45),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

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

CREATE TABLE tratamentos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id     BIGINT NOT NULL REFERENCES pacientes(id),
    triagem_item_id BIGINT REFERENCES triagem_itens(id),
    procedimento_id BIGINT NOT NULL REFERENCES procedimentos(id),
    dente           VARCHAR(2),
    classe          VARCHAR(3),
    status          VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
    data_inicio     TIMESTAMPTZ,
    data_conclusao  TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE fila_espera (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id              BIGINT NOT NULL REFERENCES pacientes(id),
    triagem_item_id          BIGINT REFERENCES triagem_itens(id),
    tratamento_id            BIGINT REFERENCES tratamentos(id),
    origem                   VARCHAR(20) NOT NULL DEFAULT 'TRIAGEM',
    procedimento_id          BIGINT NOT NULL REFERENCES procedimentos(id),
    dente                    VARCHAR(2),
    classe                   VARCHAR(3),
    nivel_urgencia           VARCHAR(10) NOT NULL,
    prioridade_reagendamento BOOLEAN NOT NULL DEFAULT FALSE,
    status_fila              VARCHAR(15) NOT NULL DEFAULT 'AGUARDANDO',
    observacao               TEXT,
    data_entrada_fila        TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_saida_fila          TIMESTAMPTZ,
    versao                   BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE cadeiras (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    ativa  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE vagas (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cadeira_id       BIGINT NOT NULL REFERENCES cadeiras(id),
    data             DATE NOT NULL,
    turno            VARCHAR(10) NOT NULL CHECK (turno IN ('MANHA','TARDE')),
    bloqueada        BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_bloqueio  VARCHAR(200)
);

CREATE TABLE consultas (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id           BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id              BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id              BIGINT NOT NULL REFERENCES turmas(id),
    tratamento_id         BIGINT REFERENCES tratamentos(id),
    vaga_id               BIGINT NOT NULL REFERENCES vagas(id),
    fila_espera_id        BIGINT REFERENCES fila_espera(id),
    consulta_origem_id    BIGINT REFERENCES consultas(id),
    criado_por_id         BIGINT REFERENCES usuarios(id),
    status_consulta       VARCHAR(25) NOT NULL DEFAULT 'AGENDADO',
    falta_justificada     BOOLEAN,
    justificativa_falta   TEXT,
    retorno_sugerido      BOOLEAN NOT NULL DEFAULT FALSE,
    justificativa_retorno TEXT,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE solicitacoes_pacientes (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aluno_id                  BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id                  BIGINT NOT NULL REFERENCES turmas(id),
    procedimento_id           BIGINT NOT NULL REFERENCES procedimentos(id),
    dente                     VARCHAR(2) NOT NULL,
    classe                    VARCHAR(3),
    data_desejada             DATE,
    status_solicitacao        VARCHAR(25) NOT NULL DEFAULT 'PENDENTE_APROVACAO',
    fila_espera_id            BIGINT REFERENCES fila_espera(id),
    paciente_alocado_id       BIGINT REFERENCES pacientes(id),
    secretaria_analisador_id  BIGINT REFERENCES usuarios(id),
    data_analise              TIMESTAMPTZ,
    observacao_secretaria     TEXT,
    data_solicitacao          TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                    BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE prontuarios (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id         BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id            BIGINT NOT NULL REFERENCES usuarios(id),
    consulta_id         BIGINT NOT NULL UNIQUE REFERENCES consultas(id),
    turma_id            BIGINT NOT NULL REFERENCES turmas(id),
    tratamento_id       BIGINT REFERENCES tratamentos(id),
    procedimento_id     BIGINT NOT NULL REFERENCES procedimentos(id),
    dente               VARCHAR(2),
    classe              VARCHAR(3),
    evolucao_clinica    TEXT NOT NULL,
    assinatura_digital  TEXT NOT NULL,
    data_registro       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE paciente_anamnese (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    queixa_principal        TEXT,
    historia_queixa         TEXT,
    respostas_questionario  JSONB NOT NULL DEFAULT '{}'::jsonb,
    pressao_arterial        VARCHAR(20),
    pulso_bpm               INT,
    temperatura_c           NUMERIC(4,1),
    frequencia_respiratoria INT,
    medo_dentista_escala    INT,
    frequencia_escovacao    INT,
    usa_fio_dental          BOOLEAN DEFAULT FALSE,
    usa_enxaguante          BOOLEAN DEFAULT FALSE,
    limpa_lingua            BOOLEAN DEFAULT FALSE,
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE paciente_exames_clinicos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    consulta_id             BIGINT REFERENCES consultas(id),
    tem_edema               BOOLEAN DEFAULT FALSE,
    tem_dor_muscular        BOOLEAN DEFAULT FALSE,
    tem_tumefacao           BOOLEAN DEFAULT FALSE,
    linfonodos_palpaveis    BOOLEAN DEFAULT FALSE,
    tem_assimetria          BOOLEAN DEFAULT FALSE,
    atm_estalido            BOOLEAN DEFAULT FALSE,
    atm_dor_palpacao        BOOLEAN DEFAULT FALSE,
    atm_desvio_abertura     BOOLEAN DEFAULT FALSE,
    descricao_lesao_forma   VARCHAR(100),
    lesao_implantacao       VARCHAR(100),
    lesao_coloracao         VARCHAR(50),
    lesao_superficie        VARCHAR(100),
    lesao_consistencia      VARCHAR(100),
    lesao_tamanho           VARCHAR(50),
    lesao_localizacao       VARCHAR(150),
    hipotese_diagnostica    TEXT,
    psr_sextante_1          VARCHAR(2),
    psr_sextante_2          VARCHAR(2),
    psr_sextante_3          VARCHAR(2),
    psr_sextante_4          VARCHAR(2),
    psr_sextante_5          VARCHAR(2),
    psr_sextante_6          VARCHAR(2),
    registrado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE paciente_odontograma (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    dente                   VARCHAR(2) NOT NULL,
    face_dente              VARCHAR(10),
    condicao_dente          VARCHAR(50) NOT NULL,
    observacao              TEXT,
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);