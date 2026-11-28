package br.com.unicatolica.odontologia_api.notificacoes.webhook;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class WebhookWhatsappServiceTest {
    @Mock private JdbcTemplate jdbc;

    @Test
    void rejeitaTelefoneInvalidoSemConsultarDados() {
        WebhookWhatsappService service = new WebhookWhatsappService(jdbc);
        assertEquals("TELEFONE_INVALIDO", service.processar("12", "1").resultado());
        verifyNoInteractions(jdbc);
    }

    @Test
    void respostaNaoReconhecidaNaoConsultaOuAtualizaDados() {
        WebhookWhatsappService service = new WebhookWhatsappService(jdbc);
        assertEquals("RESPOSTA_NAO_RECONHECIDA",
                service.processar("(11) 99999-9999", "talvez").resultado());
        verifyNoInteractions(jdbc);
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void registraFalhaOperacionalSemExporTelefoneOuResposta(CapturedOutput output) {
        String telefone = "11987654321";
        String respostaValida = "1";
        String corpoConfidencial = "texto confidencial de teste";
        var falhaEsperada = new DataAccessResourceFailureException(telefone + " " + corpoConfidencial);
        doThrow(falhaEsperada).when(jdbc).query(anyString(), any(RowMapper.class), any(), any());

        WebhookWhatsappService service = new WebhookWhatsappService(jdbc);
        DataAccessResourceFailureException falha = assertThrows(
                DataAccessResourceFailureException.class, () -> service.processar(telefone, respostaValida));

        String log = output.getAll();
        assertSame(falhaEsperada, falha);
        assertTrue(log.contains("whatsapp_webhook_processing_failed"));
        assertTrue(log.contains("DataAccessResourceFailureException"));
        assertFalse(log.contains(telefone));
        assertFalse(log.contains(corpoConfidencial));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void multiplosAvisosPendentesParaMesmoNumeroFalhamFechados() {
        var primeiro = new NotificacaoPendenteWebhook(1L, 10L, 20L,
                "LEMBRETE_24H", "11999999999", "AGENDADO", 30L);
        var segundo = new NotificacaoPendenteWebhook(2L, 10L, 21L,
                "LEMBRETE_24H", "11999999999", "AGENDADO", 31L);
        doReturn(List.of(primeiro, segundo)).when(jdbc)
                .query(anyString(), any(RowMapper.class), any(), any());

        WebhookWhatsappService service = new WebhookWhatsappService(jdbc);
        assertEquals("AMBIGUA", service.processar("+55 11 99999-9999", "1").resultado());
        verify(jdbc, times(1)).query(anyString(), any(RowMapper.class), any(), any());
        verify(jdbc, never()).update(anyString(), any(Object[].class));
    }
}
