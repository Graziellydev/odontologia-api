CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Função utilitária (Trigger) para atualizar automaticamente a coluna "atualizado_em"
-- sempre que um registro for modificado (UPDATE) nas tabelas.
CREATE OR REPLACE FUNCTION fn_set_atualizado_em() RETURNS trigger AS $$
BEGIN
    NEW.atualizado_em = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- MÓDULO DE ACESSO E CADASTRO BASE
-- Tabela de Usuários:Serve para armazenar os usuários que acessam o sistema: Alunos, Coordenadores 
-- e Secretaria.
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

-- Tabela de Pacientes: É o cadastro central do sistema. Armazena desde a identificação básica, 
-- dados de contato para o WhatsApp, informações socioeconômicas (para relatórios 
-- da LGPD/MEC), até o controle de faltas e situação (Ativo/Desligado).
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
    rg                         VARCHAR(30),
    orgao_expedidor            VARCHAR(20),
    sexo                       CHAR(1) CHECK (sexo IN ('M', 'F')),
    raca_cor                   VARCHAR(20) CHECK (raca_cor IN ('BRANCA','PRETA','AMARELA','PARDA','INDIGENA','NAO_DECLARADO')),
    local_moradia              VARCHAR(15) CHECK (local_moradia IN ('RURAL', 'URBANA')),
    tipo_escola                VARCHAR(20) CHECK (tipo_escola IN ('PUBLICA', 'PRIVADA', 'NUNCA_ESTUDOU')),
    escolaridade_paciente      VARCHAR(40),
    escolaridade_mae           VARCHAR(40),
    escolaridade_pai           VARCHAR(40),
    renda_familiar             VARCHAR(30),
    criado_em                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Logs de Auditoria: Serve para segurança e conformidade com a LGPD. Registra quem fez o quê, 
-- quando e em qual registro (ex: quem acessou um prontuário ou alterou um cadastro).
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


-- MÓDULO ACADÊMICO E DE METAS
-- Tabela de Procedimentos Clínicos: Catálogo fixo de todos os tratamentos que a clínica oferece 
-- (ex: Restauração, Endodontia, Extração).
CREATE TABLE procedimentos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE,
    especialidade VARCHAR(60)  NOT NULL,
    exige_dente   BOOLEAN NOT NULL DEFAULT TRUE,
    exige_classe  BOOLEAN NOT NULL DEFAULT FALSE,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela de Disciplinas: Representa as matérias/clínicas do curso de Odontologia 
