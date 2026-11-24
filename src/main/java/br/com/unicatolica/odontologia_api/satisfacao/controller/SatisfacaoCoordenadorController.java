package br.com.unicatolica.odontologia_api.satisfacao.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse;
import br.com.unicatolica.odontologia_api.satisfacao.service.RelatorioSatisfacaoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/coordenador/satisfacao")
@PreAuthorize("hasAuthority('COORDENADOR')")
@RequiredArgsConstructor
public class SatisfacaoCoordenadorController {

    private final RelatorioSatisfacaoService service;

    @GetMapping
    public RelatorioSatisfacaoResponse obterRelatorio(
            @RequestParam(required = false) String semestre,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return service.obterRelatorio(semestre, pagina, tamanho);
    }
}