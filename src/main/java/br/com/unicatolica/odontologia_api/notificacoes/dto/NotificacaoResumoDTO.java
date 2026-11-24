package br.com.unicatolica.odontologia_api.notificacoes.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record NotificacaoResumoDTO(
        Long id, Long pacienteId, Long consultaId, String paciente, String telefonePaciente,
        String tipo, String conteudo, String status, OffsetDateTime criadaEm,
        String linkWhatsapp, String respostaRecebida, OffsetDateTime respondidaEm,
        LocalDate dataConsulta, String turno, String cadeira,
        String procedimento, String dente, String classe) {
}
