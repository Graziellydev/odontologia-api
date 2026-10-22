package br.com.unicatolica.odontologia_api.dto;

import java.time.OffsetDateTime;

public record CandidatoFilaDTO(Long filaEsperaId, Long pacienteId, String pacienteNome,
        String urgencia, OffsetDateTime entradaFila, String dente, String classe) {}
