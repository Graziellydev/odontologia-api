DO $$
BEGIN
    IF EXISTS (
        SELECT consulta_id
        FROM notificacoes_whatsapp
        WHERE tipo_mensagem = 'PESQUISA_SATISFACAO' AND consulta_id IS NOT NULL
        GROUP BY consulta_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate PESQUISA_SATISFACAO records exist; review data before applying V18';
    END IF;
END $$;

CREATE UNIQUE INDEX uk_notificacoes_satisfacao_consulta
    ON notificacoes_whatsapp (consulta_id)
    WHERE tipo_mensagem = 'PESQUISA_SATISFACAO' AND consulta_id IS NOT NULL;
