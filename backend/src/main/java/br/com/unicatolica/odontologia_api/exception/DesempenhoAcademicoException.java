package br.com.unicatolica.odontologia_api.exception;

import org.springframework.http.HttpStatus;

public class DesempenhoAcademicoException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public DesempenhoAcademicoException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }
}
