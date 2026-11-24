package br.com.unicatolica.odontologia_api.notificacoes.webhook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record ReceberWebhookDTO(@NotBlank @Size(max = 40) String telefone,
                                @NotBlank @Size(max = 1000) String mensagem) { }
