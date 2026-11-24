package br.com.unicatolica.odontologia_api.consultas.dto;

import java.time.OffsetDateTime;

public record NotificacaoConsultaDTO(
        Long id, Long pacienteId, Long consultaId, String tipo, String conteudo,
        String status, OffsetDateTime criadaEm, boolean simulada,
        String telefonePaciente, String linkWhatsapp,
        String respostaRecebida, OffsetDateTime respondidaEm) { }