-- (ex: Odonto 1 - 6º período, Odonto 3 - 8º período).
CREATE TABLE disciplinas (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL UNIQUE,
    periodo_curso INT NOT NULL CHECK (periodo_curso BETWEEN 1 AND 12),
    ativo         BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela Disciplina x Procedimentos (Metas Acadêmicas): Serve para ditar as regras: Quais procedimentos a clínica X pode fazer?
-- E define a meta do aluno (quantos procedimentos ele tem que fazer para passar).
CREATE TABLE disciplina_procedimentos (
    id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    disciplina_id      BIGINT NOT NULL REFERENCES disciplinas(id),
    procedimento_id    BIGINT NOT NULL REFERENCES procedimentos(id),
    restricao_dente    VARCHAR(10) NOT NULL DEFAULT 'QUALQUER',
    classes_permitidas VARCHAR(3)[],
    quantidade_exigida INT NOT NULL DEFAULT 0,
    ativo              BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela de Turmas: Instância de uma disciplina em um semestre letivo específico (ex: Turma A de 2026.2).
CREATE TABLE turmas (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    disciplina_id   BIGINT NOT NULL REFERENCES disciplinas(id),
    semestre_letivo VARCHAR(6) NOT NULL,
    codigo_turma    VARCHAR(50) NOT NULL,
    professor_nome  VARCHAR(150),
    ativa           BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela de Matrículas: Vincula quais alunos (usuários) estão cursando quais turmas no semestre.
CREATE TABLE matriculas_turma (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    turma_id  BIGINT NOT NULL REFERENCES turmas(id),
    aluno_id  BIGINT NOT NULL REFERENCES usuarios(id),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);


-- MÓDULO DE TRIAGEM E DEMANDA (FILA)
-- Tabela de Triagens: Registra o acolhimento presencial inicial do paciente. Define o quão 
-- urgente é o caso (PNE, Alta, Média, Baixa) após a avaliação de um aluno/professor.
CREATE TABLE triagens (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id               BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_responsavel_id      BIGINT REFERENCES usuarios(id),
    professor_supervisor_nome VARCHAR(150),
    secretaria_lancamento_id  BIGINT REFERENCES usuarios(id),
    nivel_urgencia            VARCHAR(10) NOT NULL,
    necessidades_clinicas     TEXT NOT NULL,
    data_triagem              TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Itens da Triagem: Detalha a triagem quebrando em problemas específicos. (Ex: O paciente precisa 
-- de uma Restauração no dente 36 e uma Extração no dente 18).
CREATE TABLE triagem_itens (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    triagem_id      BIGINT NOT NULL REFERENCES triagens(id),
    procedimento_id BIGINT NOT NULL REFERENCES procedimentos(id),
    dente           VARCHAR(2),
    classe          VARCHAR(3),
    observacao      TEXT
);

-- Tabela de Tratamentos: Serve para acompanhar o andamento de um problema clínico do paciente desde o dia 
-- que foi identificado até o dia que foi finalizado (Concluído) por um aluno.
CREATE TABLE tratamentos (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id     BIGINT NOT NULL REFERENCES pacientes(id),
    triagem_item_id BIGINT REFERENCES triagem_itens(id),
    procedimento_id BIGINT NOT NULL REFERENCES procedimentos(id),
    dente           VARCHAR(2),
    classe          VARCHAR(3),
    status          VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
    data_inicio     TIMESTAMPTZ,
    data_conclusao  TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Fila de Espera: A fila oficial da clínica. Junta as demandas vindas da triagem ou pacientes 
-- retornando/remarcados, ordenando por prioridade e tempo para a recepção agendar.
CREATE TABLE fila_espera (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id              BIGINT NOT NULL REFERENCES pacientes(id),
    triagem_item_id          BIGINT REFERENCES triagem_itens(id),
    tratamento_id            BIGINT REFERENCES tratamentos(id),
    origem                   VARCHAR(20) NOT NULL DEFAULT 'TRIAGEM',
    procedimento_id          BIGINT NOT NULL REFERENCES procedimentos(id),
    dente                    VARCHAR(2),
    classe                   VARCHAR(3),
    nivel_urgencia           VARCHAR(10) NOT NULL,
    prioridade_reagendamento BOOLEAN NOT NULL DEFAULT FALSE,
    status_fila              VARCHAR(15) NOT NULL DEFAULT 'AGUARDANDO',
    observacao               TEXT,
    data_entrada_fila        TIMESTAMPTZ NOT NULL DEFAULT now(),
    data_saida_fila          TIMESTAMPTZ,
    versao                   BIGINT NOT NULL DEFAULT 0
);

-- MÓDULO DE AGENDA E CONSULTAS
-- Tabela de Cadeiras Físicas: O cadastro das cadeiras/equipamentos existentes no prédio da clínica.
CREATE TABLE cadeiras (
    id     BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    numero VARCHAR(10) NOT NULL UNIQUE,
    ativa  BOOLEAN NOT NULL DEFAULT TRUE
);

-- Tabela de Vagas de Agenda: Representa os slots de tempo disponíveis (ex: Cadeira 01, Dia 20, Turno MANHÃ).
-- É usada para evitar duplo agendamento no mesmo local/hora.
CREATE TABLE vagas (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cadeira_id       BIGINT NOT NULL REFERENCES cadeiras(id),
    data             DATE NOT NULL,
    turno            VARCHAR(10) NOT NULL CHECK (turno IN ('MANHA','TARDE')),
    bloqueada        BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_bloqueio  VARCHAR(200)
);

-- Tabela de Consultas: O coração do agendamento. Conecta o Paciente, o Aluno, a Turma e a Vaga.
-- Controla status, presenças/faltas e avisa ao paciente via WhatsApp.
CREATE TABLE consultas (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id           BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id              BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id              BIGINT NOT NULL REFERENCES turmas(id),
    tratamento_id         BIGINT REFERENCES tratamentos(id),
    vaga_id               BIGINT NOT NULL REFERENCES vagas(id),
    fila_espera_id        BIGINT REFERENCES fila_espera(id),
    consulta_origem_id    BIGINT REFERENCES consultas(id),
    criado_por_id         BIGINT REFERENCES usuarios(id),
    status_consulta       VARCHAR(25) NOT NULL DEFAULT 'AGENDADO',
    falta_justificada     BOOLEAN,
    justificativa_falta   TEXT,
    retorno_sugerido      BOOLEAN NOT NULL DEFAULT FALSE,
    justificativa_retorno TEXT,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                BIGINT NOT NULL DEFAULT 0
);

-- Tabela de Solicitações de Pacientes (pelos Alunos): Serve para quando o aluno precisa de um paciente com um perfil muito específico
-- (ex: dente 36 com cárie profunda) para bater a meta e pede à secretaria.
CREATE TABLE solicitacoes_pacientes (
    id                        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aluno_id                  BIGINT NOT NULL REFERENCES usuarios(id),
    turma_id                  BIGINT NOT NULL REFERENCES turmas(id),
    procedimento_id           BIGINT NOT NULL REFERENCES procedimentos(id),
    dente                     VARCHAR(2) NOT NULL,
    classe                    VARCHAR(3),
    data_desejada             DATE,
    status_solicitacao        VARCHAR(25) NOT NULL DEFAULT 'PENDENTE_APROVACAO',
    fila_espera_id            BIGINT REFERENCES fila_espera(id),
    paciente_alocado_id       BIGINT REFERENCES pacientes(id),
    secretaria_analisador_id  BIGINT REFERENCES usuarios(id),
    data_analise              TIMESTAMPTZ,
    observacao_secretaria     TEXT,
    data_solicitacao          TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    versao                    BIGINT NOT NULL DEFAULT 0
);


-- MÓDULO CLÍNICO E PRONTUÁRIOS (Físico + Digital) Tabela de Prontuários (Evolução Clínica Digital): Imutável. Registra o que foi feito na consulta atual (textualmente). Após 
-- salvo com assinatura digital pelo aluno, não pode ser editado nem apagado.
CREATE TABLE prontuarios (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id         BIGINT NOT NULL REFERENCES pacientes(id),
    aluno_id            BIGINT NOT NULL REFERENCES usuarios(id),
    consulta_id         BIGINT NOT NULL UNIQUE REFERENCES consultas(id),
    turma_id            BIGINT NOT NULL REFERENCES turmas(id),
    tratamento_id       BIGINT REFERENCES tratamentos(id),
    procedimento_id     BIGINT NOT NULL REFERENCES procedimentos(id),
    dente               VARCHAR(2),
    classe              VARCHAR(3),
    evolucao_clinica    TEXT NOT NULL,
    assinatura_digital  TEXT NOT NULL,
    data_registro       TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Anamnese do Paciente (Prontuário Físico Digitalizado): fArmazena o questionário de saúde completo da ficha física (alergias, 
-- hipertensão, cirurgias) e os dados de sinais vitais.
CREATE TABLE paciente_anamnese (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    queixa_principal        TEXT,
    historia_queixa         TEXT,
    respostas_questionario  JSONB NOT NULL DEFAULT '{}'::jsonb,
    pressao_arterial        VARCHAR(20),
    pulso_bpm               INT,
    temperatura_c           NUMERIC(4,1),
    frequencia_respiratoria INT,
    medo_dentista_escala    INT,
    frequencia_escovacao    INT,
    usa_fio_dental          BOOLEAN DEFAULT FALSE,
    usa_enxaguante          BOOLEAN DEFAULT FALSE,
    limpa_lingua            BOOLEAN DEFAULT FALSE,
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Exames Clínicos Extra/Intraorais e PSR: Armazena a digitalização dos exames de assimetrias faciais, problemas na 
-- ATM (maxilar), descrição detalhada de lesões (feridas) e as notas do PSR (gengiva).
CREATE TABLE paciente_exames_clinicos (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    consulta_id             BIGINT REFERENCES consultas(id),
    tem_edema               BOOLEAN DEFAULT FALSE,
    tem_dor_muscular        BOOLEAN DEFAULT FALSE,
    tem_tumefacao           BOOLEAN DEFAULT FALSE,
    linfonodos_palpaveis    BOOLEAN DEFAULT FALSE,
    tem_assimetria          BOOLEAN DEFAULT FALSE,
    atm_estalido            BOOLEAN DEFAULT FALSE,
    atm_dor_palpacao        BOOLEAN DEFAULT FALSE,
    atm_desvio_abertura     BOOLEAN DEFAULT FALSE,
    descricao_lesao_forma   VARCHAR(100),
    lesao_implantacao       VARCHAR(100),
    lesao_coloracao         VARCHAR(50),
    lesao_superficie        VARCHAR(100),
    lesao_consistencia      VARCHAR(100),
    lesao_tamanho           VARCHAR(50),
    lesao_localizacao       VARCHAR(150),
    hipotese_diagnostica    TEXT,
    psr_sextante_1          VARCHAR(2),
    psr_sextante_2          VARCHAR(2),
    psr_sextante_3          VARCHAR(2),
    psr_sextante_4          VARCHAR(2),
    psr_sextante_5          VARCHAR(2),
    psr_sextante_6          VARCHAR(2),
    registrado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Tabela de Odontograma: A representação digital dos dentes do paciente (mapeamento dente por dente e por face).
-- Grava o estado atual de cada dente (ex: Saudável, Cariado, Restaurado, Ausente).
CREATE TABLE paciente_odontograma (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paciente_id             BIGINT NOT NULL REFERENCES pacientes(id) ON DELETE CASCADE,
    dente                   VARCHAR(2) NOT NULL,
    face_dente              VARCHAR(10),
    condicao_dente          VARCHAR(50) NOT NULL,
    observacao              TEXT,
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT now()
);