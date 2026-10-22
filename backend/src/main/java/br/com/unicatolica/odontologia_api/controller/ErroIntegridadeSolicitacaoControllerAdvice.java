package br.com.unicatolica.odontologia_api.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = SolicitarPacienteController.class)
public class ErroIntegridadeSolicitacaoControllerAdvice {
    // Evita expor detalhes internos do PostgreSQL e informa conflito ao frontend.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> tratarIntegridade(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Operação não permitida pelas regras do banco de dados. "
                        + "Confira os dados ou escolha outra vaga."));
    }
}
