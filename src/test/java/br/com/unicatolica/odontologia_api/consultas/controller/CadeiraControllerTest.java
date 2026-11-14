package br.com.unicatolica.odontologia_api.consultas.controller;

import br.com.unicatolica.odontologia_api.consultas.dto.CadeiraResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.service.GestaoVagasService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CadeiraControllerTest {
    @Mock GestaoVagasService service;
    @Mock SessaoAluno sessao;

    @Test void listaCadeirasAtivasParaSecretaria() throws Exception {
        when(service.listarCadeiras()).thenReturn(List.of(new CadeiraResponseDTO(2L, "02", true)));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CadeiraController(service, sessao)).build();

        mvc.perform(get("/api/cadeiras")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("02"))
                .andExpect(jsonPath("$[0].ativa").value(true));

        verify(sessao).exigirSecretaria(org.mockito.ArgumentMatchers.any());
        verify(service).listarCadeiras();
    }
}
