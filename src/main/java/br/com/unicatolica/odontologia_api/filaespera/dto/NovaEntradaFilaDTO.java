package br.com.unicatolica.odontologia_api.filaespera.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record NovaEntradaFilaDTO(

                @NotNull @Positive Long pacienteId,

                @Positive Long triagemItemId,

                @NotNull @Positive Long procedimentoId,

                @Pattern(regexp = "^([1-4][1-8]|[5-8][1-5])$") String dente,

                @Pattern(regexp = "^(I|II|III|IV|V|VI)$") String classe,

                @NotBlank @Pattern(regexp = "(?i)^(ALTA|MEDIA|MÉDIA|BAIXA|PNE)$") String nivelUrgencia,

                Boolean prioridadeReagendamento,

                @Size(max = 5000) String observacao

) {
}