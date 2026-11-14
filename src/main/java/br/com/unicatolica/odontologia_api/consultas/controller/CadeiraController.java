package br.com.unicatolica.odontologia_api.consultas.controller;

import br.com.unicatolica.odontologia_api.consultas.dto.CadeiraResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.service.GestaoVagasService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cadeiras")
@RequiredArgsConstructor
public class CadeiraController {

    private final GestaoVagasService service;
    private final SessaoAluno sessao;

    @GetMapping
    public List<CadeiraResponseDTO> listar(HttpServletRequest request) {
        sessao.exigirSecretaria(request);
        return service.listarCadeiras();
    }
}
