package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotNull;

public record FaltaRequestDTO(
        @NotNull QuemFaltou quemFaltou,
        @NotNull Boolean justificada,
        String justificativa) {

    public enum QuemFaltou {
        PACIENTE,
        ALUNO
    }
}
