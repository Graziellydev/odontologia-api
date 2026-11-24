package br.com.unicatolica.odontologia_api.notificacoes.webhook;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WebhookWhatsappControllerTest {
    @Test
    void tokenConfiguradoEhObrigatorioEValidado() {
        WebhookWhatsappService service = mock(WebhookWhatsappService.class);
        MockEnvironment env = new MockEnvironment().withProperty("app.whatsapp.webhook-token", "secret-value");
        WebhookWhatsappController controller = new WebhookWhatsappController(service, env);
        var body = new ReceberWebhookDTO("11999999999", "1");

        ResponseStatusException semToken = assertThrows(ResponseStatusException.class,
                () -> controller.receber(null, body));
        assertEquals(HttpStatus.UNAUTHORIZED, semToken.getStatusCode());
        verifyNoInteractions(service);

        ResponseStatusException tokenInvalido = assertThrows(ResponseStatusException.class,
                () -> controller.receber("wrong-token", body));
        assertEquals(HttpStatus.UNAUTHORIZED, tokenInvalido.getStatusCode());
        verifyNoInteractions(service);

        when(service.processar("11999999999", "1")).thenReturn(new RespostaWebhookDTO("CONFIRMADA"));
        assertEquals("CONFIRMADA", controller.receber("secret-value", body).getBody().resultado());
    }
}

