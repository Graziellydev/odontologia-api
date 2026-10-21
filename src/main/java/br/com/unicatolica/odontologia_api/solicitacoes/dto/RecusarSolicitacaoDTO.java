package br.com.unicatolica.odontologia_api.solicitacoes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecusarSolicitacaoDTO(

        @NotBlank @Size(max = 5000) String observacaoSecretaria

) {
}