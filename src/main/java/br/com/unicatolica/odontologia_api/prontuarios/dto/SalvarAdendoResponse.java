package br.com.unicatolica.odontologia_api.prontuarios.dto;

import java.time.OffsetDateTime;

public record SalvarAdendoResponse(Long id, Long prontuarioId, Long autorId, String texto,
                                   OffsetDateTime criadoEm) { }
