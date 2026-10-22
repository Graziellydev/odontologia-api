
package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.dto.LoginResponseDTO;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AutenticacaoService {

    private final UsuarioRepository usuarios;

    public AutenticacaoService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO autenticar(LoginDTO dto) {

        if (dto == null ||
                dto.getEmail() == null ||
                dto.getSenha() == null) {

            throw new IllegalArgumentException("Credenciais inválidas");
        }

        Usuario usuario = usuarios
                .findByEmail(dto.getEmail().trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("Credenciais inválidas")
                );

        if (!Boolean.TRUE.equals(usuario.getAtivo()) ||
                !usuario.getSenhaHash().equals(dto.getSenha())) {

            throw new IllegalArgumentException("Credenciais inválidas");
        }

        return resposta(usuario);
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO usuarioAtual(Long id) {

        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuário não encontrado")
                );

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new IllegalArgumentException("Usuário inativo");
        }

        return resposta(usuario);
    }

    private LoginResponseDTO resposta(Usuario usuario) {

        return new LoginResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getMatriculaOuRegistro(),
                "Acesso autorizado"
        );
    }
}
