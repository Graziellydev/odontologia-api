DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['usuarios','pacientes','tratamentos','consultas','solicitacoes_pacientes']
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%1$s_atualizado_em BEFORE UPDATE ON %1$s
             FOR EACH ROW EXECUTE FUNCTION fn_set_atualizado_em()', t);
    END LOOP;
END $$;

-- MÓDULO ACADÊMICO E DE METAS
-- View de Progresso de Metas do Aluno :Serve para consolidar um painel que calcula, em tempo real, as exigências das disciplinas e os procedimentos que o aluno já completou.
CREATE VIEW v_progresso_metas_aluno AS
SELECT m.aluno_id, t.id AS turma_id, t.semestre_letivo,
       d.id AS disciplina_id, d.nome AS disciplina,
       p.id AS procedimento_id, p.nome AS procedimento,
       dp.quantidade_exigida,
       (SELECT COUNT(*) FROM prontuarios pr 
        WHERE pr.aluno_id = m.aluno_id AND pr.turma_id = t.id AND pr.procedimento_id = p.id) AS quantidade_concluida
FROM matriculas_turma m
JOIN turmas t  ON t.id = m.turma_id
JOIN disciplinas d ON d.id = t.disciplina_id
JOIN disciplina_procedimentos dp ON dp.disciplina_id = d.id AND dp.ativo AND dp.quantidade_exigida > 0
JOIN procedimentos p ON p.id = dp.procedimento_id;