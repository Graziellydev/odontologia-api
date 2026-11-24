package br.com.unicatolica.odontologia_api.notificacoes.webhook;

import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/webhooks/whatsapp")
@RequiredArgsConstructor
public class WebhookWhatsappController {
    private final WebhookWhatsappService service;
    private final Environment environment;

    @PostMapping
    public ResponseEntity<RespostaWebhookDTO> receber(
            @RequestHeader(value="X-Webhook-Token", required=false) String token,
            @Valid @RequestBody ReceberWebhookDTO request) {
        validarToken(token);
        return ResponseEntity.ok(service.processar(request.telefone(),request.mensagem()));
    }

    private void validarToken(String recebido) {
        String configurado=environment.getProperty("app.whatsapp.webhook-token","");
        if(configurado==null || configurado.isBlank()) return;
        if(recebido==null || !MessageDigest.isEqual(
                configurado.getBytes(StandardCharsets.UTF_8),recebido.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook nao autorizado.");
    }
}
