package br.com.unicatolica.odontologia_api.consultas.entity;

/** Status aceitos pela coluna status_consulta da tabela consultas. */
public enum StatusConsulta {
    AGENDADO,
    CONFIRMADA_24H,
    CANCELADO_SECRETARIA,
    CANCELADO_PACIENTE,
    REALIZADO,
    FALTOU_PACIENTE,
    FALTOU_ALUNO
}
