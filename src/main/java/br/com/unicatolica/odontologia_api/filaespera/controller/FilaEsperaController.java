package br.com.unicatolica.odontologia_api.filaespera.controller;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.service.FilaEsperaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fila-espera")
public class FilaEsperaController {

    private final FilaEsperaService filaEsperaService;

    public FilaEsperaController(FilaEsperaService filaEsperaService) {
        this.filaEsperaService = filaEsperaService;
    }

    @GetMapping
    public List<FilaEspera> listarFila() {
        return filaEsperaService.listarFila();
    }

    @GetMapping("/status/{statusFila}")
    public List<FilaEspera> listarPorStatus(@PathVariable String statusFila) {
        return filaEsperaService.listarPorStatus(statusFila);
    }
}