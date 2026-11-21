package br.com.unicatolica.odontologia_api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetDeliveryService implements PasswordResetCodeSender {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetDeliveryService.class);
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final Environment environment;
    private final String mailHost;
    private final String mailFrom;
    private final boolean logCodeInDevelopment;

    public PasswordResetDeliveryService(
            ObjectProvider<JavaMailSender> mailSenders,
            Environment environment,
            @Value("${spring.mail.host:}") String mailHost,
            @Value("${spring.mail.from:}") String mailFrom,
            @Value("${app.auth.recovery.log-code:false}") boolean logCodeInDevelopment) {
        this.mailSenders = mailSenders;
        this.environment = environment;
        this.mailHost = mailHost;
        this.mailFrom = mailFrom;
        this.logCodeInDevelopment = logCodeInDevelopment;
    }

    @Override
    public void enviar(Long usuarioId, String email, String nome, String codigo) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (!mailHost.isBlank() && sender != null) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                if (!mailFrom.isBlank()) message.setFrom(mailFrom);
                message.setTo(email);
                message.setSubject("Recuperação de acesso — OdontoUni");
                message.setText("Olá, " + nome + ". Seu código para redefinir a senha é " + codigo
                        + ". Ele expira em 15 minutos. Se você não solicitou esta recuperação, ignore esta mensagem.");
                sender.send(message);
                return;
            } catch (MailException erro) {
                log.error("password_reset_delivery_failed userId={} reason={}", usuarioId,
                        erro.getClass().getSimpleName());
            }
        }

        if (logCodeInDevelopment && environment.acceptsProfiles(Profiles.of("local", "dev", "test"))) {
            log.info("password_reset_code_generated userId={} code={}", usuarioId, codigo);
        } else {
            log.warn("password_reset_delivery_unavailable userId={}", usuarioId);
        }
    }
}
