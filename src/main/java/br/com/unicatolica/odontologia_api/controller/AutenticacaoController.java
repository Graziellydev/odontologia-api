package br.com.unicatolica.odontologia_api.controller;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.dto.LoginResponseDTO;
import br.com.unicatolica.odontologia_api.service.AutenticacaoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    @Autowired
    private AutenticacaoService autenticacaoService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginDTO dto,
            HttpServletRequest request,
            HttpServletResponse response) {

        try {
            LoginResponseDTO resposta = autenticacaoService.autenticar(dto);

            var autoridade = new SimpleGrantedAuthority(
                    resposta.getPerfil().name());

            var autenticacao = UsernamePasswordAuthenticationToken.authenticated(
                    resposta.getEmail(),
                    null,
                    List.of(autoridade));

            SecurityContext contexto = SecurityContextHolder.createEmptyContext();

            contexto.setAuthentication(autenticacao);
            SecurityContextHolder.setContext(contexto);

            request.getSession(true);
            request.changeSessionId();

            securityContextRepository.saveContext(
                    contexto,
                    request,
                    response);

            return ResponseEntity.ok(resposta);

        } catch (BadCredentialsException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("E-mail ou senha incorretos.");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {

        HttpSession sessao = request.getSession(false);

        if (sessao != null) {
            sessao.invalidate();
        }

        SecurityContextHolder.clearContext();

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> usuarioAtual(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return usuarioRepository.findByEmail(authentication.getName())
                .<ResponseEntity<?>>map(usuario -> ResponseEntity.ok(
                        new LoginResponseDTO(
                                usuario.getId(),
                                usuario.getNome(),
                                usuario.getEmail(),
                                usuario.getPerfil(),
                                usuario.getMatriculaOuRegistro(),
                                "Usuário autenticado"
                        )
                ))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
                );
    }
}
