
package br.com.unicatolica.odontologia_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SolicitarPacienteDTO(

        @NotNull
        Long alunoId,

        @NotNull
        Long turmaId,

        @NotNull
        Long procedimentoId,

        @NotBlank
        @Pattern(regexp = "^([1-4][1-8]|[5-8][1-5])$")
        String dente,

        @Pattern(regexp = "^(I|II|III|IV|V|VI)$")
        String classe,

        LocalDate dataDesejada,

        @Size(max = 1000)
        String observacaoSecretaria

) {
}
