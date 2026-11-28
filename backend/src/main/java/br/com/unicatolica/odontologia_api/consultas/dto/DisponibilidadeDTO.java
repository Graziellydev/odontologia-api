package br.com.unicatolica.odontologia_api.consultas.dto;

import java.time.LocalDate;

public record DisponibilidadeDTO(LocalDate data, String turno, int vagasDisponiveis) { }
