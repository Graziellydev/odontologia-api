package br.com.unicatolica.odontologia_api.retornos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import br.com.unicatolica.odontologia_api.retornos.dto.AtendimentoDoTurnoDTO;
import br.com.unicatolica.odontologia_api.retornos.dto.RetornoResponse;
import br.com.unicatolica.odontologia_api.retornos.dto.SugestaoRetornoRequest;
import br.com.unicatolica.odontologia_api.retornos.service.RetornoService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/retornos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ALUNO')")
public class RetornoController {

    private final RetornoService service;
    private final SessaoAluno sessao;

    @GetMapping("/meus-atendimentos")
    public List<AtendimentoDoTurnoDTO> meusAtendimentos(HttpServletRequest request) {
        Long alunoId = sessao.exigirAluno(request);
        return service.listarAtendimentosDeHoje(alunoId);
    }

    @PostMapping
    public RetornoResponse registrar(@Valid @RequestBody SugestaoRetornoRequest dto,
                                     HttpServletRequest request) {
        Long alunoId = sessao.exigirAluno(request);
        return service.registrar(alunoId, dto);
    }
}
