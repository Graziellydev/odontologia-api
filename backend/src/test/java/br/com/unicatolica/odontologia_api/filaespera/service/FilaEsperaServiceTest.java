package br.com.unicatolica.odontologia_api.filaespera.service;

import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaAtualizadaDTO;
import java.util.Optional;
import br.com.unicatolica.odontologia_api.filaespera.dto.EntradaFilaCriadaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaItemDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.NovaEntradaFilaDTO;
import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilaEsperaServiceTest {

        @Mock
        private FilaEsperaRepository filaEsperaRepository;

        @InjectMocks
        private FilaEsperaService filaEsperaService;

        @Test
        void deveListarFilaComFiltros() {

                when(filaEsperaRepository.pesquisarFila(
                                null, "Dentística", null, "I", "ALTA", 5, 30)).thenReturn(List.of());

                List<FilaEsperaItemDTO> resultado = filaEsperaService.listarFila(
                                null,
                                " Dentística ",
                                " ",
                                "I",
                                "ALTA",
                                5,
                                30);

                assertNotNull(resultado);
                assertTrue(resultado.isEmpty());

                verify(filaEsperaRepository).pesquisarFila(
                                null, "Dentística", null, "I", "ALTA", 5, 30);
        }

        @Test
        void naoDeveAceitarIntervaloDeEsperaInvalido() {

                assertThrows(
                                ResponseStatusException.class,
                                () -> filaEsperaService.listarFila(
                                                null, null, null, null, null, 31, 10));

                verifyNoInteractions(filaEsperaRepository);
        }

        @Test
        void deveCadastrarEntradaNaFila() {

                NovaEntradaFilaDTO dto = new NovaEntradaFilaDTO(
                                1L,
                                null,
                                2L,
                                "11",
                                "I",
                                "ALTA",
                                true,
                                "Paciente com prioridade");

                when(filaEsperaRepository.save(any(FilaEspera.class)))
                                .thenAnswer(invocation -> {
                                        FilaEspera entrada = invocation.getArgument(0);
                                        entrada.setId(15L);
                                        return entrada;
                                });

                EntradaFilaCriadaDTO resultado = filaEsperaService.adicionarEntrada(dto);

                assertEquals(15L, resultado.id());
                assertEquals(1L, resultado.pacienteId());
                assertEquals(2L, resultado.procedimentoId());
                assertEquals("ALTA", resultado.nivelUrgencia());
                assertEquals("AGUARDANDO", resultado.statusFila());

                ArgumentCaptor<FilaEspera> captor = ArgumentCaptor.forClass(FilaEspera.class);

                verify(filaEsperaRepository).save(captor.capture());

                FilaEspera entradaSalva = captor.getValue();

                assertEquals("TRIAGEM", entradaSalva.getOrigem());
                assertEquals("AGUARDANDO", entradaSalva.getStatusFila());
                assertTrue(entradaSalva.getPrioridadeReagendamento());
        }

        @Test
        void deveConsultarMetricasPorEspecialidade() {

                when(filaEsperaRepository.calcularTempoMedioPorEspecialidade())
                                .thenReturn(List.of());

                List<?> resultado = filaEsperaService.calcularTempoMedioPorEspecialidade();

                assertNotNull(resultado);
                assertTrue(resultado.isEmpty());

                verify(filaEsperaRepository)
                                .calcularTempoMedioPorEspecialidade();
        }

        @Test
        void deveAlocarPacienteQueEstaAguardando() {

                FilaEspera entrada = new FilaEspera();
                entrada.setId(10L);
                entrada.setStatusFila("AGUARDANDO");

                when(filaEsperaRepository.findById(10L))
                                .thenReturn(Optional.of(entrada));

                when(filaEsperaRepository.save(any(FilaEspera.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                FilaEsperaAtualizadaDTO resultado = filaEsperaService.alocarPaciente(10L);

                assertEquals(10L, resultado.id());
                assertEquals("ALOCADO", resultado.statusFila());
                assertNotNull(resultado.dataSaidaFila());

                assertEquals("ALOCADO", entrada.getStatusFila());
                assertNotNull(entrada.getDataSaidaFila());

                verify(filaEsperaRepository).save(entrada);
        }

        @Test
        void naoDeveAlocarEntradaInexistente() {

                when(filaEsperaRepository.findById(99L))
                                .thenReturn(Optional.empty());

                ResponseStatusException erro = assertThrows(
                                ResponseStatusException.class,
                                () -> filaEsperaService.alocarPaciente(99L));

                assertEquals(404, erro.getStatusCode().value());

                verify(filaEsperaRepository, never())
                                .save(any(FilaEspera.class));
        }

        @Test
        void naoDeveAlocarPacienteQueJaSaiuDaFila() {

                FilaEspera entrada = new FilaEspera();
                entrada.setId(10L);
                entrada.setStatusFila("ALOCADO");

                when(filaEsperaRepository.findById(10L))
                                .thenReturn(Optional.of(entrada));

                ResponseStatusException erro = assertThrows(
                                ResponseStatusException.class,
                                () -> filaEsperaService.alocarPaciente(10L));

                assertEquals(409, erro.getStatusCode().value());

                verify(filaEsperaRepository, never())
                                .save(any(FilaEspera.class));
        }
}