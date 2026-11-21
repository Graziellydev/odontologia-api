package br.com.unicatolica.odontologia_api.dto;

import lombok.Data;

/**
 * DTO (Data Transfer Object) de entrada.
 * Carrega estritamente o e-mail e a senha enviados no corpo do JSON da requisição de login.
 */
@Data
public class LoginDTO {
    private String identificador;
    private String email;
    private String senha;
}
