package br.com.unicatolica.odontologia_api.relatorios.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.unicatolica.odontologia_api.relatorios.dto.RelatorioDemandaClinicaResponse;
import br.com.unicatolica.odontologia_api.relatorios.dto.RelatorioDemograficoResponse;
import br.com.unicatolica.odontologia_api.relatorios.service.RelatorioCoordenacaoService;

@RestController
@RequestMapping("/api/coordenador/relatorios")
@PreAuthorize("hasAuthority('COORDENADOR')")
public class RelatorioCoordenacaoController {
    private final RelatorioCoordenacaoService service;
    public RelatorioCoordenacaoController(RelatorioCoordenacaoService service) { this.service = service; }

    @GetMapping("/demografico")
    public RelatorioDemograficoResponse demografico(@RequestParam(required=false) String semestre,
            @RequestParam(required=false) String dataInicio, @RequestParam(required=false) String dataFim,
            @RequestParam(required=false) String especialidade, @RequestParam(required=false) String regiao) {
        return service.demografico(semestre, dataInicio, dataFim, especialidade, regiao);
    }
    @GetMapping("/demanda-clinica")
    public RelatorioDemandaClinicaResponse demanda(@RequestParam(required=false) String semestre,
            @RequestParam(required=false) String dataInicio, @RequestParam(required=false) String dataFim,
            @RequestParam(required=false) String especialidade, @RequestParam(required=false) String regiao) {
        return service.demanda(semestre, dataInicio, dataFim, especialidade, regiao);
    }
    @GetMapping("/demografico/exportar")
    public ResponseEntity<byte[]> exportarDemografico(@RequestParam(required=false) String semestre,
            @RequestParam(required=false) String dataInicio, @RequestParam(required=false) String dataFim,
            @RequestParam(required=false) String especialidade, @RequestParam(required=false) String regiao,
            @RequestParam(defaultValue="CSV") String formato) {
        return attachment(service.exportarDemografico(service.demografico(semestre,dataInicio,dataFim,especialidade,regiao), formato), formato, "demografico");
    }
    @GetMapping("/demanda-clinica/exportar")
    public ResponseEntity<byte[]> exportarDemanda(@RequestParam(required=false) String semestre,
            @RequestParam(required=false) String dataInicio, @RequestParam(required=false) String dataFim,
            @RequestParam(required=false) String especialidade, @RequestParam(required=false) String regiao,
            @RequestParam(defaultValue="CSV") String formato) {
        return attachment(service.exportarDemanda(service.demanda(semestre,dataInicio,dataFim,especialidade,regiao), formato), formato, "demanda-clinica");
    }
    private ResponseEntity<byte[]> attachment(String body, String formato, String nome) {
        boolean json = "JSON".equalsIgnoreCase(formato);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(json ? MediaType.APPLICATION_JSON : new MediaType("text", "csv", StandardCharsets.UTF_8));
        h.setContentDisposition(ContentDisposition.attachment().filename(nome + "." + (json ? "json" : "csv"), StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(h).body(body.getBytes(StandardCharsets.UTF_8));
    }
}
