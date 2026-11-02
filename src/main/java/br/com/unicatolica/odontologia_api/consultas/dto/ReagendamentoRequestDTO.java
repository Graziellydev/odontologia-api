package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ReagendamentoRequestDTO(
        @NotNull @Positive Long novaVagaId,
        @NotNull Boolean notificarWhatsapp) {
}
