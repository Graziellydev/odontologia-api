package br.com.unicatolica.odontologia_api.solicitacoes.controller;

import br.com.unicatolica.odontologia_api.solicitacoes.dto.AprovarSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.RecusarSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.NovaSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.SolicitacaoAdministrativaDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.UsuarioIdentidadeRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.service.SolicitacaoPacienteService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes-pacientes")
public class SolicitacaoPacienteController {

    private final SolicitacaoPacienteService service;
    private final UsuarioIdentidadeRepository usuarioRepository;

    public SolicitacaoPacienteController(
            SolicitacaoPacienteService service,
            UsuarioIdentidadeRepository usuarioRepository) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ALUNO')")
    public ResponseEntity<SolicitacaoPaciente> criarSolicitacao(
            @Valid @RequestBody NovaSolicitacaoDTO dados,
            Authentication autenticacao) {

        Long alunoId = obterIdUsuario(autenticacao);

        SolicitacaoPaciente solicitacao = service.criarSolicitacao(alunoId, dados);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(solicitacao);
    }

    @GetMapping("/minhas")
    @PreAuthorize("hasAuthority('ALUNO')")
    public List<SolicitacaoPaciente> listarMinhasSolicitacoes(
            Authentication autenticacao) {

        Long alunoId = obterIdUsuario(autenticacao);

        return service.listarSolicitacoesDoAluno(alunoId);
    }

    @GetMapping("/pendentes")
    @PreAuthorize("hasAuthority('SECRETARIA')")
    public List<SolicitacaoPaciente> listarPendentes() {
        return service.listarPendentesSecretaria();
    }

    @GetMapping("/administrativas")
    @PreAuthorize("hasAuthority('SECRETARIA')")
    public List<SolicitacaoAdministrativaDTO> listarAdministrativas() {
        return service.listarSolicitacoesAdministrativas();
    }

    @PatchMapping("/{solicitacaoId}/aprovar")
    @PreAuthorize("hasAuthority('SECRETARIA')")
    public SolicitacaoPaciente aprovarSolicitacao(
            @PathVariable Long solicitacaoId,
            @Valid @RequestBody AprovarSolicitacaoDTO dados,
            Authentication autenticacao) {

        Long secretariaId = obterIdUsuario(autenticacao);

        return service.aprovarSolicitacao(
                solicitacaoId,
                dados.filaEsperaId(),
                secretariaId);
    }

    @PatchMapping("/{solicitacaoId}/recusar")
    @PreAuthorize("hasAuthority('SECRETARIA')")
    public SolicitacaoPaciente recusarSolicitacao(
            @PathVariable Long solicitacaoId,
            @Valid @RequestBody RecusarSolicitacaoDTO dados,
            Authentication autenticacao) {

        Long secretariaId = obterIdUsuario(autenticacao);

        return service.recusarSolicitacao(
                solicitacaoId,
                secretariaId,
                dados.observacaoSecretaria());
    }

    private Long obterIdUsuario(Authentication autenticacao) {

        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Usuário não autenticado.");
        }

        return usuarioRepository
                .buscarIdPorEmail(autenticacao.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário autenticado não encontrado."));
    }
}
