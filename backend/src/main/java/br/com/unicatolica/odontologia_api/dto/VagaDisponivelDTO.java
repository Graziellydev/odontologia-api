package br.com.unicatolica.odontologia_api.dto;

import java.time.LocalDate;

public record VagaDisponivelDTO(Long vagaId, LocalDate data, String turno, String cadeira) {}
