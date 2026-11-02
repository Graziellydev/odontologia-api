package br.com.unicatolica.odontologia_api.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.unicatolica.odontologia_api.config.SecurityConfig;
import br.com.unicatolica.odontologia_api.dto.MetricasCoordenadorResponse;
import br.com.unicatolica.odontologia_api.dto.SemestresLetivosResponse;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import br.com.unicatolica.odontologia_api.service.AutenticacaoService;
import br.com.unicatolica.odontologia_api.service.MetricasCoordenadorService;

@WebMvcTest(controllers = {
        MetricasCoordenadorController.class,
        AutenticacaoController.class
})
@Import({ SecurityConfig.class, MetricasCoordenadorExceptionHandler.class })
class MetricasCoordenadorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MetricasCoordenadorService metricasService;

    @MockitoBean
    private AutenticacaoService autenticacaoService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @Test
    void deveRetornar401ComJsonParaAcessoSemAutenticacao() throws Exception {
        mockMvc.perform(get("/api/coordenador/metricas/semestres"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.codigo").value("NAO_AUTENTICADO"))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária."));
    }

    @Test
    void deveRetornar403ParaAluno() throws Exception {
        mockMvc.perform(get("/api/coordenador/metricas/semestres")
                        .session(sessaoAutenticada("ALUNO")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void deveRetornar403ParaSecretaria() throws Exception {
        mockMvc.perform(get("/api/coordenador/metricas")
                        .session(sessaoAutenticada("SECRETARIA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SEM_PERMISSAO"));
    }

    @Test
    void coordenadorPodeConsultarSemestres() throws Exception {
        when(metricasService.listarSemestres())
                .thenReturn(new SemestresLetivosResponse(List.of("2026.2", "2026.1")));

        mockMvc.perform(get("/api/coordenador/metricas/semestres")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semestres[0]").value("2026.2"))
                .andExpect(jsonPath("$.semestres[1]").value("2026.1"));
    }

    @Test
    void deveRetornarContratoDeMetricasParaTodosOsPeriodos() throws Exception {
        when(metricasService.obterMetricas(null)).thenReturn(metricasTodosPeriodos());

        mockMvc.perform(get("/api/coordenador/metricas")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semestre").hasJsonPath())
                .andExpect(jsonPath("$.semestre").value(nullValue()))
                .andExpect(jsonPath("$.totalAtendimentosConcluidos").value(5))
                .andExpect(jsonPath("$.porSemestre[0].semestre").value("2026.2"))
                .andExpect(jsonPath("$.porDisciplina[0].disciplina").value("Dentística"))
                .andExpect(jsonPath("$.porEspecialidade[2].especialidade").value("Não informada"))
                .andExpect(jsonPath("$.pacientes").doesNotExist());

        verify(metricasService).obterMetricas(null);
    }

    @Test
    void deveRepassarSemestreInformadoAoService() throws Exception {
        when(metricasService.obterMetricas("2026.1")).thenReturn(
                new MetricasCoordenadorResponse("2026.1", 0,
                        List.of(new MetricasCoordenadorResponse.PorSemestre("2026.1", 0)),
                        List.of(), List.of()));

        mockMvc.perform(get("/api/coordenador/metricas")
                        .param("semestre", "2026.1")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semestre").value("2026.1"))
                .andExpect(jsonPath("$.totalAtendimentosConcluidos").value(0))
                .andExpect(jsonPath("$.porSemestre[0].total").value(0))
                .andExpect(jsonPath("$.porDisciplina").isEmpty())
                .andExpect(jsonPath("$.porEspecialidade").isEmpty());

        verify(metricasService).obterMetricas("2026.1");
    }

    @Test
    void semestreMalformadoRetorna400NoContratoDefinido() throws Exception {
        when(metricasService.obterMetricas("2026.3"))
                .thenThrow(new br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException());

        mockMvc.perform(get("/api/coordenador/metricas")
                        .param("semestre", "2026.3")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SEMESTRE_INVALIDO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Informe um semestre no formato AAAA.1 ou AAAA.2."));
    }

    @Test
    void semestreValidoNaoCadastradoRetorna404NoContratoDefinido() throws Exception {
        when(metricasService.obterMetricas("2025.2"))
                .thenThrow(new br.com.unicatolica.odontologia_api.exception.SemestreNaoEncontradoException("2025.2"));

        mockMvc.perform(get("/api/coordenador/metricas")
                        .param("semestre", "2025.2")
                        .session(sessaoAutenticada("COORDENADOR")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("SEMESTRE_NAO_ENCONTRADO"))
                .andExpect(jsonPath("$.mensagem")
                        .value("O semestre letivo informado não está cadastrado."));
    }

    @Test
    void logoutExistenteContinuaAcessivelSemAutenticacao() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
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

    private MetricasCoordenadorResponse metricasTodosPeriodos() {
        return new MetricasCoordenadorResponse(
                null,
                5,
                List.of(
                        new MetricasCoordenadorResponse.PorSemestre("2026.2", 3),
                        new MetricasCoordenadorResponse.PorSemestre("2026.1", 2)),
                List.of(
                        new MetricasCoordenadorResponse.PorDisciplina("Dentística", 3),
                        new MetricasCoordenadorResponse.PorDisciplina("Endodontia", 2)),
                List.of(
                        new MetricasCoordenadorResponse.PorEspecialidade("Dentística", 2),
                        new MetricasCoordenadorResponse.PorEspecialidade("Endodontia", 2),
                        new MetricasCoordenadorResponse.PorEspecialidade("Não informada", 1)));
    }
}