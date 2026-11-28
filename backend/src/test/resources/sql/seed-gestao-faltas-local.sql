-- Dados de demonstração somente para banco local de desenvolvimento.
-- Execute depois que o Spring/Flyway tiver criado o schema local.
DO $$
DECLARE
    v_secretaria_id BIGINT;
    v_aluno_id BIGINT;
    v_paciente_id BIGINT;
    v_disciplina_id BIGINT;
    v_turma_id BIGINT;
    v_procedimento_id BIGINT;
    v_cadeira_id BIGINT;
    v_vaga_origem_id BIGINT;
    v_vaga_nova_id BIGINT;
    v_consulta_id BIGINT;
    v_data_origem DATE := CURRENT_DATE + 7;
    v_data_nova DATE := CURRENT_DATE + 8;
BEGIN
    INSERT INTO usuarios (nome, email, senha_hash, perfil, matricula_ou_registro, semestre_atual)
    VALUES ('Secretaria de Teste', 'secretaria.faltas@local.test', 'teste123', 'SECRETARIA', 'TESTE-SEC-FALTAS', 1)
    ON CONFLICT (email) DO UPDATE SET
        nome = EXCLUDED.nome,
        senha_hash = EXCLUDED.senha_hash,
        perfil = EXCLUDED.perfil,
        ativo = TRUE
    RETURNING id INTO v_secretaria_id;

    INSERT INTO usuarios (nome, email, senha_hash, perfil, matricula_ou_registro, semestre_atual)
    VALUES ('Aluno de Teste', 'aluno.faltas@local.test', 'teste123', 'ALUNO', 'TESTE-ALU-FALTAS', 5)
    ON CONFLICT (email) DO UPDATE SET
        nome = EXCLUDED.nome,
        senha_hash = EXCLUDED.senha_hash,
        perfil = EXCLUDED.perfil,
        ativo = TRUE
    RETURNING id INTO v_aluno_id;

    INSERT INTO pacientes (numero_prontuario, nome_completo, cpf, data_nascimento, telefone, bairro, cidade)
    VALUES ('TESTE-FALTAS-001', 'Paciente de Teste', '99123456789', DATE '1990-01-01', '85999990000', 'Centro', 'Fortaleza')
    ON CONFLICT (cpf) DO UPDATE SET nome_completo = EXCLUDED.nome_completo
    RETURNING id INTO v_paciente_id;

    INSERT INTO disciplinas (nome, periodo_curso)
    VALUES ('Clínica de Teste de Faltas', 5)
    ON CONFLICT (nome) DO UPDATE SET ativo = TRUE
    RETURNING id INTO v_disciplina_id;

    SELECT t.id INTO v_turma_id
    FROM turmas t
    WHERE t.disciplina_id = v_disciplina_id
      AND t.semestre_letivo = '2026.2'
      AND t.codigo_turma = 'TESTE-FALTAS'
    ORDER BY id
    LIMIT 1;
    IF v_turma_id IS NULL THEN
        INSERT INTO turmas (disciplina_id, semestre_letivo, codigo_turma, professor_nome)
        VALUES (v_disciplina_id, '2026.2', 'TESTE-FALTAS', 'Professor de Teste')
        RETURNING id INTO v_turma_id;
    END IF;

    SELECT id INTO v_procedimento_id FROM procedimentos WHERE nome = 'Restauração' LIMIT 1;
    IF v_procedimento_id IS NULL THEN
        RAISE EXCEPTION 'Procedimento Restauração não encontrado. Confira se as migrações Flyway foram executadas.';
    END IF;

    INSERT INTO cadeiras (numero, ativa)
    VALUES ('TESTE-FALTAS', TRUE)
    ON CONFLICT (numero) DO UPDATE SET ativa = TRUE
    RETURNING id INTO v_cadeira_id;

    INSERT INTO vagas (cadeira_id, data, turno, bloqueada)
    VALUES (v_cadeira_id, v_data_origem, 'MANHA', FALSE)
    ON CONFLICT (cadeira_id, data, turno) DO UPDATE SET bloqueada = FALSE
    RETURNING id INTO v_vaga_origem_id;

    LOOP
        INSERT INTO vagas (cadeira_id, data, turno, bloqueada)
        VALUES (v_cadeira_id, v_data_nova, 'TARDE', FALSE)
        ON CONFLICT (cadeira_id, data, turno) DO UPDATE SET bloqueada = FALSE
        RETURNING id INTO v_vaga_nova_id;

        EXIT WHEN NOT EXISTS (
            SELECT 1 FROM consultas
            WHERE vaga_id = v_vaga_nova_id
              AND status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
        );
        v_data_nova := v_data_nova + 1;
    END LOOP;

    SELECT c.id INTO v_consulta_id
    FROM consultas c
    WHERE c.paciente_id = v_paciente_id
      AND c.vaga_id = v_vaga_origem_id
      AND c.consulta_origem_id IS NULL
    ORDER BY c.id
    LIMIT 1;

    IF v_consulta_id IS NULL THEN
        INSERT INTO consultas (
            paciente_id, aluno_id, turma_id, procedimento_id, dente, classe,
            vaga_id, criado_por_id, status_consulta
        ) VALUES (
            v_paciente_id, v_aluno_id, v_turma_id, v_procedimento_id, '36', 'II',
            v_vaga_origem_id, v_secretaria_id, 'AGENDADO'
        ) RETURNING id INTO v_consulta_id;
    ELSE
        UPDATE consultas SET
            aluno_id = v_aluno_id,
            turma_id = v_turma_id,
            procedimento_id = v_procedimento_id,
            dente = '36',
            classe = 'II',
            criado_por_id = v_secretaria_id,
            status_consulta = 'AGENDADO',
            falta_justificada = NULL,
            justificativa_falta = NULL
        WHERE id = v_consulta_id;
    END IF;

    RAISE NOTICE 'Login secretaria: secretaria.faltas@local.test / teste123';
    RAISE NOTICE 'Consulta de demonstração: id=%', v_consulta_id;
    RAISE NOTICE 'Vaga nova livre para reagendamento: id=%', v_vaga_nova_id;
END $$;
