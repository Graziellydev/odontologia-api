-- MÓDULO CLÍNICO E PRONTUÁRIOS
-- Tabela de Prontuários (evolução clínica digital): imutável. Após salvo com a assinatura digital do aluno,
-- não pode ser editado nem apagado (trigger no V8). Correções entram como adendo.
CREATE TABLE prontuarios (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id         BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id            BIGINT NOT NULL REFERENCES usuarios(id),
    consulta_id         BIGINT NOT NULL UNIQUE REFERENCES consultas(id),
    turma_id            BIGINT NOT NULL REFERENCES turmas(id),
    procedimento_id     BIGINT NOT NULL REFERENCES procedimentos(id),
    dente               VARCHAR(2) CHECK (dente ~ '^([1-4][1-8]|[5-8][1-5])$'),
    classe              VARCHAR(3) CHECK (classe IN ('I','II','III','IV','V','VI')),
    evolucao_clinica    TEXT NOT NULL,
    assinatura_digital  TEXT NOT NULL,
    data_registro       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_prontuarios_paciente ON prontuarios (paciente_id, data_registro DESC);
CREATE INDEX idx_prontuarios_aluno    ON prontuarios (aluno_id, turma_id);

-- Tabela de Adendos do Prontuário: observações adicionais após o prontuário ter sido assinado.
CREATE TABLE prontuario_adendos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prontuario_id BIGINT NOT NULL REFERENCES prontuarios(id),
    autor_id      BIGINT NOT NULL REFERENCES usuarios(id),
    texto         TEXT NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Retiradas de Prontuário: controle da pasta de papel (quem pegou na recepção e quando devolveu).
CREATE TABLE retiradas_prontuario (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id        BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id           BIGINT NOT NULL REFERENCES usuarios(id),
    consulta_id        BIGINT REFERENCES consultas(id),
    entregue_por_id    BIGINT NOT NULL REFERENCES usuarios(id),
    retirado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    devolvido_em       TIMESTAMPTZ,
    recebido_por_id    BIGINT REFERENCES usuarios(id),
    CONSTRAINT ck_devolucao CHECK (devolvido_em IS NULL OR devolvido_em >= retirado_em)
);
-- Uma pasta física não pode estar com dois alunos ao mesmo tempo
CREATE UNIQUE INDEX uk_retirada_aberta ON retiradas_prontuario (paciente_id) WHERE devolvido_em IS NULL;
