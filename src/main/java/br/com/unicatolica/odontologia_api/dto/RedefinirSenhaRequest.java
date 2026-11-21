package br.com.unicatolica.odontologia_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RedefinirSenhaRequest(
        @NotBlank @Size(max = 150) String identificador,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$") String codigo,
        @NotBlank @Size(min = 12, max = 72) String novaSenha) {
}
