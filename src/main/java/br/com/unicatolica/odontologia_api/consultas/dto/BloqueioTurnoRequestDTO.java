package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record BloqueioTurnoRequestDTO(
        @NotNull @Positive Long cadeiraId,
        @NotNull LocalDate data,
        @NotNull VagaRequestDTO.Turno turno,
        @NotBlank @Size(max = 200) String motivo) { }
