package br.com.unicatolica.odontologia_api.security;

import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SessaoAluno {

    private final UsuarioRepository usuarios;

    public SessaoAluno(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    public Long exigirAluno(HttpServletRequest request) {
        return exigirPerfil(Perfil.ALUNO);
    }

    public Long exigirSecretaria(HttpServletRequest request) {
        return exigirPerfil(Perfil.SECRETARIA);
    }

    private Long exigirPerfil(Perfil perfilEsperado) {

        Authentication autenticacao =
                SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao == null ||
                !autenticacao.isAuthenticated() ||
                autenticacao instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Faça login para continuar"
            );
        }

        String email = autenticacao.getName();

        Usuario usuario = usuarios.findByEmail(email)
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

        return usuario.getId();
    }
}