package br.com.unicatolica.odontologia_api.relatorios.dto;

import java.time.LocalDate;

public record FiltrosRelatorioResponse(String semestre, LocalDate dataInicio, LocalDate dataFim,
        String especialidade, String regiao) {}
