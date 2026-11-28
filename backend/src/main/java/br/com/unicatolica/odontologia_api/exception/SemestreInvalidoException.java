package br.com.unicatolica.odontologia_api.exception;

public class SemestreInvalidoException extends RuntimeException {

    public SemestreInvalidoException() {
        super("Informe um semestre no formato AAAA.1 ou AAAA.2.");
    }
}