package br.com.unicatolica.odontologia_api.solicitacoes.controller;

import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.SolicitacaoPacienteRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.UsuarioIdentidadeRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.service.SolicitacaoPacienteService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MockMvc standalone: controller/service reais, todos os repositories simulados. */
@ExtendWith(MockitoExtension.class)
class RecusaSolicitacaoControllerTest {
    @Mock SolicitacaoPacienteRepository repository;
    @Mock FilaEsperaRepository fila;
    @Mock UsuarioIdentidadeRepository identidades;
    @Mock JdbcTemplate jdbc;
    @Mock Authentication autenticacao;
    private static LocalValidatorFactoryBean validator;
    private MockMvc mvc;
    private static final String URL = "/api/solicitacoes-pacientes/10/recusar";

    @BeforeAll static void iniciarValidator() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }
    @AfterAll static void fecharValidator() { validator.close(); }
    @BeforeEach void preparar() {
        var service = new SolicitacaoPacienteService(repository, fila, jdbc);
        var controller = new SolicitacaoPacienteController(service, identidades);
        mvc = MockMvcBuilders.standaloneSetup(controller).setValidator(validator).build();
    }
    private void autenticar() {
        when(autenticacao.isAuthenticated()).thenReturn(true);
        when(autenticacao.getName()).thenReturn("secretaria@teste.local");
        when(identidades.buscarIdPorEmail("secretaria@teste.local")).thenReturn(Optional.of(7L));
    }
    private SolicitacaoPaciente pedido(String status) {
        var pedido = new SolicitacaoPaciente();
        pedido.setId(10L); pedido.setStatusSolicitacao(status);
        pedido.setObservacaoSecretaria("Texto do aluno");
        return pedido;
    }

    @Test void patchUsaObservacaoSecretariaEDevolveRecusado() throws Exception {
        autenticar();
        when(repository.findById(10L)).thenReturn(Optional.of(pedido("PENDENTE_APROVACAO")));
        when(repository.save(any(SolicitacaoPaciente.class))).thenAnswer(i -> i.getArgument(0));

        var resposta = mvc.perform(patch(URL).principal(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacaoSecretaria\":\"Motivo enviado pela secretaria\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        var captor = ArgumentCaptor.forClass(SolicitacaoPaciente.class);
        verify(repository).save(captor.capture());
        assertEquals("RECUSADO", captor.getValue().getStatusSolicitacao());
        assertEquals("Motivo enviado pela secretaria", captor.getValue().getRespostaSecretaria());
        assertEquals("Texto do aluno", captor.getValue().getObservacaoSecretaria());
        assertEquals(7L, captor.getValue().getSecretariaAnalisadorId());
        assertTrue(resposta.contains("\"statusSolicitacao\":\"RECUSADO\""));
        assertTrue(resposta.contains("\"respostaSecretaria\":\"Motivo enviado pela secretaria\""));
        verifyNoInteractions(fila, jdbc);
    }

    @ParameterizedTest(name = "payload vazio/nulo [{index}]")
    @NullAndEmptySource
    @ValueSource(strings = { " ", "   " })
    void payloadInvalidoERejeitadoAntesDosRepositories(String motivo) throws Exception {
        String valor = motivo == null ? "null" : "\"" + motivo + "\"";
        mvc.perform(patch(URL).principal(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacaoSecretaria\":" + valor + "}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, fila, jdbc, identidades);
    }

    @Test void campoDiferenteNaoSubstituiObservacaoSecretaria() throws Exception {
        mvc.perform(patch(URL).principal(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"respostaSecretaria\":\"Motivo no campo incorreto\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, fila, jdbc, identidades);
    }

    @Test void justificativaAcimaDoLimiteNaoAcessaRepositories() throws Exception {
        mvc.perform(patch(URL).principal(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacaoSecretaria\":\"" + "x".repeat(5001) + "\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, fila, jdbc, identidades);
    }

    @ParameterizedTest(name = "HTTP 409 para {0}")
    @ValueSource(strings = { "APROVADO_E_ALOCADO", "RECUSADO" })
    void recusaDeDecisaoAnteriorRetorna409(String estado) throws Exception {
        autenticar();
        when(repository.findById(10L)).thenReturn(Optional.of(pedido(estado)));
        mvc.perform(patch(URL).principal(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacaoSecretaria\":\"Novo motivo\"}"))
                .andExpect(status().isConflict());
        verify(repository, never()).save(any());
        verifyNoInteractions(fila, jdbc);
    }

    @Test void semAutenticacaoRetorna401SemRepositoryReal() throws Exception {
        mvc.perform(patch(URL).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacaoSecretaria\":\"Motivo\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(repository, fila, jdbc, identidades);
    }

    @Test void solicitacaoInexistenteRetorna404SemGravar() throws Exception {
        autenticar();
        when(repository.findById(10L)).thenReturn(Optional.empty());
        mvc.perform(patch(URL).principal(autenticacao).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacaoSecretaria\":\"Motivo\"}"))
                .andExpect(status().isNotFound());
        verify(repository, never()).save(any());
        verifyNoInteractions(fila, jdbc);
    }
}
