package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.NotificacaoWhatsapp;
import br.com.unicatolica.odontologia_api.consultas.repository.AgendaConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.NotificacaoWhatsappRepository;
import br.com.unicatolica.odontologia_api.model.Paciente;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificacaoAgendamentoService {

    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final AgendaConsultaRepository agenda;
    private final NotificacaoWhatsappRepository notificacoes;

    @Transactional
    public NotificacaoWhatsapp registrar(
            Consulta consulta, Paciente paciente, LocalDate data, String turno,
            String codigoTemplate, Long secretariaId) {
        String template = agenda.buscarTemplate(codigoTemplate);
        if (template == null) {
            template = templatePadrao(codigoTemplate);
        }
        // O template legado de cancelamento não incluía o turno. Preserve a edição
        // existente e complete a mensagem gerada para cumprir o conteúdo do RF007.
        if ("AVISO_CANCELAMENTO".equals(codigoTemplate) && !template.contains("{{turno}}")) {
            template = template + " Turno: {{turno}}.";
        }
        String mensagem = template
                .replace("{{nome_paciente}}", paciente.getNomeCompleto())
                .replace("{{data}}", DATA_BR.format(data))
                .replace("{{turno}}", turno.equals("MANHA") ? "manhã" : "tarde");

        NotificacaoWhatsapp notificacao = new NotificacaoWhatsapp();
        notificacao.setPacienteId(paciente.getId());
        notificacao.setConsultaId(consulta.getId());
        notificacao.setTipoMensagem(codigoTemplate);
        notificacao.setConteudoMensagem(mensagem);
        notificacao.setEnviadoPorId(secretariaId);
        // Sem credenciais/provedor configurados, registra a mensagem na fila de simulação.
        notificacao.setStatusEnvio("PENDENTE");
        return notificacoes.saveAndFlush(notificacao);
    }

    private String templatePadrao(String codigo) {
        return switch (codigo) {
            case "COMPROVANTE_AGENDAMENTO" -> "Olá, {{nome_paciente}}! Sua consulta foi agendada para {{data}}, turno {{turno}}.";
            case "AVISO_REMARCACAO" -> "Olá, {{nome_paciente}}! Sua consulta foi remarcada para {{data}}, turno {{turno}}.";
            case "AVISO_CANCELAMENTO" -> "Olá, {{nome_paciente}}! Sua consulta de {{data}} foi cancelada. Você retornará à fila de espera.";
            default -> throw new IllegalArgumentException("Tipo de notificação de agendamento desconhecido.");
        };
    }
}
