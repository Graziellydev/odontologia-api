package br.com.unicatolica.odontologia_api.notificacoes.service;

import br.com.unicatolica.odontologia_api.notificacoes.repository.CandidatoLembrete24h;
import br.com.unicatolica.odontologia_api.notificacoes.repository.Lembrete24hRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class Lembrete24hService {

    private static final Logger log = LoggerFactory.getLogger(Lembrete24hService.class);

    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Fortaleza");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Lembrete24hRepository repository;

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Fortaleza")
    public void processarDiariamente() {
        processar(LocalDate.now(ZONA_CLINICA));
    }

    @Transactional
    public int processar(LocalDate dataReferencia) {
        try {
            LocalDate dataConsulta = dataReferencia.plusDays(1);
            return repository.listarCandidatos(dataConsulta).stream()
                    .mapToInt(candidato -> repository.inserirSeAusente(
                            candidato, gerarConteudo(candidato)))
                    .sum();
        } catch (RuntimeException falha) {
            NotificacaoErrorLogger.registrar(log, "whatsapp_reminder_processing_failed", falha);
            throw falha;
        }
    }

    String gerarConteudo(CandidatoLembrete24h candidato) {
        String template = candidato.template();
        if (template == null || template.isBlank()) {
            template = "Ola, {{nome_paciente}}! Sua consulta e amanha, {{data}}, turno {{turno}}. "
                    + "Responda 1 para confirmar ou 2 para cancelar.";
        }
        return template
                .replace("{{nome_paciente}}", candidato.nomePaciente())
                .replace("{{data}}", DATA_BR.format(candidato.dataConsulta()))
                .replace("{{turno}}", "MANHA".equals(candidato.turno()) ? "manhã" : "tarde");
    }
}
