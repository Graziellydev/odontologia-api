package br.com.unicatolica.odontologia_api.solicitacoes.controller;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Map;

@RestControllerAdvice(assignableTypes = SolicitacaoPacienteController.class)
public class SolicitacaoConflitoHandler {
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> conflito(OptimisticLockingFailureException exception) {
        return ResponseEntity.status(409).body(Map.of(
                "message", "Solicitacao ou demanda alterada por outro usuario. Atualize os dados."));
    }
}
