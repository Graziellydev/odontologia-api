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
    motivo_bloqueio  VARCHAR(200)
);

-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Consultas:Serve para formalizar o agendamento, ligando o paciente, aluno, cadeira, tratamento e gerenciar a liberação da pasta física de prontuário.
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
    
    prontuario_liberado   BOOLEAN NOT NULL DEFAULT FALSE, 
    
    falta_justificada     BOOLEAN,
    justificativa_falta   TEXT,
    retorno_sugerido      BOOLEAN NOT NULL DEFAULT FALSE,
    justificativa_retorno TEXT,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0
);