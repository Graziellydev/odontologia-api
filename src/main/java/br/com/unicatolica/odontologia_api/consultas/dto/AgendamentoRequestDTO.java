package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record AgendamentoRequestDTO(
        @NotNull @Positive Long filaEsperaId,
        @NotNull @Positive Long alunoId,
        @NotNull @Positive Long turmaId,
        @NotNull LocalDate data,
        @NotNull Turno turno) {

    public enum Turno { MANHA, TARDE }
}
