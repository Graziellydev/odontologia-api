-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Cadeiras:Serve para cadastrar os equipamentos e os boxes de atendimento físicos disponíveis no prédio da clínica.
CREATE TABLE cadeiras (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    ativa  BOOLEAN NOT NULL DEFAULT TRUE
);

-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Vagas:Serve para criar a grade de horários disponíveis em cada cadeira, por data e turno, evitando choques de agendamento.
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

-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Consultas:Serve para formalizar o agendamento, ligando o paciente, aluno, cadeira, tratamento e o status de comparecimento.
CREATE TABLE consultas (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id           VARCHAR(30) NOT NULL REFERENCES pacientes(numero_prontuario) ON UPDATE CASCADE,
    aluno_id              BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id              BIGINT NOT NULL REFERENCES turmas(id),
    tratamento_id         BIGINT REFERENCES tratamentos(id),
    vaga_id               BIGINT NOT NULL REFERENCES vagas(id),
    fila_espera_id        BIGINT REFERENCES fila_espera(id),
    consulta_origem_id    BIGINT REFERENCES consultas(id),     
    criado_por_id         BIGINT REFERENCES usuarios(id),     
    status_consulta       VARCHAR(25) NOT NULL DEFAULT 'AGENDADO' CHECK (status_consulta IN (
        'AGENDADO','CONFIRMADA_24H','CANCELADO_SECRETARIA','CANCELADO_PACIENTE',
        'REALIZADO','FALTOU_PACIENTE','FALTOU_ALUNO')),
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

CREATE UNIQUE INDEX uk_consulta_vaga_ativa ON consultas (vaga_id)
    WHERE status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE');
CREATE INDEX idx_consultas_paciente ON consultas (paciente_id, status_consulta);
CREATE INDEX idx_consultas_aluno    ON consultas (aluno_id, status_consulta);
CREATE INDEX idx_consultas_turma    ON consultas (turma_id);
CREATE INDEX idx_consultas_trat     ON consultas (tratamento_id);

-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Histórico de Consulta:Serve para registrar a linha do tempo de alterações de status de uma consulta, identificando os autores das mudanças.
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

-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Transferências de Paciente:Serve para gerenciar a passagem formal de um paciente de um aluno para outro visando dar continuidade a tratamentos.
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

-- MÓDULO DE TRIAGEM E DEMANDA
-- Tabela de Solicitações de Pacientes:Serve para os alunos solicitarem à secretaria que puxe da fila um paciente com perfil clínico específico para as metas acadêmicas.
CREATE TABLE solicitacoes_pacientes (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aluno_id                  BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id                  BIGINT NOT NULL REFERENCES turmas(id),
    procedimento_id           BIGINT NOT NULL REFERENCES procedimentos(id),
    dente                     VARCHAR(2) NOT NULL CHECK (dente ~ '^([1-4][1-8]|[5-8][1-5])$'),
    classe                    VARCHAR(3) CHECK (classe IN ('I','II','III','IV','V','VI')),
    data_desejada             DATE,        -- ex.: "paciente para o atendimento de quinta"
    status_solicitacao        VARCHAR(25) NOT NULL DEFAULT 'PENDENTE_APROVACAO'
        CHECK (status_solicitacao IN ('PENDENTE_APROVACAO','PENDENTE_FILA','APROVADO_E_ALOCADO','RECUSADO')),
    fila_espera_id            BIGINT REFERENCES fila_espera(id),
    paciente_alocado_id       VARCHAR(30) REFERENCES pacientes(numero_prontuario) ON UPDATE CASCADE,
    secretaria_analisador_id  BIGINT REFERENCES usuarios(id),
    data_analise              TIMESTAMPTZ,
    observacao_secretaria     TEXT,
    data_solicitacao          TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                    BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_solic_alocado CHECK (
        status_solicitacao <> 'APROVADO_E_ALOCADO' OR paciente_alocado_id IS NOT NULL),
    CONSTRAINT ck_solic_recusa CHECK (
        status_solicitacao <> 'RECUSADO' OR observacao_secretaria IS NOT NULL)
);
CREATE INDEX idx_solic_status ON solicitacoes_pacientes (status_solicitacao, data_solicitacao);
CREATE INDEX idx_solic_aluno  ON solicitacoes_pacientes (aluno_id);