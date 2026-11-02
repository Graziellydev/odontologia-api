package br.com.unicatolica.odontologia_api.consultas.dto;

import java.time.LocalDate;

public record AusenciaResponseDTO(
        Long consultaId,
        LocalDate data,
        String ausencia,
        String vinculoPaciente,
        String vinculoAluno,
        String situacao,
        String proximoPasso) {
}
