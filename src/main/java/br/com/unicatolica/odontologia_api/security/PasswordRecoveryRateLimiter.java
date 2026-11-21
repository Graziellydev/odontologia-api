package br.com.unicatolica.odontologia_api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PasswordRecoveryRateLimiter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long windowMillis;
    private final Clock clock = Clock.systemUTC();

    public PasswordRecoveryRateLimiter(
            @Value("${app.auth.recovery.rate-limit.max-requests:10}") int maxRequests,
            @Value("${app.auth.recovery.rate-limit.window-seconds:3600}") long windowSeconds) {
        this.maxRequests = Math.max(1, maxRequests);
        this.windowMillis = Math.max(1, windowSeconds) * 1000;
    }

    public boolean permitir(String origem, String identificador) {
        long agora = clock.millis();
        String origemKey = "ip:" + (origem == null || origem.isBlank() ? "unknown" : origem);
        String identificadorKey = "id:" + hash(identificador == null ? "" : identificador.trim().toLowerCase(Locale.ROOT));
        boolean permitido = consumir(origemKey, agora) & consumir(identificadorKey, agora);
        if (buckets.size() > 20_000) {
            buckets.entrySet().removeIf(entry -> agora - entry.getValue().inicio > windowMillis);
        }
        return permitido;
    }

    private boolean consumir(String chave, long agora) {
        Bucket bucket = buckets.computeIfAbsent(chave, ignored -> new Bucket(agora));
        synchronized (bucket) {
            if (agora - bucket.inicio >= windowMillis) {
                bucket.inicio = agora;
                bucket.usos = 0;
            }
            if (bucket.usos >= maxRequests) return false;
            bucket.usos++;
            return true;
        }
    }

    private static String hash(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponível", impossible);
        }
    }

    private static final class Bucket {
        private long inicio;
        private int usos;
        private Bucket(long inicio) { this.inicio = inicio; }
    }
}
