
-- A observação enviada pelo aluno permanece em observacao_secretaria.
-- Esta nova coluna guardará a resposta escrita pela secretaria.

ALTER TABLE solicitacoes_pacientes
ADD COLUMN IF NOT EXISTS resposta_secretaria TEXT;

-- Preserva observações de análises antigas.
UPDATE solicitacoes_pacientes
SET resposta_secretaria = observacao_secretaria
WHERE data_analise IS NOT NULL
  AND observacao_secretaria IS NOT NULL
  AND resposta_secretaria IS NULL;

-- A justificativa da recusa precisa vir da secretaria.
ALTER TABLE solicitacoes_pacientes
DROP CONSTRAINT IF EXISTS ck_solic_recusa;

ALTER TABLE solicitacoes_pacientes
ADD CONSTRAINT ck_solic_recusa CHECK (
    status_solicitacao <> 'RECUSADO'
    OR (
        resposta_secretaria IS NOT NULL
        AND btrim(resposta_secretaria) <> ''
    )
);
