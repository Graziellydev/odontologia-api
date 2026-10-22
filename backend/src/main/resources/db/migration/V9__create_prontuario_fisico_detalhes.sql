-- Dados da ficha física (socioeconômicos / identificação) na tabela de pacientes
ALTER TABLE pacientes
    ADD COLUMN IF NOT EXISTS rg VARCHAR(30),
    ADD COLUMN IF NOT EXISTS orgao_expedidor VARCHAR(20),
    ADD COLUMN IF NOT EXISTS sexo CHAR(1) CHECK (sexo IN ('M', 'F')),
    ADD COLUMN IF NOT EXISTS raca_cor VARCHAR(20) CHECK (raca_cor IN ('BRANCA','PRETA','AMARELA','PARDA','INDIGENA','NAO_DECLARADO')),
    ADD COLUMN IF NOT EXISTS local_moradia VARCHAR(15) CHECK (local_moradia IN ('RURAL', 'URBANA')),
    ADD COLUMN IF NOT EXISTS tipo_escola VARCHAR(20) CHECK (tipo_escola IN ('PUBLICA', 'PRIVADA', 'NUNCA_ESTUDOU')),
    ADD COLUMN IF NOT EXISTS escolaridade_paciente VARCHAR(40),
    ADD COLUMN IF NOT EXISTS escolaridade_mae VARCHAR(40),
    ADD COLUMN IF NOT EXISTS escolaridade_pai VARCHAR(40),
    ADD COLUMN IF NOT EXISTS renda_familiar VARCHAR(30);

-- MÓDULO CLÍNICO E PRONTUÁRIOS
-- Tabela de Anamnese do Paciente: questionário de saúde e sinais vitais (pressão, pulso, alergias...).
-- Sem ON DELETE CASCADE: dado clínico não é apagado em cascata (paciente só é desligado/excluído logicamente).
CREATE TABLE IF NOT EXISTS paciente_anamnese (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id),
    queixa_principal        TEXT,
    historia_queixa         TEXT,
    respostas_questionario  JSONB NOT NULL DEFAULT '{}'::jsonb,
    pressao_arterial        VARCHAR(20),
    pulso_bpm               INT,
    temperatura_c           NUMERIC(4,1),
    frequencia_respiratoria INT,
    medo_dentista_escala    INT CHECK (medo_dentista_escala BETWEEN 0 AND 10),
    frequencia_escovacao    INT,
    usa_fio_dental          BOOLEAN DEFAULT FALSE,
    usa_enxaguante          BOOLEAN DEFAULT FALSE,
    limpa_lingua            BOOLEAN DEFAULT FALSE,
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_anamnese_paciente ON paciente_anamnese (paciente_id);

-- Tabela de Exames Clínicos Extra/Intraorais e PSR: assimetrias, ATM, lesões e notas do PSR (gengiva).
CREATE TABLE IF NOT EXISTS paciente_exames_clinicos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id),
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
CREATE INDEX IF NOT EXISTS idx_exames_paciente ON paciente_exames_clinicos (paciente_id);

-- Tabela de Odontograma: estado de cada dente/face do paciente (saudável, cariado, restaurado, ausente...).
CREATE TABLE IF NOT EXISTS paciente_odontograma (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id),
    dente                   VARCHAR(2) NOT NULL CHECK (dente ~ '^([1-4][1-8]|[5-8][1-5])$'),
    face_dente              VARCHAR(10),
    condicao_dente          VARCHAR(50) NOT NULL,
    observacao              TEXT,
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- (UNIQUE com expressão não é permitido em CONSTRAINT; por isso é um índice único)
CREATE UNIQUE INDEX IF NOT EXISTS uk_paciente_dente_face
    ON paciente_odontograma (paciente_id, dente, COALESCE(face_dente, 'GERAL'));

CREATE TRIGGER trg_paciente_anamnese_atualizado_em BEFORE UPDATE ON paciente_anamnese
    FOR EACH ROW EXECUTE FUNCTION fn_set_atualizado_em();
CREATE TRIGGER trg_paciente_odontograma_atualizado_em BEFORE UPDATE ON paciente_odontograma
    FOR EACH ROW EXECUTE FUNCTION fn_set_atualizado_em();
