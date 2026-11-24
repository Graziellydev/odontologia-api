package br.com.unicatolica.odontologia_api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.LinhaHistorico;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Modalidade;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Resumo;
import br.com.unicatolica.odontologia_api.service.DesempenhoAcademicoService;

@WebMvcTest(controllers = DesempenhoAcademicoController.class)
@Import({ SecurityConfig.class, DesempenhoAcademicoExceptionHandler.class })
class DesempenhoAcademicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DesempenhoAcademicoService service;

    @Test
    void exigeAutenticacaoEPerfilCoordenador() throws Exception {
        mockMvc.perform(get("/api/coordenador/desempenho-historico"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"));

        mockMvc.perform(get("/api/coordenador/desempenho-historico")
                        .session(sessaoAutenticada("ALUNO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));

        mockMvc.perform(get("/api/coordenador/desempenho-historico")
                        .session(sessaoAutenticada("SECRETARIA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void coordenadorRecebeSomenteIndicadoresAcademicos() throws Exception {
        when(service.consultar("ALUNO", "A001", "2026.1"))
                .thenReturn(new DesempenhoAcademicoResponse(
                        Modalidade.ALUNO, "A001", "2026.1", "Ana", "A001", null,
                        new Resumo(2, 1, 1, 50),
                        List.of(new LinhaHistorico("Dentística", "Restauração", 2, 1, 1, 50)),
                        List.of()));

        mockMvc.perform(get("/api/coordenador/desempenho-historico")
                        .param("modalidade", "ALUNO")
                        .param("identificador", "A001")
                        .param("semestre", "2026.1")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumo.exigidos").value(2))
                .andExpect(jsonPath("$.historico[0].procedimento").value("Restauração"))
                .andExpect(jsonPath("$.alunos").isEmpty())
                .andExpect(jsonPath("$.pacientes").doesNotExist())
                .andExpect(jsonPath("$.prontuarios").doesNotExist());

        verify(service).consultar("ALUNO", "A001", "2026.1");
    }

    private MockHttpSession sessaoAutenticada(String perfil) {
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "usuario-teste",
                null,
                List.of(new SimpleGrantedAuthority(perfil))));

        MockHttpSession sessao = new MockHttpSession();
        sessao.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, contexto);
        return sessao;
    }
}
