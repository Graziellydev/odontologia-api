package br.com.unicatolica.odontologia_api.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse;
import br.com.unicatolica.odontologia_api.service.DesempenhoAcademicoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coordenador/desempenho-historico")
@PreAuthorize("hasAuthority('COORDENADOR')")
@RequiredArgsConstructor
public class DesempenhoAcademicoController {

    private final DesempenhoAcademicoService service;

    @GetMapping
    public DesempenhoAcademicoResponse consultar(
            @RequestParam(required = false) String modalidade,
            @RequestParam(required = false) String identificador,
            @RequestParam(required = false) String semestre) {
        return service.consultar(modalidade, identificador, semestre);
    }
}
