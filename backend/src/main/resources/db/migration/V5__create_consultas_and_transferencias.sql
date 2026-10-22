-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Cadeiras Físicas: o cadastro das cadeiras/equipamentos existentes no prédio da clínica.
CREATE TABLE cadeiras (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    ativa  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela de Vagas de Agenda: slots de tempo (ex.: Cadeira 01, dia 20, turno MANHÃ). Evita duplo agendamento.
CREATE TABLE vagas (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cadeira_id       BIGINT NOT NULL REFERENCES cadeiras(id),
    data             DATE NOT NULL,
    turno            VARCHAR(10) NOT NULL CHECK (turno IN ('MANHA','TARDE')),
    bloqueada        BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_bloqueio  VARCHAR(200),
    CONSTRAINT uk_vaga UNIQUE (cadeira_id, data, turno)
);
CREATE INDEX idx_vagas_data_turno ON vagas (data, turno);

-- Tabela de Consultas: o coração do agendamento. Conecta paciente, aluno, turma, procedimento e vaga.
-- Controla status, faltas, retorno sugerido e a liberação do prontuário (a recepção entrega a pasta física).
CREATE TABLE consultas (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id           BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id              BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id              BIGINT NOT NULL REFERENCES turmas(id),
    procedimento_id       BIGINT REFERENCES procedimentos(id),
    dente                 VARCHAR(2) CHECK (dente ~ '^([1-4][1-8]|[5-8][1-5])$'),
    classe                VARCHAR(3) CHECK (classe IN ('I','II','III','IV','V','VI')),
    vaga_id               BIGINT NOT NULL REFERENCES vagas(id),
    fila_espera_id        BIGINT REFERENCES fila_espera(id),
    consulta_origem_id    BIGINT REFERENCES consultas(id),     -- remarcação
    criado_por_id         BIGINT REFERENCES usuarios(id),      -- secretaria
    status_consulta       VARCHAR(25) NOT NULL DEFAULT 'AGENDADO' CHECK (status_consulta IN (
        'AGENDADO','CONFIRMADA_24H','CANCELADO_SECRETARIA','CANCELADO_PACIENTE',
        'REALIZADO','FALTOU_PACIENTE','FALTOU_ALUNO')),
    prontuario_liberado   BOOLEAN NOT NULL DEFAULT FALSE,
    falta_justificada     BOOLEAN,
    justificativa_falta   TEXT,
    retorno_sugerido      BOOLEAN NOT NULL DEFAULT FALSE,
    justificativa_retorno TEXT,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0,           -- @Version
    CONSTRAINT ck_falta_so_paciente CHECK (falta_justificada IS NULL OR status_consulta = 'FALTOU_PACIENTE'),
    CONSTRAINT ck_falta_justificativa CHECK (falta_justificada IS NOT TRUE OR justificativa_falta IS NOT NULL),
    CONSTRAINT ck_retorno_justificativa CHECK (NOT retorno_sugerido OR justificativa_retorno IS NOT NULL)
);
-- Impede agendamento duplo da mesma vaga (RNF004)
CREATE UNIQUE INDEX uk_consulta_vaga_ativa ON consultas (vaga_id)
    WHERE status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE');
CREATE INDEX idx_consultas_paciente ON consultas (paciente_id, status_consulta);
CREATE INDEX idx_consultas_aluno    ON consultas (aluno_id, status_consulta);
CREATE INDEX idx_consultas_turma    ON consultas (turma_id);
CREATE INDEX idx_consultas_proc     ON consultas (procedimento_id);

-- Tabela de Histórico de Consulta: linha do tempo das mudanças de status (preenchida por trigger no V8).
CREATE TABLE consulta_historico (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consulta_id     BIGINT NOT NULL REFERENCES consultas(id),
    status_anterior VARCHAR(25),
    status_novo     VARCHAR(25) NOT NULL,
    alterado_por_id BIGINT REFERENCES usuarios(id),
    origem          VARCHAR(20) NOT NULL DEFAULT 'SISTEMA'
        CHECK (origem IN ('SECRETARIA','ALUNO','PACIENTE_WHATSAPP','SISTEMA')),
    observacao      TEXT,
    alterado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_consulta_hist ON consulta_historico (consulta_id, alterado_em);

-- Tabela de Transferências de Paciente: passagem de um paciente de um aluno para outro quando o aluno falta.
CREATE TABLE transferencias_paciente (
    id                     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consulta_origem_id     BIGINT NOT NULL REFERENCES consultas(id),
    aluno_origem_id        BIGINT NOT NULL REFERENCES usuarios(id),
    aluno_destino_id       BIGINT REFERENCES usuarios(id),
    consulta_destino_id    BIGINT REFERENCES consultas(id),
    status                 VARCHAR(25) NOT NULL DEFAULT 'PENDENTE_AUTORIZACAO'
        CHECK (status IN ('PENDENTE_AUTORIZACAO','AUTORIZADA','NEGADA','SEM_RESPOSTA','CONCLUIDA')),
    canal_autorizacao      VARCHAR(10) CHECK (canal_autorizacao IN ('WHATSAPP','SISTEMA')),
    solicitado_por_id      BIGINT REFERENCES usuarios(id),
    solicitado_em          TIMESTAMPTZ NOT NULL DEFAULT now(),
    respondido_em          TIMESTAMPTZ
);
CREATE INDEX idx_transf_origem ON transferencias_paciente (consulta_origem_id);
