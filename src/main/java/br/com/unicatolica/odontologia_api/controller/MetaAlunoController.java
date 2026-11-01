package br.com.unicatolica.odontologia_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.unicatolica.odontologia_api.dto.PainelMetasResponse;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import br.com.unicatolica.odontologia_api.service.MetaAlunoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/metas")
@RequiredArgsConstructor
public class MetaAlunoController {

    private final MetaAlunoService service;
    private final SessaoAluno sessao;

    @GetMapping("/minhas")
    public PainelMetasResponse minhasMetas(
            @RequestParam(required = false) String semestre,
            HttpServletRequest request) {
        Long alunoId = sessao.exigirAluno(request);
        return service.obterPainelDeMetas(alunoId, semestre);
    }
}