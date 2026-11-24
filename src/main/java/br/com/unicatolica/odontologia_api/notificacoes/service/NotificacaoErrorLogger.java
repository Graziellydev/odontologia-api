package br.com.unicatolica.odontologia_api.notificacoes.service;

import org.slf4j.Logger;

/** Logs notification failures with diagnostic stack frames, excluding exception messages and request data. */
public final class NotificacaoErrorLogger {

    private NotificacaoErrorLogger() {
    }

    public static void registrar(Logger logger, String evento, RuntimeException falha) {
        Throwable causa = falha;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }

        RuntimeException pilhaSegura = new RuntimeException("Sanitized notification failure stack");
        pilhaSegura.setStackTrace(falha.getStackTrace());
        logger.error("{} exceptionType={} causeType={}", evento,
                falha.getClass().getSimpleName(), causa.getClass().getSimpleName(), pilhaSegura);
    }
}
