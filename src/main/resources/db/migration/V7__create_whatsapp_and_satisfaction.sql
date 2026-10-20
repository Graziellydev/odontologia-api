-- MÓDULO DE COMUNICAÇÃO E NOTIFICAÇÕES
-- Tabela de Templates de Mensagem:Serve para padronizar as matrizes de texto que serão enviadas nas mensagens automáticas de sistema aos pacientes.
CREATE TABLE templates_mensagem (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    codigo    VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(200) NOT NULL,
    conteudo  TEXT NOT NULL,
    ativo     BOOLEAN NOT NULL DEFAULT TRUE
);

-- MÓDULO DE COMUNICAÇÃO E NOTIFICAÇÕES
-- Tabela de Notificações de WhatsApp:Serve para organizar a fila de envio e o acompanhamento do status (Pendente, Enviado, Respondido) do disparo pelo bot.
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

-- MÓDULO DE COMUNICAÇÃO E NOTIFICAÇÕES
-- Tabela de Avaliações de Satisfação:Serve para coletar métricas pós-atendimento, onde o paciente responde uma nota de 1 a 5 e um comentário avaliando a clínica.
CREATE TABLE avaliacoes_satisfacao (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    consulta_id   BIGINT NOT NULL UNIQUE REFERENCES consultas(id),
    gerada_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    nota          INT CHECK (nota BETWEEN 1 AND 5),
    comentario    TEXT,
    data_resposta TIMESTAMPTZ
);