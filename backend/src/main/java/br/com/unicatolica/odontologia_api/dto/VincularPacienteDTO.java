package br.com.unicatolica.odontologia_api.dto;

import jakarta.validation.constraints.NotNull;

public record VincularPacienteDTO(
        @NotNull Long secretariaId,
        @NotNull Long filaEsperaId,
        @NotNull Long vagaId
) {}
