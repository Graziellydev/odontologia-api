package br.com.unicatolica.odontologia_api.prontuarios.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RegistrarProntuarioRequest(
        @NotNull Long consultaId,
        @NotNull Long procedimentoId,
        @Pattern(regexp = "([1-4][1-8]|[5-8][1-5])") String dente,
        @Pattern(regexp = "I|II|III|IV|V|VI") String classe,
        @NotBlank @Size(max = 20000) String evolucaoClinica,
        @NotNull Boolean concluirAtendimento,
        @Valid List<ObservacaoOdontogramaRequest> observacoesOdontograma) {
}
