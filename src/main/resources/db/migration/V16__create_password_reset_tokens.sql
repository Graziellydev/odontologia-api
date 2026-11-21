CREATE TABLE password_reset_tokens (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES usuarios(id),
    codigo_hash         VARCHAR(255) NOT NULL,
    expira_em            TIMESTAMPTZ NOT NULL,
    usado                BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em            TIMESTAMPTZ NOT NULL DEFAULT now(),
    tentativas_falhas    INTEGER NOT NULL DEFAULT 0 CHECK (tentativas_falhas >= 0)
);

CREATE INDEX idx_password_reset_tokens_usuario_criado
    ON password_reset_tokens (usuario_id, criado_em DESC);

CREATE UNIQUE INDEX uk_password_reset_tokens_aberto
    ON password_reset_tokens (usuario_id)
    WHERE usado = FALSE;
