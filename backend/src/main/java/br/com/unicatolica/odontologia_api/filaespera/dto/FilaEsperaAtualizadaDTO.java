package br.com.unicatolica.odontologia_api.filaespera.dto;

import java.time.OffsetDateTime;

public record FilaEsperaAtualizadaDTO(
        Long id,
        String statusFila,
        OffsetDateTime dataSaidaFila) {
}