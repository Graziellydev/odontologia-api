package br.com.unicatolica.odontologia_api.prontuarios.dto;

import java.time.OffsetDateTime;

public record SalvarProntuarioResponse(Long id, Long consultaId, String assinaturaDigital,
                                       OffsetDateTime dataRegistro, boolean atendimentoConcluido) { }
