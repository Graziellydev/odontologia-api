package br.com.unicatolica.odontologia_api.security;

public interface PasswordResetCodeSender {
    void enviar(Long usuarioId, String email, String nome, String codigo);
}
