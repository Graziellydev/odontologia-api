package br.com.unicatolica.odontologia_api.filaespera.controller;

import br.com.unicatolica.odontologia_api.filaespera.dto.EntradaFilaCriadaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaItemDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.NovaEntradaFilaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.TempoMedioEsperaDTO;
import br.com.unicatolica.odontologia_api.filaespera.service.FilaEsperaService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilaEsperaControllerTest {

        @Mock
        private FilaEsperaService filaEsperaService;

        @InjectMocks
        private FilaEsperaController filaEsperaController;

        @Test
        void deveListarFila() {

                when(filaEsperaService.listarFila(
                                null, null, null, null, null, null, null)).thenReturn(List.of());

                List<FilaEsperaItemDTO> resultado = filaEsperaController.listarFila(
                                null, null, null, null, null, null, null);

                assertNotNull(resultado);
                assertTrue(resultado.isEmpty());

                verify(filaEsperaService).listarFila(
                                null, null, null, null, null, null, null);
        }

        @Test
        void deveConsultarMetricas() {

                when(filaEsperaService.calcularTempoMedioPorEspecialidade())
                                .thenReturn(List.of());

                List<TempoMedioEsperaDTO> resultado = filaEsperaController.consultarTempoMedio();

                assertNotNull(resultado);
                assertTrue(resultado.isEmpty());

                verify(filaEsperaService)
                                .calcularTempoMedioPorEspecialidade();
        }

        @Test
        void deveAdicionarEntradaNaFila() {

                NovaEntradaFilaDTO dto = new NovaEntradaFilaDTO(
                                1L,
                                null,
                                2L,
                                "11",
                                "I",
                                "ALTA",
                                true,
                                "Paciente prioritário");

                EntradaFilaCriadaDTO entradaCriada = new EntradaFilaCriadaDTO(
                                15L,
                                1L,
                                2L,
                                "ALTA",
                                "AGUARDANDO");

                when(filaEsperaService.adicionarEntrada(dto))
                                .thenReturn(entradaCriada);

                ResponseEntity<EntradaFilaCriadaDTO> resposta = filaEsperaController.adicionarEntrada(dto);

                assertEquals(201, resposta.getStatusCode().value());
                assertEquals(entradaCriada, resposta.getBody());

                verify(filaEsperaService).adicionarEntrada(dto);
        }
}