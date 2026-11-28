package br.com.unicatolica.odontologia_api.consultas.dto;

import java.time.LocalDate;

public record VagaResponseDTO(
        Long id,
        Long cadeiraId,
        String cadeiraNumero,
        LocalDate data,
        String turno,
        boolean bloqueada,
        String motivoBloqueio) { }
