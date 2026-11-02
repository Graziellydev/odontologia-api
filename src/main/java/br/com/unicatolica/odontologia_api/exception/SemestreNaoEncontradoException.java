package br.com.unicatolica.odontologia_api.exception;

public class SemestreNaoEncontradoException extends RuntimeException {

    public SemestreNaoEncontradoException(String semestre) {
        super("O semestre letivo informado não está cadastrado: " + semestre);
    }
}