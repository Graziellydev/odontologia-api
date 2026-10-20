-- MÓDULO CLÍNICO E PRONTUÁRIOS
-- Tabela de Prontuários:Serve para registrar a evolução clínica textual de cada consulta e manter a assinatura digital imutável do aluno responsável.
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

-- MÓDULO CLÍNICO E PRONTUÁRIOS
-- Tabela de Adendos do Prontuário:Serve para inserir observações adicionais a um prontuário após o mesmo já ter sido fechado/assinado, preservando o histórico oficial.
CREATE TABLE prontuario_adendos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    prontuario_id BIGINT NOT NULL REFERENCES prontuarios(id),
    autor_id      BIGINT NOT NULL REFERENCES usuarios(id),
    texto         TEXT NOT NULL,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- MÓDULO CLÍNICO E PRONTUÁRIOS
-- Tabela de Retiradas de Prontuário:Serve para controlar fisicamente as pastas de papel dos pacientes, registrando quando o aluno pegou e quando devolveu na recepção.
CREATE TABLE retiradas_prontuario (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id        BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id           BIGINT NOT NULL REFERENCES usuarios(id),
    consulta_id        BIGINT REFERENCES consultas(id),
    entregue_por_id    BIGINT NOT NULL REFERENCES usuarios(id),
    retirado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    devolvido_em       TIMESTAMPTZ,
    recebido_por_id    BIGINT REFERENCES usuarios(id)
);