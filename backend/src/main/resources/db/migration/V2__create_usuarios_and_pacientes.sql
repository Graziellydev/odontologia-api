-- MÓDULO DE ACESSO E CADASTRO BASE
-- Tabela de Usuários:Serve para armazenar os usuários que acessam o sistema: Alunos, Coordenadores e Secretaria.
CREATE TABLE usuarios (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome                  VARCHAR(150) NOT NULL,
    email                 VARCHAR(150) NOT NULL UNIQUE,
    senha_hash            VARCHAR(255) NOT NULL,
    perfil                VARCHAR(30)  NOT NULL CHECK (perfil IN ('ALUNO','COORDENADOR','SECRETARIA')),
    matricula_ou_registro VARCHAR(50)  UNIQUE,
    semestre_atual        INT CHECK (semestre_atual BETWEEN 1 AND 12),
    ativo                 BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- MÓDULO DE ACESSO E CADASTRO BASE
-- Tabela de Pacientes:Serve para armazenar as informações demográficas, de contato e socioeconômicas dos pacientes da clínica.
CREATE TABLE pacientes (
    id                         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero_prontuario          VARCHAR(30) UNIQUE,
    nome_completo              VARCHAR(150) NOT NULL,
    cpf                        VARCHAR(11)  NOT NULL UNIQUE CHECK (cpf ~ '^[0-9]{11}$'),
    data_nascimento            DATE         NOT NULL,
    telefone                   VARCHAR(20)  NOT NULL,
    telefone_alternativo       VARCHAR(20),
    email                      VARCHAR(150),
    cep                        VARCHAR(9),
    logradouro                 VARCHAR(150),
    numero_endereco            VARCHAR(20),
    complemento                VARCHAR(100),
    bairro                     VARCHAR(100) NOT NULL,
    cidade                     VARCHAR(100) NOT NULL,
    uf                         CHAR(2),
    codigo_acesso              VARCHAR(20) UNIQUE,
    mora_proximo               BOOLEAN NOT NULL DEFAULT FALSE,
    aceita_chamada_ultima_hora BOOLEAN NOT NULL DEFAULT FALSE,
    disponibilidade_horario    VARCHAR(100),
    observacao                 TEXT,
    id_externo                 VARCHAR(50),
    situacao                   VARCHAR(15) NOT NULL DEFAULT 'ATIVO' CHECK (situacao IN ('ATIVO','DESLIGADO','EXCLUIDO')),
    termo_compromisso_assinado BOOLEAN NOT NULL DEFAULT FALSE,
    data_termo_compromisso     TIMESTAMPTZ,
    faltas_justificadas        INT NOT NULL DEFAULT 0,
    faltas_nao_justificadas    INT NOT NULL DEFAULT 0,
    motivo_desligamento        TEXT,
    data_desligamento          TIMESTAMPTZ,
    desligado_por_id           BIGINT REFERENCES usuarios(id),
    criado_em                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- MÓDULO DE SEGURANÇA E AUDITORIA
-- Tabela de Logs de Auditoria:Serve para registrar ações sensíveis realizadas no sistema, garantindo rastreabilidade de quem fez o que e quando.
CREATE TABLE logs_auditoria (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id  BIGINT REFERENCES usuarios(id),
    acao        VARCHAR(100) NOT NULL,
    entidade    VARCHAR(60),
    entidade_id BIGINT,
    detalhes    TEXT,
    ip_origem   VARCHAR(45),
    criado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);