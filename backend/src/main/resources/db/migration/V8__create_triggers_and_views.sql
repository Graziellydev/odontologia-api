-- =========================================================================
-- TRIGGERS
-- =========================================================================
-- Atualiza "atualizado_em" automaticamente
DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['usuarios','pacientes','consultas','solicitacoes_pacientes']
    LOOP
        EXECUTE format(
            'CREATE TRIGGER trg_%1$s_atualizado_em BEFORE UPDATE ON %1$s
             FOR EACH ROW EXECUTE FUNCTION fn_set_atualizado_em()', t);
    END LOOP;
END $$;

-- Histórico automático de status da consulta. A aplicação informa o autor por transação:
--   SELECT set_config('app.usuario_id', '<id>', true), set_config('app.origem', 'SECRETARIA', true);
CREATE OR REPLACE FUNCTION fn_consulta_historico() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'INSERT' OR NEW.status_consulta IS DISTINCT FROM OLD.status_consulta THEN
        INSERT INTO consulta_historico (consulta_id, status_anterior, status_novo, alterado_por_id, origem)
        VALUES (NEW.id,
                CASE WHEN TG_OP = 'UPDATE' THEN OLD.status_consulta END,
                NEW.status_consulta,
                NULLIF(current_setting('app.usuario_id', true), '')::BIGINT,
                COALESCE(NULLIF(current_setting('app.origem', true), ''), 'SISTEMA'));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_consulta_historico
AFTER INSERT OR UPDATE OF status_consulta ON consultas
FOR EACH ROW EXECUTE FUNCTION fn_consulta_historico();

-- Prontuário e adendos: somente INSERT (assinatura digital não editável)
CREATE OR REPLACE FUNCTION fn_bloqueia_alteracao() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Registro imutável: % não permitido em %', TG_OP, TG_TABLE_NAME
        USING ERRCODE = '23000';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prontuarios_imutavel
BEFORE UPDATE OR DELETE ON prontuarios
FOR EACH ROW EXECUTE FUNCTION fn_bloqueia_alteracao();

CREATE TRIGGER trg_adendos_imutavel
BEFORE UPDATE OR DELETE ON prontuario_adendos
FOR EACH ROW EXECUTE FUNCTION fn_bloqueia_alteracao();

-- Regras por clínica: a disciplina da turma pode atender o procedimento/dente/classe pedido?
-- Dentes anteriores (FDI): incisivos e caninos (final 1, 2 ou 3)
CREATE OR REPLACE FUNCTION fn_dente_anterior(p_dente VARCHAR) RETURNS BOOLEAN AS $$
    SELECT substr(p_dente, 2, 1) IN ('1','2','3');
$$ LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION fn_procedimento_permitido(
    p_disciplina BIGINT, p_procedimento BIGINT, p_dente VARCHAR, p_classe VARCHAR
) RETURNS BOOLEAN AS $$
    SELECT EXISTS (
        SELECT 1 FROM disciplina_procedimentos dp
        WHERE dp.disciplina_id = p_disciplina
          AND dp.procedimento_id = p_procedimento
          AND dp.ativo
          AND ( dp.restricao_dente = 'QUALQUER'
             OR (dp.restricao_dente = 'ANTERIOR'  AND p_dente IS NOT NULL AND     fn_dente_anterior(p_dente))
             OR (dp.restricao_dente = 'POSTERIOR' AND p_dente IS NOT NULL AND NOT fn_dente_anterior(p_dente)) )
          AND ( dp.classes_permitidas IS NULL OR p_classe = ANY (dp.classes_permitidas) )
    );
$$ LANGUAGE sql STABLE;

CREATE OR REPLACE FUNCTION fn_valida_solicitacao() RETURNS trigger AS $$
DECLARE v_disc BIGINT;
BEGIN
    SELECT disciplina_id INTO v_disc FROM turmas WHERE id = NEW.turma_id;
    IF NOT fn_procedimento_permitido(v_disc, NEW.procedimento_id, NEW.dente, NEW.classe) THEN
        RAISE EXCEPTION 'Procedimento/dente/classe não permitido para a clínica desta turma'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_valida_solicitacao
BEFORE INSERT OR UPDATE OF turma_id, procedimento_id, dente, classe ON solicitacoes_pacientes
FOR EACH ROW EXECUTE FUNCTION fn_valida_solicitacao();

-- =========================================================================
-- VIEWS
-- =========================================================================
-- MÓDULO ACADÊMICO E DE METAS
-- Progresso de metas do aluno: exigido x concluído (prontuários assinados) e saldo pendente (HU03)
CREATE VIEW v_progresso_metas_aluno AS
SELECT m.aluno_id, t.id AS turma_id, t.semestre_letivo,
       d.id AS disciplina_id, d.nome AS disciplina,
       p.id AS procedimento_id, p.nome AS procedimento,
       dp.quantidade_exigida,
       c.qtd AS quantidade_concluida,
       GREATEST(dp.quantidade_exigida - c.qtd, 0) AS saldo_pendente
FROM matriculas_turma m
JOIN turmas t  ON t.id = m.turma_id
JOIN disciplinas d ON d.id = t.disciplina_id
JOIN disciplina_procedimentos dp ON dp.disciplina_id = d.id AND dp.ativo AND dp.quantidade_exigida > 0
JOIN procedimentos p ON p.id = dp.procedimento_id
CROSS JOIN LATERAL (
    SELECT COUNT(*) AS qtd
    FROM prontuarios pr
    WHERE pr.aluno_id = m.aluno_id AND pr.turma_id = t.id AND pr.procedimento_id = p.id
) c;

-- Vagas livres em tempo real (HU10 / HU11 / HU15)
CREATE VIEW v_vagas_disponiveis AS
SELECT v.id AS vaga_id, v.data, v.turno, ca.numero AS cadeira
FROM vagas v
JOIN cadeiras ca ON ca.id = v.cadeira_id AND ca.ativa
WHERE NOT v.bloqueada
  AND NOT EXISTS (SELECT 1 FROM consultas c
                  WHERE c.vaga_id = v.id
                    AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'));

-- RF011: atendimentos por semestre, disciplina e especialidade
CREATE VIEW v_atendimentos_semestre AS
SELECT t.semestre_letivo, d.id AS disciplina_id, d.nome AS disciplina, p.especialidade,
       COUNT(*) FILTER (WHERE c.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE')) AS total_agendados,
       COUNT(*) FILTER (WHERE c.status_consulta = 'REALIZADO') AS total_concluidos,
       COUNT(DISTINCT c.paciente_id) FILTER (WHERE c.status_consulta = 'REALIZADO') AS pacientes_atendidos
FROM consultas c
JOIN turmas t ON t.id = c.turma_id
JOIN disciplinas d ON d.id = t.disciplina_id
LEFT JOIN procedimentos p ON p.id = c.procedimento_id
GROUP BY t.semestre_letivo, d.id, d.nome, p.especialidade;

-- RF012 / HU18: demografia anonimizada (sem nome/CPF)
CREATE VIEW v_demografico_pacientes AS
SELECT bairro,
       CASE WHEN age(data_nascimento) < interval '18 years' THEN '0-17'
            WHEN age(data_nascimento) < interval '30 years' THEN '18-29'
            WHEN age(data_nascimento) < interval '45 years' THEN '30-44'
            WHEN age(data_nascimento) < interval '60 years' THEN '45-59'
            ELSE '60+' END AS faixa_etaria,
       COUNT(*) AS total
FROM pacientes
WHERE situacao <> 'EXCLUIDO'
GROUP BY 1, 2;

-- RF012 / HU19: demanda por procedimento (prioridade de insumos)
CREATE VIEW v_demanda_procedimentos AS
SELECT x.*, RANK() OVER (ORDER BY x.total_demandas DESC) AS prioridade_insumo
FROM (
    SELECT p.id AS procedimento_id, p.nome, p.especialidade,
           (SELECT COUNT(*) FROM triagem_itens ti WHERE ti.procedimento_id = p.id) AS total_demandas,
           (SELECT COUNT(*) FROM prontuarios pr WHERE pr.procedimento_id = p.id)  AS total_realizados
    FROM procedimentos p
) x;

-- RF014 / HU21: tempo médio entre a triagem e o 1º atendimento (alerta vermelho > 30 dias)
CREATE VIEW v_tempo_espera_especialidade AS
SELECT p.especialidade,
       COUNT(*) AS amostra,
       ROUND(AVG(pr.data_atendimento - tri.data_triagem::date), 2) AS tempo_medio_espera_dias,
       AVG(pr.data_atendimento - tri.data_triagem::date) > 30 AS alerta_vermelho
FROM (
    SELECT fe.triagem_item_id, MIN(v.data) AS data_atendimento
    FROM consultas c
    JOIN vagas v ON v.id = c.vaga_id
    JOIN fila_espera fe ON fe.id = c.fila_espera_id
    WHERE c.status_consulta = 'REALIZADO' AND fe.triagem_item_id IS NOT NULL
    GROUP BY fe.triagem_item_id
) pr
JOIN triagem_itens ti ON ti.id = pr.triagem_item_id
JOIN triagens tri ON tri.id = ti.triagem_id
JOIN procedimentos p ON p.id = ti.procedimento_id
GROUP BY p.especialidade;

-- RF015 / HU22: satisfação (sem identificar o paciente)
CREATE VIEW v_satisfacao_semestre AS
SELECT t.semestre_letivo,
       COUNT(a.nota) AS total_respostas,
       ROUND(AVG(a.nota), 2) AS nota_media
FROM avaliacoes_satisfacao a
JOIN consultas c ON c.id = a.consulta_id
JOIN turmas t ON t.id = c.turma_id
WHERE a.nota IS NOT NULL
GROUP BY t.semestre_letivo;

CREATE VIEW v_comentarios_satisfacao AS
SELECT t.semestre_letivo, a.nota, a.comentario, a.data_resposta
FROM avaliacoes_satisfacao a
JOIN consultas c ON c.id = a.consulta_id
JOIN turmas t ON t.id = c.turma_id
WHERE a.comentario IS NOT NULL;
