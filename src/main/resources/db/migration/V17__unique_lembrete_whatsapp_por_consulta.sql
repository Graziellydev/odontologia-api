DO $$
BEGIN
    IF EXISTS (
        SELECT consulta_id
        FROM notificacoes_whatsapp
        WHERE tipo_mensagem = 'LEMBRETE_24H' AND consulta_id IS NOT NULL
        GROUP BY consulta_id
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate LEMBRETE_24H records exist; review data before applying V17';
    END IF;
END $$;

CREATE UNIQUE INDEX uk_notificacoes_lembrete_consulta
    ON notificacoes_whatsapp (consulta_id)
    WHERE tipo_mensagem = 'LEMBRETE_24H' AND consulta_id IS NOT NULL;
