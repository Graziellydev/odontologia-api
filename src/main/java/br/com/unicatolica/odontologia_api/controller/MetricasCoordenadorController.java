package br.com.unicatolica.odontologia_api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.unicatolica.odontologia_api.dto.MetricasCoordenadorResponse;
import br.com.unicatolica.odontologia_api.dto.SemestresLetivosResponse;
import br.com.unicatolica.odontologia_api.service.MetricasCoordenadorService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coordenador/metricas")
@PreAuthorize("hasAuthority('COORDENADOR')")
@RequiredArgsConstructor
public class MetricasCoordenadorController {

    private final MetricasCoordenadorService service;

    @GetMapping("/semestres")
    public SemestresLetivosResponse listarSemestres() {
        return service.listarSemestres();
    }

    @GetMapping
    public MetricasCoordenadorResponse obterMetricas(
            @RequestParam(required = false) String semestre) {
        return service.obterMetricas(semestre);
    }
}