-- MÓDULO DE COMUNICAÇÃO E NOTIFICAÇÕES
-- Tabela de Templates de Mensagem: matrizes de texto das mensagens automáticas (editáveis pela secretaria).
CREATE TABLE templates_mensagem (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo    VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(200) NOT NULL,
    conteudo  TEXT NOT NULL,          -- placeholders: {{nome_paciente}}, {{data}}, {{turno}} ...
    ativo     BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela de Notificações de WhatsApp: fila de envio e acompanhamento (Pendente, Enviado, Respondido).
CREATE TABLE notificacoes_whatsapp (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id       BIGINT NOT NULL REFERENCES pacientes(id),
    consulta_id       BIGINT REFERENCES consultas(id),
    tipo_mensagem     VARCHAR(50) NOT NULL REFERENCES templates_mensagem(codigo),
    conteudo_mensagem TEXT NOT NULL,                       -- texto final (pode ter sido editado)
    enviado_por_id    BIGINT REFERENCES usuarios(id),      -- NULL = envio automático
    status_envio      VARCHAR(25) NOT NULL DEFAULT 'PENDENTE'
        CHECK (status_envio IN ('PENDENTE','ENVIADO','CONFIRMADO_RESPOSTA','ERRO')),
    resposta_recebida VARCHAR(10) CHECK (resposta_recebida IN ('1','2','3','4','5')),  -- 1/2 confirmações; 1-5 nota
    erro_detalhe      TEXT,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    enviado_em        TIMESTAMPTZ,
    respondido_em     TIMESTAMPTZ
);
CREATE INDEX idx_wpp_paciente  ON notificacoes_whatsapp (paciente_id, criado_em DESC);
CREATE INDEX idx_wpp_consulta  ON notificacoes_whatsapp (consulta_id);
CREATE INDEX idx_wpp_pendentes ON notificacoes_whatsapp (status_envio) WHERE status_envio = 'PENDENTE';

-- Tabela de Avaliações de Satisfação: nota de 1 a 5 (e comentário opcional) respondida na própria conversa.
CREATE TABLE avaliacoes_satisfacao (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consulta_id   BIGINT NOT NULL UNIQUE REFERENCES consultas(id),
    gerada_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    nota          INT CHECK (nota BETWEEN 1 AND 5),
    comentario    TEXT,
    data_resposta TIMESTAMPTZ,
    CONSTRAINT ck_resposta CHECK ((nota IS NULL) = (data_resposta IS NULL))
);
