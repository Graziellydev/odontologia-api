package br.com.unicatolica.odontologia_api.solicitacoes.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AprovarSolicitacaoDTO(

        @NotNull @Positive Long filaEsperaId

) {
}