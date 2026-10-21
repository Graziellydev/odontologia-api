package br.com.unicatolica.odontologia_api.solicitacoes.service;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.SolicitacaoPacienteRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitacaoPacienteServiceTest {

    @Mock
    private SolicitacaoPacienteRepository repository;

    @Mock
    private FilaEsperaRepository filaEsperaRepository;

    @InjectMocks
    private SolicitacaoPacienteService service;

    @Test
    void deveAprovarSolicitacaoComPacienteCompativel() {

        SolicitacaoPaciente solicitacao = new SolicitacaoPaciente();
        solicitacao.setId(1L);
        solicitacao.setStatusSolicitacao("PENDENTE_APROVACAO");
        solicitacao.setProcedimentoId(5L);
        solicitacao.setDente("11");
        solicitacao.setClasse("I");

        FilaEspera entrada = new FilaEspera();
        entrada.setId(25L);
        entrada.setPacienteId(99L);
        entrada.setProcedimentoId(5L);
        entrada.setDente("11");
        entrada.setClasse("I");
        entrada.setStatusFila("AGUARDANDO");

        when(repository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(filaEsperaRepository.findById(25L))
                .thenReturn(Optional.of(entrada));

        when(filaEsperaRepository.save(any(FilaEspera.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(repository.save(any(SolicitacaoPaciente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoPaciente resultado = service.aprovarSolicitacao(1L, 25L, 7L);

        assertEquals("APROVADO_E_ALOCADO",
                resultado.getStatusSolicitacao());

        assertEquals(25L, resultado.getFilaEsperaId());
        assertEquals(99L, resultado.getPacienteAlocadoId());
        assertEquals(7L, resultado.getSecretariaAnalisadorId());
        assertNotNull(resultado.getDataAnalise());

        assertEquals("ALOCADO", entrada.getStatusFila());
        assertNotNull(entrada.getDataSaidaFila());

        verify(filaEsperaRepository).save(entrada);
        verify(repository).save(solicitacao);
    }

    @Test
    void deveRecusarSolicitacaoComMotivo() {

        SolicitacaoPaciente solicitacao = new SolicitacaoPaciente();
        solicitacao.setId(1L);
        solicitacao.setStatusSolicitacao("PENDENTE_APROVACAO");

        when(repository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(repository.save(any(SolicitacaoPaciente.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SolicitacaoPaciente resultado = service.recusarSolicitacao(
                1L,
                7L,
                "Não há paciente compatível.");

        assertEquals("RECUSADO", resultado.getStatusSolicitacao());
        assertEquals(
                "Não há paciente compatível.",
                resultado.getObservacaoSecretaria());
        assertEquals(7L, resultado.getSecretariaAnalisadorId());
        assertNotNull(resultado.getDataAnalise());

        verify(repository).save(solicitacao);
        verifyNoInteractions(filaEsperaRepository);
    }

    @Test
    void naoDeveAprovarPacienteComProcedimentoIncompativel() {

        SolicitacaoPaciente solicitacao = new SolicitacaoPaciente();
        solicitacao.setId(1L);
        solicitacao.setStatusSolicitacao("PENDENTE_APROVACAO");
        solicitacao.setProcedimentoId(5L);
        solicitacao.setDente("11");

        FilaEspera entrada = new FilaEspera();
        entrada.setId(25L);
        entrada.setPacienteId(99L);
        entrada.setProcedimentoId(8L);
        entrada.setDente("11");
        entrada.setStatusFila("AGUARDANDO");

        when(repository.findById(1L))
                .thenReturn(Optional.of(solicitacao));

        when(filaEsperaRepository.findById(25L))
                .thenReturn(Optional.of(entrada));

        ResponseStatusException erro = assertThrows(
                ResponseStatusException.class,
                () -> service.aprovarSolicitacao(1L, 25L, 7L));

        assertEquals(409, erro.getStatusCode().value());

        verify(filaEsperaRepository, never())
                .save(any(FilaEspera.class));

        verify(repository, never())
                .save(any(SolicitacaoPaciente.class));
    }
}