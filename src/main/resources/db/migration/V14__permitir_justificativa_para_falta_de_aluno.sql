ALTER TABLE consultas DROP CONSTRAINT ck_falta_so_paciente;

ALTER TABLE consultas
    ADD CONSTRAINT ck_falta_so_paciente CHECK (
        falta_justificada IS NULL
        OR status_consulta IN ('FALTOU_PACIENTE', 'FALTOU_ALUNO')
    );
