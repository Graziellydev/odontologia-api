-- MÓDULO DE TRIAGEM E DEMANDA (FILA)
-- Tabela de Fila de Espera: A fila oficial da clínica. Junta as demandas vindas da triagem ou de pacientes
-- retornando/remarcados, ordenando por prioridade e tempo para a recepção agendar.
CREATE TABLE fila_espera (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id              BIGINT NOT NULL REFERENCES pacientes(id),
    triagem_item_id          BIGINT REFERENCES triagem_itens(id),
    origem                   VARCHAR(20) NOT NULL DEFAULT 'TRIAGEM'
        CHECK (origem IN ('TRIAGEM','RETORNO','FALTA_ALUNO','CANCELAMENTO')),
    procedimento_id          BIGINT NOT NULL REFERENCES procedimentos(id),
    dente                    VARCHAR(2) CHECK (dente ~ '^([1-4][1-8]|[5-8][1-5])$'),
    classe                   VARCHAR(3) CHECK (classe IN ('I','II','III','IV','V','VI')),
    nivel_urgencia           VARCHAR(10) NOT NULL CHECK (nivel_urgencia IN ('ALTA','MEDIA','BAIXA','PNE')),
    prioridade_reagendamento BOOLEAN NOT NULL DEFAULT FALSE,
    status_fila              VARCHAR(15) NOT NULL DEFAULT 'AGUARDANDO'
        CHECK (status_fila IN ('AGUARDANDO','ALOCADO','INATIVO')),
    observacao               TEXT,
    data_entrada_fila        TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_saida_fila          TIMESTAMPTZ,
    versao                   BIGINT NOT NULL DEFAULT 0           -- @Version
);
-- Sem duplicidade da mesma demanda aguardando (vale também para retornos, que não têm triagem)
CREATE UNIQUE INDEX uk_fila_demanda_aguardando
    ON fila_espera (paciente_id, procedimento_id, COALESCE(dente,''), COALESCE(classe,''))
    WHERE status_fila = 'AGUARDANDO';
-- Filtros e ordenação da fila (HU09)
CREATE INDEX idx_fila_filtros
    ON fila_espera (procedimento_id, dente, classe, nivel_urgencia, data_entrada_fila)
    WHERE status_fila = 'AGUARDANDO';
CREATE INDEX idx_fila_paciente ON fila_espera (paciente_id);

-- MÓDULO DE TRIAGEM E DEMANDA (FILA)
-- Tabela de Solicitações de Pacientes (pelos Alunos): o aluno pede à secretaria um paciente com perfil
-- clínico específico (ex.: dente 36, classe II) para cumprir a meta, opcionalmente para um dia específico.
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
    paciente_alocado_id       BIGINT REFERENCES pacientes(id),
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
