package br.com.unicatolica.odontologia_api.notificacoes.webhook;
public record NotificacaoPendenteWebhook(Long notificacaoId, Long pacienteId, Long consultaId,
        String tipo, String telefone, String statusConsulta, Long filaEsperaId) { }
