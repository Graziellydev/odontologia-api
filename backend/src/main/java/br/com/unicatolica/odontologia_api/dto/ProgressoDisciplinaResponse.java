package br.com.unicatolica.odontologia_api.dto;

public record ProgressoDisciplinaResponse(
        Long disciplinaId,
        String disciplina,
        Long totalExigido,
        Long totalConcluido,
        Long saldoPendente) {
}