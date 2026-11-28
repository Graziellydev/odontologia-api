package br.com.unicatolica.odontologia_api.consultas.controller;

import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.AlunoAgendamentoDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.DisponibilidadeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.FilaAgendamentoDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.NotificacaoConsultaDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.RemarcacaoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.service.AgendamentoService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Validated
public class AgendamentoController {

    private final AgendamentoService service;
    private final SessaoAluno sessao;

    @GetMapping("/fila-disponivel")
    public List<FilaAgendamentoDTO> filaDisponivel(HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarFila();
    }

    @GetMapping("/alunos-elegiveis")
    public List<AlunoAgendamentoDTO> alunosElegiveis(
            @RequestParam @Positive Long filaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam AgendamentoRequestDTO.Turno turno,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarAlunos(filaId, data, turno);
    }

    @GetMapping("/disponibilidade")
    public List<DisponibilidadeDTO> disponibilidade(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) @Positive Long filaId,
            @RequestParam(required = false) @Positive Long alunoId,
            @RequestParam(required = false) @Positive Long consultaIgnorada,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarDisponibilidade(inicio, fim, filaId, alunoId, consultaIgnorada);
    }

    @GetMapping("/notificacoes")
    public List<NotificacaoConsultaDTO> notificacoes(
            @RequestParam(required = false) @Positive Long consultaId,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarNotificacoes(consultaId);
    }

    @GetMapping
    public List<AgendamentoResponseDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) @Positive Long alunoId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String busca,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarAgendamentos(data, alunoId, status, busca);
    }

    @GetMapping("/{id}")
    public AgendamentoResponseDTO buscar(@PathVariable @Positive Long id, HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.buscarAgendamento(id);
    }

    @PostMapping
    public ResponseEntity<AgendamentoResponseDTO> agendar(
            @Valid @RequestBody AgendamentoRequestDTO body,
            HttpServletRequest request) {
        long secretariaId = sessao.exigirSecretaria(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.agendar(body, secretariaId));
    }

    @PatchMapping("/{id}/cancelar")
    public AgendamentoResponseDTO cancelar(@PathVariable @Positive Long id, HttpServletRequest request) {
        return service.cancelar(id, sessao.exigirSecretaria(request));
    }

    @PostMapping("/{id}/remarcar")
    public AgendamentoResponseDTO remarcar(
            @PathVariable @Positive Long id,
            @Valid @RequestBody RemarcacaoRequestDTO body,
            HttpServletRequest request) {
        return service.remarcar(id, body, sessao.exigirSecretaria(request));
    }
}
