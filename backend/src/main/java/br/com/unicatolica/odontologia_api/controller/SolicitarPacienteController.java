
package br.com.unicatolica.odontologia_api.controller;

import br.com.unicatolica.odontologia_api.dto.*;
import br.com.unicatolica.odontologia_api.model.SolicitarPaciente;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import br.com.unicatolica.odontologia_api.service.SolicitarPacienteService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/solicitacoes-pacientes")
public class SolicitarPacienteController {

    private final SolicitarPacienteService service;
    private final SessaoAluno sessao;

    public SolicitarPacienteController(
            SolicitarPacienteService service,
            SessaoAluno sessao
    ) {
        this.service = service;
        this.sessao = sessao;
    }

    // O aluno envia uma solicitação.
    @PostMapping
    public ResponseEntity<SolicitarPaciente> solicitar(
            @Valid @RequestBody SolicitarPacienteDTO dto,
            HttpServletRequest request
    ) {
        Long alunoLogado = sessao.exigirAluno(request);

        if (!alunoLogado.equals(dto.alunoId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Aluno diferente do usuário autenticado"
            );
        }

        SolicitarPaciente solicitacao = service.solicitar(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(solicitacao);
    }

    // A secretaria consulta todas as solicitações.
    @GetMapping
    public List<SolicitarPaciente> listarTodas(
            HttpServletRequest request
    ) {
        sessao.exigirSecretaria(request);
        return service.listarTodas();
    }

    // A secretaria consulta solicitações pendentes.
    @GetMapping("/pendentes")
    public List<SolicitarPaciente> listarPendentes(
            HttpServletRequest request
    ) {
        sessao.exigirSecretaria(request);
        return service.listarPendentes();
    }

    // O aluno consulta apenas as próprias solicitações.
    @GetMapping("/aluno/{alunoId}")
    public List<SolicitarPaciente> listarPorAluno(
            @PathVariable Long alunoId,
            HttpServletRequest request
    ) {
        Long alunoLogado = sessao.exigirAluno(request);

        if (!alunoLogado.equals(alunoId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você só pode consultar suas próprias solicitações"
            );
        }

        return service.listarPorAluno(alunoId);
    }

    // Operações da secretaria: continuam no backend.
    @GetMapping("/{id}/candidatos")
    public List<CandidatoFilaDTO> candidatos(
            @PathVariable Long id,
            HttpServletRequest request
    ) {
        sessao.exigirSecretaria(request);
        return service.candidatos(id);
    }

    @GetMapping("/vagas-disponiveis")
    public List<VagaDisponivelDTO> vagasDisponiveis(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate data,
            HttpServletRequest request
    ) {
        sessao.exigirSecretaria(request);
        return service.vagasDisponiveis(data);
    }

    @PatchMapping("/{id}/aprovar")
    public SolicitarPaciente aprovar(
            @PathVariable Long id,
            @Valid @RequestBody AnaliseSolicitacaoDTO dto,
            HttpServletRequest request
    ) {
        Long secretariaLogada = sessao.exigirSecretaria(request);

        if (!secretariaLogada.equals(dto.secretariaId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Secretaria diferente da sessão autenticada"
            );
        }

        return service.aprovar(id, dto);
    }

    @PatchMapping("/{id}/recusar")
    public SolicitarPaciente recusar(
            @PathVariable Long id,
            @Valid @RequestBody AnaliseSolicitacaoDTO dto,
            HttpServletRequest request
    ) {
        Long secretariaLogada = sessao.exigirSecretaria(request);

        if (!secretariaLogada.equals(dto.secretariaId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Secretaria diferente da sessão autenticada"
            );
        }

        return service.recusar(id, dto);
    }

    @PatchMapping("/{id}/vincular")
    public SolicitarPaciente vincular(
            @PathVariable Long id,
            @Valid @RequestBody VincularPacienteDTO dto,
            HttpServletRequest request
    ) {
        Long secretariaLogada = sessao.exigirSecretaria(request);

        if (!secretariaLogada.equals(dto.secretariaId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Secretaria diferente da sessão autenticada"
            );
        }

        return service.vincular(id, dto);
    }
}
