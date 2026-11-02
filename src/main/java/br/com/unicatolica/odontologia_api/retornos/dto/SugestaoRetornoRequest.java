package br.com.unicatolica.odontologia_api.retornos.dto;

import jakarta.validation.constraints.NotNull;

public record SugestaoRetornoRequest(
        @NotNull Long consultaId,
        @NotNull Boolean necessitaRetorno,
        String justificativa) {
}