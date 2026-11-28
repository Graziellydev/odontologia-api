package br.com.unicatolica.odontologia_api.filaespera.controller;

import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaAtualizadaDTO;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import br.com.unicatolica.odontologia_api.filaespera.dto.NovaEntradaFilaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.EntradaFilaCriadaDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import br.com.unicatolica.odontologia_api.filaespera.dto.TempoMedioEsperaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaItemDTO;
import br.com.unicatolica.odontologia_api.filaespera.service.FilaEsperaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fila-espera")
@PreAuthorize("hasAuthority('SECRETARIA')")
public class FilaEsperaController {

    private final FilaEsperaService filaEsperaService;

    public FilaEsperaController(
            FilaEsperaService filaEsperaService) {
        this.filaEsperaService = filaEsperaService;
    }

    @GetMapping
    public List<FilaEsperaItemDTO> listarFila(
            @RequestParam(required = false) Long procedimentoId,
            @RequestParam(required = false) String especialidade,
            @RequestParam(required = false) String dente,
            @RequestParam(required = false) String classe,
            @RequestParam(required = false) String nivelUrgencia,
            @RequestParam(required = false) Integer diasEsperaMin,
            @RequestParam(required = false) Integer diasEsperaMax) {

        return filaEsperaService.listarFila(
                procedimentoId,
                especialidade,
                dente,
                classe,
                nivelUrgencia,
                diasEsperaMin,
                diasEsperaMax);
    }

    @GetMapping("/metricas/tempo-medio")
    public List<TempoMedioEsperaDTO> consultarTempoMedio() {
        return filaEsperaService.calcularTempoMedioPorEspecialidade();
    }

    @PostMapping
    public ResponseEntity<EntradaFilaCriadaDTO> adicionarEntrada(
            @Valid @RequestBody NovaEntradaFilaDTO dto) {

        EntradaFilaCriadaDTO entrada = filaEsperaService.adicionarEntrada(dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(entrada);
    }

    @PatchMapping("/{id}/alocar")
    public FilaEsperaAtualizadaDTO alocarPaciente(
            @PathVariable Long id) {

        return filaEsperaService.alocarPaciente(id);
    }
}
