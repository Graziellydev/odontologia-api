package br.com.unicatolica.odontologia_api.dto;

public record MetaProcedimentoResponse(
    String procedimento,
    String disciplina,
    Integer quantidadeExigida,
    Long quantidadeConcluida,
    Long saldoPendente) {
}