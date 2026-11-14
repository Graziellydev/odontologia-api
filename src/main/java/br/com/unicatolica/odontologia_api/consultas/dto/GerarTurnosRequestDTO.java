package br.com.unicatolica.odontologia_api.consultas.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record GerarTurnosRequestDTO(
        @NotNull LocalDate inicio,
        @NotNull LocalDate fim) { }
