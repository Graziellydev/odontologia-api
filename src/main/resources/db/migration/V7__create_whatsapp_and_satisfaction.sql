CREATE TABLE templates_mensagem (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo    VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(200) NOT NULL,
    conteudo  TEXT NOT NULL,
    ativo     BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE notificacoes_whatsapp (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id       BIGINT NOT NULL REFERENCES pacientes(id),
    consulta_id       BIGINT REFERENCES consultas(id),
    tipo_mensagem     VARCHAR(50) NOT NULL REFERENCES templates_mensagem(codigo),
    conteudo_mensagem TEXT NOT NULL,
    enviado_por_id    BIGINT REFERENCES usuarios(id),
    status_envio      VARCHAR(25) NOT NULL DEFAULT 'PENDENTE',
    resposta_recebida VARCHAR(10),
    erro_detalhe      TEXT,
    criado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    enviado_em        TIMESTAMPTZ,
    respondido_em     TIMESTAMPTZ
);

CREATE TABLE avaliacoes_satisfacao (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consulta_id   BIGINT NOT NULL UNIQUE REFERENCES consultas(id),
    gerada_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    nota          INT CHECK (nota BETWEEN 1 AND 5),
    comentario    TEXT,
    data_resposta TIMESTAMPTZ
);