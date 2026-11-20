package br.com.unicatolica.odontologia_api.prontuarios.controller;

import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.prontuarios.dto.ProntuarioHistoricoResponse;
import br.com.unicatolica.odontologia_api.prontuarios.dto.RegistrarAdendoRequest;
import br.com.unicatolica.odontologia_api.prontuarios.dto.RegistrarProntuarioRequest;
import br.com.unicatolica.odontologia_api.prontuarios.dto.SalvarAdendoResponse;
import br.com.unicatolica.odontologia_api.prontuarios.dto.SalvarProntuarioResponse;
import br.com.unicatolica.odontologia_api.prontuarios.service.ProntuarioService;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/prontuarios")
@RequiredArgsConstructor
public class ProntuarioController {
    private final ProntuarioService prontuarios;
    private final SessaoAluno sessaoAluno;
    private final UsuarioRepository usuarios;

    @GetMapping("/paciente/{pacienteId}")
    public ProntuarioHistoricoResponse historico(@PathVariable long pacienteId,
                                                  Authentication authentication,
                                                  HttpServletRequest request) {
        Usuario usuario = buscarUsuario(authentication);
        if (usuario.getPerfil() == Perfil.COORDENADOR) {
            return prontuarios.buscarHistorico(pacienteId, null);
        }
        return prontuarios.buscarHistorico(pacienteId, sessaoAluno.exigirAluno(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SalvarProntuarioResponse salvar(@Valid @RequestBody RegistrarProntuarioRequest request,
                                            HttpServletRequest servletRequest) {
        long alunoId = sessaoAluno.exigirAluno(servletRequest);
        return prontuarios.salvar(alunoId, request);
    }

    @PostMapping("/{prontuarioId}/adendos")
    @ResponseStatus(HttpStatus.CREATED)
    public SalvarAdendoResponse adicionarAdendo(@PathVariable long prontuarioId,
                                                 @Valid @RequestBody RegistrarAdendoRequest request,
                                                 HttpServletRequest servletRequest) {
        long alunoId = sessaoAluno.exigirAluno(servletRequest);
        return prontuarios.adicionarAdendo(prontuarioId, alunoId, request);
    }

    private Usuario buscarUsuario(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Faça login para continuar.");
        }
        Usuario usuario = usuarios.findByEmail(authentication.getName()).orElseThrow(() ->
                new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Usuário sem permissão para esta operação.");
        }
        return usuario;
    }
}
