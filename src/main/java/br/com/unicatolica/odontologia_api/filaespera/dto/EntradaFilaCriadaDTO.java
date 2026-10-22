package br.com.unicatolica.odontologia_api.filaespera.dto;

public record EntradaFilaCriadaDTO(
        Long id,
        Long pacienteId,
        Long procedimentoId,
        String nivelUrgencia,
        String statusFila) {
}