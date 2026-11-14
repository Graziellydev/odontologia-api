package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record RemarcacaoRequestDTO(
        @NotNull @Positive Long cadeiraId,
        @NotNull LocalDate data,
        @NotNull AgendamentoRequestDTO.Turno turno) {
}
