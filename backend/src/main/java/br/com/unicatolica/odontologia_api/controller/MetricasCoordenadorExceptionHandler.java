package br.com.unicatolica.odontologia_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.unicatolica.odontologia_api.dto.MetricasErroResponse;
import br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException;
import br.com.unicatolica.odontologia_api.exception.SemestreNaoEncontradoException;

@RestControllerAdvice(assignableTypes = MetricasCoordenadorController.class)
public class MetricasCoordenadorExceptionHandler {

    @ExceptionHandler(SemestreInvalidoException.class)
    public ResponseEntity<MetricasErroResponse> semestreInvalido(SemestreInvalidoException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new MetricasErroResponse("SEMESTRE_INVALIDO", exception.getMessage()));
    }

    @ExceptionHandler(SemestreNaoEncontradoException.class)
    public ResponseEntity<MetricasErroResponse> semestreNaoEncontrado(SemestreNaoEncontradoException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new MetricasErroResponse(
                        "SEMESTRE_NAO_ENCONTRADO",
                        "O semestre letivo informado não está cadastrado."));
    }
}