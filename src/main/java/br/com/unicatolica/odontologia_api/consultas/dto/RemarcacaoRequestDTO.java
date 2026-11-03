package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RemarcacaoRequestDTO(
        @NotNull LocalDate data,
        @NotNull AgendamentoRequestDTO.Turno turno) {
}
