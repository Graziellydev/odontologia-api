package br.com.unicatolica.odontologia_api.prontuarios.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = ProntuarioController.class)
public class ProntuarioExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException exception) {
        String detail = exception.getReason() == null || exception.getReason().isBlank()
                ? exception.getStatusCode().toString()
                : exception.getReason();
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("detail", detail));
    }
}
