package br.com.unicatolica.odontologia_api.controller;

import br.com.unicatolica.odontologia_api.dto.LoginDTO;
import br.com.unicatolica.odontologia_api.dto.LoginResponseDTO;
import br.com.unicatolica.odontologia_api.dto.EsqueciSenhaRequest;
import br.com.unicatolica.odontologia_api.dto.MensagemAuthResponse;
import br.com.unicatolica.odontologia_api.dto.RedefinirSenhaRequest;
import br.com.unicatolica.odontologia_api.security.PasswordRecoveryRateLimiter;
import br.com.unicatolica.odontologia_api.service.PasswordRecoveryService;
import br.com.unicatolica.odontologia_api.service.AutenticacaoService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

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
import org.springframework.security.web.csrf.CsrfToken;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;
    private final PasswordRecoveryService passwordRecoveryService;
    private final PasswordRecoveryRateLimiter rateLimiter;
    private final UsuarioRepository usuarioRepository;

    public AutenticacaoController(AutenticacaoService autenticacaoService,
                                  PasswordRecoveryService passwordRecoveryService,
                                  PasswordRecoveryRateLimiter rateLimiter,
                                  UsuarioRepository usuarioRepository) {
        this.autenticacaoService = autenticacaoService;
        this.passwordRecoveryService = passwordRecoveryService;
        this.rateLimiter = rateLimiter;
        this.usuarioRepository = usuarioRepository;
    }

    private final HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/csrf")
    public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
        // Materializa o token no cookie XSRF-TOKEN para clientes browser enviarem no header.
        csrfToken.getToken();
        return ResponseEntity.noContent().build();
    }

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
                    .body("Login ou senha inválidos");
        }
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<MensagemAuthResponse> esqueciSenha(
            @Valid @RequestBody EsqueciSenhaRequest dto,
            HttpServletRequest request) {
        if (rateLimiter.permitir(request.getRemoteAddr(), dto.identificador())) {
            passwordRecoveryService.solicitar(dto.identificador());
        }
        return ResponseEntity.ok(new MensagemAuthResponse(PasswordRecoveryService.MENSAGEM_NEUTRA));
    }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<MensagemAuthResponse> redefinirSenha(
            @Valid @RequestBody RedefinirSenhaRequest dto,
            HttpServletRequest request) {
        if (!rateLimiter.permitir(request.getRemoteAddr(), dto.identificador())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new MensagemAuthResponse("Muitas tentativas. Aguarde e tente novamente."));
        }
        boolean redefinida = passwordRecoveryService.redefinir(dto.identificador(), dto.codigo(), dto.novaSenha());
        if (!redefinida) {
            return ResponseEntity.badRequest()
                    .body(new MensagemAuthResponse("Código inválido ou expirado."));
        }
        return ResponseEntity.ok(new MensagemAuthResponse("Senha redefinida com sucesso. Entre com a nova senha."));
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

        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
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
