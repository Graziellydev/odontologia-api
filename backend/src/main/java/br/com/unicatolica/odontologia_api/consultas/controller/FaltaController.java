package br.com.unicatolica.odontologia_api.consultas.controller;

import br.com.unicatolica.odontologia_api.consultas.dto.AusenciaResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.FaltaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.ReagendamentoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.service.ConsultaFaltaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/consultas")
@RequiredArgsConstructor
@Validated
public class FaltaController {

    private final ConsultaFaltaService service;

    @PostMapping("/{id}/faltas")
    public ResponseEntity<Void> registrarFalta(
            @PathVariable Long id,
            @Valid @RequestBody FaltaRequestDTO request) {
        service.registrarFalta(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/reagendamentos")
    public ResponseEntity<Long> reagendar(
            @PathVariable Long id,
            @Valid @RequestBody ReagendamentoRequestDTO request) {
        Long novaConsultaId = service.reagendar(id, request);
        return ResponseEntity.created(URI.create("/api/consultas/" + novaConsultaId))
                .body(novaConsultaId);
    }

    @GetMapping("/faltas")
    public List<AusenciaResponseDTO> listarFaltas(
            @RequestParam(required = false) @Min(1) @Max(12) Integer mes,
            @RequestParam(required = false) FaltaRequestDTO.QuemFaltou quemFaltou) {
        return service.listarFaltas(mes, quemFaltou);
    }
}
