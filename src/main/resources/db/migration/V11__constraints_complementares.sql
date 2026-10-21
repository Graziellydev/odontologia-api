-- Regras de integridade que não existiam nas tabelas criadas em V2 e V3 (já aplicadas).
-- Por isso entram aqui como ALTER TABLE, sem alterar o checksum de V1-V3.

-- Semestres: alunos do 5º ao 10º período
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_semestre_atual_check;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_semestre CHECK (semestre_atual BETWEEN 5 AND 10);
ALTER TABLE disciplinas DROP CONSTRAINT IF EXISTS disciplinas_periodo_curso_check;
ALTER TABLE disciplinas ADD CONSTRAINT ck_disciplinas_periodo CHECK (periodo_curso BETWEEN 5 AND 10);

-- Pacientes: faltas e desligamento
ALTER TABLE pacientes
    ADD CONSTRAINT ck_paciente_faltas_just CHECK (faltas_justificadas >= 0),
    ADD CONSTRAINT ck_paciente_faltas_nao_just CHECK (faltas_nao_justificadas >= 0),
    ADD CONSTRAINT ck_paciente_desligamento CHECK (
        situacao <> 'DESLIGADO' OR (data_desligamento IS NOT NULL AND motivo_desligamento IS NOT NULL)),
    ADD CONSTRAINT ck_paciente_termo CHECK (
        NOT termo_compromisso_assinado OR data_termo_compromisso IS NOT NULL);
CREATE INDEX idx_pacientes_situacao   ON pacientes (situacao);
CREATE INDEX idx_pacientes_bairro     ON pacientes (bairro);
CREATE INDEX idx_pacientes_nascimento ON pacientes (data_nascimento);
CREATE INDEX idx_pacientes_nome_trgm  ON pacientes USING gin (nome_completo gin_trgm_ops);

-- Regras por clínica / turmas / matrículas
ALTER TABLE disciplina_procedimentos
    ADD CONSTRAINT ck_disc_proc_restricao CHECK (restricao_dente IN ('QUALQUER','ANTERIOR','POSTERIOR')),
    ADD CONSTRAINT ck_disc_proc_qtd CHECK (quantidade_exigida >= 0),
    ADD CONSTRAINT uk_disc_proc UNIQUE (disciplina_id, procedimento_id);
ALTER TABLE turmas
    ADD CONSTRAINT ck_turma_semestre CHECK (semestre_letivo ~ '^[0-9]{4}\.[12]$'),
    ADD CONSTRAINT uk_turma UNIQUE (disciplina_id, semestre_letivo, codigo_turma);
ALTER TABLE matriculas_turma ADD CONSTRAINT uk_matricula UNIQUE (turma_id, aluno_id);

-- Triagem: nível de urgência (inclui PNE), tag de cor e dente/classe válidos
ALTER TABLE triagens
    ADD CONSTRAINT ck_triagem_urgencia CHECK (nivel_urgencia IN ('ALTA','MEDIA','BAIXA','PNE')),
    ADD COLUMN tag_visual_prioridade VARCHAR(10) GENERATED ALWAYS AS (
        CASE nivel_urgencia WHEN 'PNE' THEN 'AZUL' WHEN 'ALTA' THEN 'VERMELHO' WHEN 'MEDIA' THEN 'AMARELO' ELSE 'VERDE' END
    ) STORED;
ALTER TABLE triagem_itens
    ADD CONSTRAINT ck_triagem_item_dente CHECK (dente ~ '^([1-4][1-8]|[5-8][1-5])$'),
    ADD CONSTRAINT ck_triagem_item_classe CHECK (classe IN ('I','II','III','IV','V','VI'));
CREATE INDEX idx_triagens_paciente ON triagens (paciente_id);
CREATE INDEX idx_triagem_itens_triagem ON triagem_itens (triagem_id);
