package br.com.unicatolica.odontologia_api.notificacoes.controller;

import br.com.unicatolica.odontologia_api.notificacoes.dto.NotificacaoResumoDTO;
import br.com.unicatolica.odontologia_api.notificacoes.dto.ProcessamentoLembretesDTO;
import br.com.unicatolica.odontologia_api.notificacoes.repository.NotificacaoConsultaQueryRepository;
import br.com.unicatolica.odontologia_api.notificacoes.service.Lembrete24hService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Fortaleza");

    private final NotificacaoConsultaQueryRepository notificacoes;
    private final Lembrete24hService lembretes;
    private final SessaoAluno sessao;

    @GetMapping
    public List<NotificacaoResumoDTO> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String status,
            HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periodo invalido.");
        }
        return notificacoes.listar(inicio, fim, tipo, status);
    }

    @PostMapping("/processar-lembretes-24h")
    public ProcessamentoLembretesDTO processar(HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return new ProcessamentoLembretesDTO(lembretes.processar(LocalDate.now(ZONA_CLINICA)));
    }
}
