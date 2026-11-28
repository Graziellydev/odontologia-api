package br.com.unicatolica.odontologia_api.prontuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ObservacaoOdontogramaRequest(
        @NotBlank @Pattern(regexp = "([1-4][1-8]|[5-8][1-5])") String dente,
        @Size(max = 10) String faceDente,
        @Size(max = 50) String condicaoDente,
        @NotBlank String observacao) {
}
