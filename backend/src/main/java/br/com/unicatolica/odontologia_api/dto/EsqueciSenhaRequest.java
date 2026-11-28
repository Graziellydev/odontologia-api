package br.com.unicatolica.odontologia_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EsqueciSenhaRequest(
        @NotBlank @Size(max = 150) String identificador) {
}
