package br.com.unicatolica.odontologia_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static br.com.unicatolica.odontologia_api.CsrfTestSupport.csrf;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAuditIntegrationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void rotasDeDadosProtegidosRetornam401SemSessao() throws Exception {
        mockMvc.perform(get("/api/pacientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/vagas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/consultas/faltas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/retornos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/metas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/catalogo-solicitacoes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/fila-espera")).andExpect(status().isUnauthorized());
    }

    @Test
    void bearerMalformadoOuExpiradoSemSessaoNaoAutentica() throws Exception {
        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer token-malformado"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/pacientes").header("Authorization", "Bearer " + "a.".repeat(16)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emiteCookieCsrfERejeitaMutacaoSemHeaderDoToken() throws Exception {
        mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/auth/logout").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void alunoNaoAcessaDadosDePacientesNemOperacoesDaSecretaria() throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "aluno@teste.invalid", null, List.of(new SimpleGrantedAuthority("ALUNO"))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        mockMvc.perform(get("/api/pacientes").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/vagas").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/consultas/faltas").session(session)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/solicitacoes-pacientes/pendentes").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/coordenador/metricas").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/notificacoes").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/consultas/notificacoes?consultaId=72").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/fila-espera").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void secretariaNaoAcessaRelatoriosNemEscreveProntuario() throws Exception {
        MockHttpSession session = sessao("SECRETARIA");
        mockMvc.perform(get("/api/coordenador/metricas").session(session))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/prontuarios").session(session).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void perfisCruzadosRecebem403NasRotasPadronizadas() throws Exception {
        MockHttpSession secretaria = sessao("SECRETARIA");
        mockMvc.perform(get("/api/retornos").session(secretaria)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/metas").session(secretaria)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/catalogo-solicitacoes").session(secretaria)).andExpect(status().isForbidden());

        MockHttpSession aluno = sessao("ALUNO");
        mockMvc.perform(get("/api/fila-espera").session(aluno)).andExpect(status().isForbidden());
    }

    @Test
    void coordenadorNaoPodeEscreverProntuario() throws Exception {
        mockMvc.perform(post("/api/prontuarios").session(sessao("COORDENADOR")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession sessao(String perfil) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                perfil.toLowerCase() + "@teste.invalid", null,
                List.of(new SimpleGrantedAuthority(perfil))));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
}
