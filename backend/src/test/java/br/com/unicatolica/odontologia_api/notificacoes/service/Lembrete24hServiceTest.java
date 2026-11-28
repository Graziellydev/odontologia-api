package br.com.unicatolica.odontologia_api.notificacoes.service;

import br.com.unicatolica.odontologia_api.notificacoes.repository.CandidatoLembrete24h;
import br.com.unicatolica.odontologia_api.notificacoes.repository.Lembrete24hRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class Lembrete24hServiceTest {

    @Mock private Lembrete24hRepository repository;
    @InjectMocks private Lembrete24hService service;

    @Test
    void processaSomenteDataSeguinteEGravaMensagemPendenteUmaVez() {
        LocalDate hoje = LocalDate.of(2026, 11, 23);
        var candidato = new CandidatoLembrete24h(
                10L, 20L, "Ana", hoje.plusDays(1), "MANHA",
                "Olá, {{nome_paciente}}! {{data}} {{turno}}. Responda 1 para confirmar ou 2 para cancelar.");
        when(repository.listarCandidatos(hoje.plusDays(1))).thenReturn(List.of(candidato));
        when(repository.inserirSeAusente(eq(candidato), any())).thenReturn(1);

        assertEquals(1, service.processar(hoje));

        verify(repository).listarCandidatos(hoje.plusDays(1));
        verify(repository).inserirSeAusente(eq(candidato),
                eq("Olá, Ana! 24/11/2026 manhã. Responda 1 para confirmar ou 2 para cancelar."));
    }

    @Test
    void registraFalhaOperacionalSemExporDadosDaConsulta(CapturedOutput output) {
        LocalDate hoje = LocalDate.of(2026, 11, 23);
        String telefone = "11987654321";
        String mensagem = "conteudo confidencial de teste";
        var falhaEsperada = new DataAccessResourceFailureException(telefone + " " + mensagem);
        when(repository.listarCandidatos(hoje.plusDays(1))).thenThrow(falhaEsperada);

        DataAccessResourceFailureException falha = assertThrows(
                DataAccessResourceFailureException.class, () -> service.processar(hoje));

        String log = output.getAll();
        assertSame(falhaEsperada, falha);
        assertTrue(log.contains("whatsapp_reminder_processing_failed"));
        assertTrue(log.contains("DataAccessResourceFailureException"));
        assertFalse(log.contains(telefone));
        assertFalse(log.contains(mensagem));
    }
}
