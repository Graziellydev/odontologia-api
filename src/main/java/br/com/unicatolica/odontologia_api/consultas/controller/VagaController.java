package br.com.unicatolica.odontologia_api.consultas.controller;

import br.com.unicatolica.odontologia_api.consultas.dto.BloqueioVagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.BloqueioTurnoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.TurnoGradeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.service.GestaoVagasService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vagas")
@RequiredArgsConstructor
@Validated
public class VagaController {

    private final GestaoVagasService service;
    private final SessaoAluno sessao;

    @PostMapping("/geracoes")
    public ResponseEntity<GerarTurnosResponseDTO> gerarTurnos(
            @Valid @RequestBody GerarTurnosRequestDTO body, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return ResponseEntity.ok(service.gerarTurnos(body));
    }

    @GetMapping("/grade")
    public List<TurnoGradeDTO> listarGrade(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String disciplina,
            @RequestParam(required = false) String disponibilidade,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarGradeSemanal(inicio, fim, disciplina, disponibilidade);
    }

    @PatchMapping("/bloqueio-turno")
    public VagaResponseDTO bloquearTurno(
            @Valid @RequestBody BloqueioTurnoRequestDTO body, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.bloquearTurno(body);
    }

    @PostMapping
    public ResponseEntity<VagaResponseDTO> criar(
            @Valid @RequestBody VagaRequestDTO body, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        VagaResponseDTO vaga = service.criar(body);
        return ResponseEntity.created(URI.create("/api/vagas/" + vaga.id())).body(vaga);
    }

    @GetMapping
    public List<VagaResponseDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) @Positive Long cadeiraId,
            @RequestParam(required = false) VagaRequestDTO.Turno turno,
            @RequestParam(required = false) Boolean bloqueada,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listar(inicio, fim, cadeiraId, turno, bloqueada);
    }

    @GetMapping("/{id}")
    public VagaResponseDTO buscar(@PathVariable @Positive Long id, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.buscar(id);
    }

    @PatchMapping("/{id}/bloqueio")
    public VagaResponseDTO bloquear(
            @PathVariable @Positive Long id,
            @Valid @RequestBody BloqueioVagaRequestDTO body,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.bloquear(id, body);
    }

    @PatchMapping("/{id}/desbloqueio")
    public VagaResponseDTO desbloquear(@PathVariable @Positive Long id, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.desbloquear(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable @Positive Long id, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        service.remover(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
