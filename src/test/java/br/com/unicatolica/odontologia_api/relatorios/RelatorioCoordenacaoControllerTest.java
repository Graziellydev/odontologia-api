package br.com.unicatolica.odontologia_api.relatorios;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import br.com.unicatolica.odontologia_api.config.SecurityConfig;
import br.com.unicatolica.odontologia_api.relatorios.controller.RelatorioCoordenacaoController;
import br.com.unicatolica.odontologia_api.relatorios.dto.*;
import br.com.unicatolica.odontologia_api.relatorios.service.RelatorioCoordenacaoService;

@WebMvcTest(RelatorioCoordenacaoController.class)
@Import(SecurityConfig.class)
class RelatorioCoordenacaoControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean RelatorioCoordenacaoService service;
    @Test void semSessaoRetorna401() throws Exception { mvc.perform(get("/api/coordenador/relatorios/demografico")).andExpect(status().isUnauthorized()); }
    @Test void alunoRetorna403() throws Exception { mvc.perform(get("/api/coordenador/relatorios/demografico").session(session("ALUNO"))).andExpect(status().isForbidden()); }
    @Test void secretariaRetorna403() throws Exception { mvc.perform(get("/api/coordenador/relatorios/demografico").session(session("SECRETARIA"))).andExpect(status().isForbidden()); }
    @Test void coordenadorRecebeAgregadosSemCamposPessoais() throws Exception {
        when(service.demografico(null,null,null,null,null)).thenReturn(new RelatorioDemograficoResponse(
            new FiltrosRelatorioResponse(null,null,null,null,"TODOS"), new RelatorioDemograficoResponse.BaseAnalise(4,"Pacientes únicos no recorte"),
            new DistribuicaoItemResponse("30 a 44 anos",3,75), new DistribuicaoItemResponse("Centro",3,75),
            List.of(new DistribuicaoItemResponse("30 a 44 anos",3,75)), List.of(new DistribuicaoItemResponse("Centro",3,75))));
        mvc.perform(get("/api/coordenador/relatorios/demografico").session(session("COORDENADOR")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.baseAnalise.totalPacientesUnicos").value(4))
            .andExpect(jsonPath("$.distribuicaoFaixaEtaria[0].percentual").value(75))
            .andExpect(jsonPath("$.cpf").doesNotExist()).andExpect(jsonPath("$.nomeCompleto").doesNotExist())
            .andExpect(jsonPath("$.telefone").doesNotExist()).andExpect(jsonPath("$.pacienteId").doesNotExist());
    }
    @Test void exportaCsvEJsonComoAttachmentSemPII() throws Exception {
        var response = new RelatorioDemograficoResponse(new FiltrosRelatorioResponse(null,null,null,null,"TODOS"),
            new RelatorioDemograficoResponse.BaseAnalise(3,"Pacientes únicos no recorte"),null,null,
            List.of(new DistribuicaoItemResponse("Até 17 anos",3,100)),List.of());
        when(service.demografico(null,null,null,null,null)).thenReturn(response);
        when(service.exportarDemografico(response,"CSV")).thenReturn("relatorio,categoria\n\"Perfil\",\"Até 17 anos\"");
        when(service.exportarDemografico(response,"JSON")).thenReturn("{\"aviso\":\"agregado\",\"dados\":{}}");
        mvc.perform(get("/api/coordenador/relatorios/demografico/exportar").session(session("COORDENADOR")))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition",org.hamcrest.Matchers.containsString("attachment")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("cpf"))));
        mvc.perform(get("/api/coordenador/relatorios/demografico/exportar").param("formato","JSON").session(session("COORDENADOR")))
            .andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("cpf"))));
    }
    private MockHttpSession session(String perfil) {
        SecurityContext c=SecurityContextHolder.createEmptyContext();
        c.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("test",null,List.of(new SimpleGrantedAuthority(perfil))));
        MockHttpSession s=new MockHttpSession(); s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,c); return s;
    }
}
