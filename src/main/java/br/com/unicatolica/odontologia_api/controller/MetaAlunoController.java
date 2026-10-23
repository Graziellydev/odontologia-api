package br.com.unicatolica.odontologia_api.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.unicatolica.odontologia_api.dto.PainelMetasResponse;
import br.com.unicatolica.odontologia_api.service.MetaAlunoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alunos")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MetaAlunoController {

    private final MetaAlunoService service;

    @GetMapping("/{alunoId}/metas")
    public PainelMetasResponse obterPainelDeMetas(
            @PathVariable Long alunoId,
            @RequestParam(required = false) String semestre) {
        return service.obterPainelDeMetas(alunoId, semestre);
    }
}