package br.com.unicatolica.odontologia_api.dto;

import br.com.unicatolica.odontologia_api.model.Perfil;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de resposta retornado ao cliente após a autenticação realizada com sucesso.
 * Omite dados sensíveis como a hash da senha.
 */
@Data
@AllArgsConstructor
public class LoginResponseDTO {
    private Long id;
    private String nome;
    private String email;
    private Perfil perfil;
    private String matriculaOuRegistro;
    private String mensagem;
}
