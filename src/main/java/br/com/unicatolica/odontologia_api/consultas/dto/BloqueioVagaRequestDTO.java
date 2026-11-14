package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloqueioVagaRequestDTO(
        @NotBlank @Size(max = 200) String motivo) { }
