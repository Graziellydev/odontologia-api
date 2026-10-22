package br.com.unicatolica.odontologia_api.solicitacoes.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record NovaSolicitacaoDTO(

        @NotNull @Positive Long turmaId,

        @NotNull @Positive Long procedimentoId,

        @NotBlank @Pattern(regexp = "^([1-4][1-8]|[5-8][1-5])$") String dente,

        @Pattern(regexp = "^(I|II|III|IV|V|VI)$") String classe,

        @NotNull @FutureOrPresent LocalDate dataDesejada

) {
}