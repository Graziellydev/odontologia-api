package br.com.unicatolica.odontologia_api.consultas.controller;

import br.com.unicatolica.odontologia_api.consultas.dto.VagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.TurnoGradeDTO;
import br.com.unicatolica.odontologia_api.consultas.service.GestaoVagasService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VagaControllerTest {

    @Mock GestaoVagasService service;
    @Mock SessaoAluno sessao;
    private static LocalValidatorFactoryBean validator;
    private MockMvc mvc;

    @BeforeAll static void iniciarValidator() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    @AfterAll static void fecharValidator() { validator.close(); }

    @BeforeEach void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new VagaController(service, sessao))
                .setValidator(validator).build();
    }

    @Test void criaVagaComStatus201ELocation() throws Exception {
        when(service.criar(any())).thenReturn(new VagaResponseDTO(
                12L, 3L, "03", LocalDate.parse("2026-12-01"), "MANHA", false, null));

        mvc.perform(post("/api/vagas").contentType("application/json")
                        .content("""
                                {"cadeiraId":3,"data":"2026-12-01","turno":"MANHA"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(12))
                .andExpect(jsonPath("$.cadeiraNumero").value("03"));
        verify(sessao).exigirSecretaria(any());
    }

    @Test void payloadInvalidoNaoInvocaService() throws Exception {
        mvc.perform(post("/api/vagas").contentType("application/json")
                        .content("{\"cadeiraId\":0,\"turno\":\"NOITE\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, sessao);
    }

    @Test void encaminhaFiltrosDeListagem() throws Exception {
        when(service.listar(any(), any(), any(), any(), any())).thenReturn(List.of());

        mvc.perform(get("/api/vagas").param("inicio", "2026-12-01")
                        .param("fim", "2026-12-31").param("cadeiraId", "3")
                        .param("turno", "TARDE").param("bloqueada", "true"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray());
        verify(service).listar(eq(LocalDate.parse("2026-12-01")), eq(LocalDate.parse("2026-12-31")),
                eq(3L), eq(VagaRequestDTO.Turno.TARDE), eq(true));
    }

    @Test void usuarioSemPerfilDeSecretariaNaoAcessaDados() throws Exception {
        org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN))
                .when(sessao).exigirSecretaria(any());

        mvc.perform(get("/api/vagas")).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test void geraTurnosRetornaContagens() throws Exception {
        when(service.gerarTurnos(any())).thenReturn(new GerarTurnosResponseDTO(28, 4));

        mvc.perform(post("/api/vagas/geracoes").contentType("application/json")
                        .content("{\"inicio\":\"2026-11-09\",\"fim\":\"2026-11-22\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criadas").value(28))
                .andExpect(jsonPath("$.jaExistentes").value(4));
        verify(sessao).exigirSecretaria(any());
    }

    @Test void requisicaoDeGeracaoSemDatasEhRejeitada() throws Exception {
        mvc.perform(post("/api/vagas/geracoes").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service, sessao);
    }

    @Test void gradeSemanalRetornaEstadoDeCadeira() throws Exception {
        when(service.listarGradeSemanal(any(), any(), any(), any())).thenReturn(List.of(new TurnoGradeDTO(
                LocalDate.parse("2026-11-09"), "MANHA", 3L, "03", null,
                false, null, "LIVRE", null, null, null, null, null, null)));

        mvc.perform(get("/api/vagas/grade").param("inicio", "2026-11-09").param("fim", "2026-11-15")
                        .param("disciplina", "Endodontia").param("disponibilidade", "LIVRE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].situacao").value("LIVRE"))
                .andExpect(jsonPath("$[0].cadeiraNumero").value("03"));
        verify(service).listarGradeSemanal(any(), any(), org.mockito.ArgumentMatchers.eq("Endodontia"),
                org.mockito.ArgumentMatchers.eq("LIVRE"));
    }
}
