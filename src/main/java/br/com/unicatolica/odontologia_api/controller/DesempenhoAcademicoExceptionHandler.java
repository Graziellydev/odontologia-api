package br.com.unicatolica.odontologia_api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.unicatolica.odontologia_api.dto.DesempenhoErroResponse;
import br.com.unicatolica.odontologia_api.exception.DesempenhoAcademicoException;
import br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException;
import br.com.unicatolica.odontologia_api.exception.SemestreNaoEncontradoException;

@RestControllerAdvice(assignableTypes = DesempenhoAcademicoController.class)
public class DesempenhoAcademicoExceptionHandler {

    @ExceptionHandler(DesempenhoAcademicoException.class)
    public ResponseEntity<DesempenhoErroResponse> desempenhoInvalido(
            DesempenhoAcademicoException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new DesempenhoErroResponse(exception.getCodigo(), exception.getMessage()));
    }

    @ExceptionHandler(SemestreInvalidoException.class)
    public ResponseEntity<DesempenhoErroResponse> semestreInvalido() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new DesempenhoErroResponse(
                        "SEMESTRE_INVALIDO", "Informe um semestre no formato AAAA.1 ou AAAA.2."));
    }

    @ExceptionHandler(SemestreNaoEncontradoException.class)
    public ResponseEntity<DesempenhoErroResponse> semestreNaoEncontrado() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DesempenhoErroResponse(
                        "SEMESTRE_NAO_ENCONTRADO", "O semestre letivo informado não está cadastrado."));
    }
}
