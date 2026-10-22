
package br.com.unicatolica.odontologia_api.security;

import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SessaoAluno {

    private final UsuarioRepository usuarios;

    public SessaoAluno(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Long exigirAluno(HttpServletRequest request) {
        return exigirPerfil(request, Perfil.ALUNO);
    }

    public Long exigirSecretaria(HttpServletRequest request) {
        return exigirPerfil(request, Perfil.SECRETARIA);
    }

    private Long exigirPerfil(
            HttpServletRequest request,
            Perfil perfilEsperado
    ) {

        HttpSession sessao = request.getSession(false);

        if (sessao == null ||
                !(sessao.getAttribute("usuarioId") instanceof Long id)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Faça login para continuar"
            );
        }

        Usuario usuario = usuarios.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Usuário não encontrado"
                        )
                );

        if (!Boolean.TRUE.equals(usuario.getAtivo()) ||
                usuario.getPerfil() != perfilEsperado) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Usuário sem permissão para esta operação"
            );
        }

        return id;
    }
}
