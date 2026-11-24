package br.com.unicatolica.odontologia_api.satisfacao.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
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
import br.com.unicatolica.odontologia_api.controller.AutenticacaoController;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse;
import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse.Avaliacao;
import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse.PorNota;
import br.com.unicatolica.odontologia_api.satisfacao.service.RelatorioSatisfacaoService;
import br.com.unicatolica.odontologia_api.security.PasswordRecoveryRateLimiter;
import br.com.unicatolica.odontologia_api.service.AutenticacaoService;
import br.com.unicatolica.odontologia_api.service.PasswordRecoveryService;

@WebMvcTest(controllers = {
        SatisfacaoCoordenadorController.class,
        AutenticacaoController.class
})
@Import(SecurityConfig.class)
class SatisfacaoCoordenadorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RelatorioSatisfacaoService service;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @MockitoBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockitoBean
    private PasswordRecoveryRateLimiter rateLimiter;

    @Test
    void deveRetornar401SemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/coordenador/satisfacao"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deveRetornar403ParaAluno() throws Exception {
        mockMvc.perform(get("/api/coordenador/satisfacao").session(sessaoAutenticada("ALUNO")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveRetornar403ParaSecretaria() throws Exception {
        mockMvc.perform(get("/api/coordenador/satisfacao").session(sessaoAutenticada("SECRETARIA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordenadorRecebeORelatorioSemDadosDoPaciente() throws Exception {
        when(service.obterRelatorio(null, 0, 10)).thenReturn(relatorio());

        mockMvc.perform(get("/api/coordenador/satisfacao").session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mediaGeral").value(4.5))
                .andExpect(jsonPath("$.totalRespostas").value(2))
                .andExpect(jsonPath("$.avaliacoes[0].aluno").value("Aluno Um"))
                .andExpect(jsonPath("$.avaliacoes[0].comentario").value("Ótimo"))
                .andExpect(jsonPath("$.avaliacoes[0].paciente").doesNotExist());
    }

    @Test
    void repassaSemestrePaginaETamanhoAoServico() throws Exception {
        when(service.obterRelatorio("2026.2", 1, 5)).thenReturn(relatorio());

        mockMvc.perform(get("/api/coordenador/satisfacao")
                        .param("semestre", "2026.2")
                        .param("pagina", "1")
                        .param("tamanho", "5")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isOk());

        verify(service).obterRelatorio("2026.2", 1, 5);
    }

    private RelatorioSatisfacaoResponse relatorio() {
        return new RelatorioSatisfacaoResponse(
                null,
                4.5,
                2L,
                List.of(new PorNota(5, 1), new PorNota(4, 1), new PorNota(3, 0),
                        new PorNota(2, 0), new PorNota(1, 0)),
                List.of(new Avaliacao(5, OffsetDateTime.parse("2026-11-20T10:00:00-03:00"),
                        "Aluno Um", "Ótimo")),
                0, 10, 1);
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