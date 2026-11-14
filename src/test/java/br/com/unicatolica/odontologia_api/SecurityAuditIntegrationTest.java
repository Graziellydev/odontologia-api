package br.com.unicatolica.odontologia_api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    }
}
