package br.com.unicatolica.odontologia_api.solicitacoes.service;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.SolicitacaoAdministrativaProjection;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.SolicitacaoPacienteRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
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

    @Mock
    private JdbcTemplate jdbc;

    @InjectMocks
    private SolicitacaoPacienteService service;

    @Test
    void deveMapearConsultaAdministrativaComPacienteOpcional() {
        SolicitacaoAdministrativaProjection projection = mock(
                SolicitacaoAdministrativaProjection.class);

        when(projection.getId()).thenReturn(10L);
        when(projection.getAlunoId()).thenReturn(2L);
        when(projection.getAlunoNome()).thenReturn("Aluno Teste");
        when(projection.getAlunoIdentificacao()).thenReturn("MAT-002");
        when(projection.getTurmaId()).thenReturn(3L);
        when(projection.getTurma()).thenReturn("TURMA-A");
        when(projection.getDisciplina()).thenReturn("Dentística");
        when(projection.getSemestreAtual()).thenReturn(6);
        when(projection.getProcedimentoId()).thenReturn(5L);
        when(projection.getProcedimento()).thenReturn("Restauração");
        when(projection.getDente()).thenReturn("11");
        when(projection.getClasse()).thenReturn("I");
        when(projection.getDataDesejada()).thenReturn(LocalDate.of(2026, 11, 10));
        when(projection.getStatusSolicitacao()).thenReturn("PENDENTE_FILA");
        when(projection.getDataSolicitacao())
                .thenReturn(Instant.parse("2026-11-01T10:15:00Z"));
        when(projection.getPacienteAlocadoId()).thenReturn(null);
        when(projection.getPacienteAlocadoNome()).thenReturn(null);

        when(repository.listarAdministrativas()).thenReturn(List.of(projection));

        var resultado = service.listarSolicitacoesAdministrativas();

        assertEquals(1, resultado.size());
        assertEquals("PENDENTE_FILA", resultado.get(0).statusSolicitacao());
        assertEquals("MAT-002", resultado.get(0).alunoIdentificacao());
        assertEquals(6, resultado.get(0).semestreAtual());
        assertEquals(
                Instant.parse("2026-11-01T10:15:00Z").atOffset(ZoneOffset.UTC),
                resultado.get(0).dataSolicitacao());
        assertEquals(LocalDate.of(2026, 11, 10), resultado.get(0).dataDesejada());
        assertNull(resultado.get(0).pacienteAlocadoId());
        assertNull(resultado.get(0).pacienteAlocadoNome());
        verify(repository).listarAdministrativas();
    }

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
                resultado.getRespostaSecretaria());
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

    @Test
    void recusaPreservaObservacaoDoAlunoEAplicaTrim() {
        var pedido = new SolicitacaoPaciente();
        pedido.setStatusSolicitacao("PENDENTE_APROVACAO");
        pedido.setObservacaoSecretaria("Observacao original do aluno");
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));
        when(repository.save(any(SolicitacaoPaciente.class))).thenAnswer(i -> i.getArgument(0));
        var resultado = service.recusarSolicitacao(1L, 7L, "  Justificativa da secretaria  ");
        assertEquals("Justificativa da secretaria", resultado.getRespostaSecretaria());
        assertEquals("Observacao original do aluno", resultado.getObservacaoSecretaria());
        verifyNoInteractions(filaEsperaRepository);
    }

    @Test
    void recusaRejeitaMotivoAusenteSemEscrever() {
        for (String motivo : new String[] { null, "", "   ", "\t\n" }) {
            var pedido = new SolicitacaoPaciente();
            pedido.setStatusSolicitacao("PENDENTE_APROVACAO");
            when(repository.findById(1L)).thenReturn(Optional.of(pedido));
            var erro = assertThrows(ResponseStatusException.class,
                    () -> service.recusarSolicitacao(1L, 7L, motivo));
            assertEquals(400, erro.getStatusCode().value());
            assertEquals("PENDENTE_APROVACAO", pedido.getStatusSolicitacao());
        }
        verify(repository, never()).save(any());
        verifyNoInteractions(filaEsperaRepository);
    }

    @Test
    void recusaRejeitaSolicitacaoJaDecidida() {
        for (String status : List.of("APROVADO_E_ALOCADO", "RECUSADO")) {
            var pedido = new SolicitacaoPaciente();
            pedido.setStatusSolicitacao(status);
            when(repository.findById(1L)).thenReturn(Optional.of(pedido));
            var erro = assertThrows(ResponseStatusException.class,
                    () -> service.recusarSolicitacao(1L, 7L, "Motivo"));
            assertEquals(409, erro.getStatusCode().value());
        }
        verify(repository, never()).save(any());
        verifyNoInteractions(filaEsperaRepository);
    }

    @Test
    void consultaAdministrativaPreservaRespostaEAutoria() {
        var projection = mock(SolicitacaoAdministrativaProjection.class);
        when(projection.getStatusSolicitacao()).thenReturn("RECUSADO");
        when(projection.getObservacaoSecretaria()).thenReturn("Observacao original");
        when(projection.getRespostaSecretaria()).thenReturn("Motivo da recusa");
        when(projection.getSecretariaAnalisadorId()).thenReturn(7L);
        var instante = Instant.parse("2026-11-02T12:00:00Z");
        when(projection.getDataAnalise()).thenReturn(instante);
        when(repository.listarAdministrativas()).thenReturn(List.of(projection));
        var dto = service.listarSolicitacoesAdministrativas().get(0);
        assertEquals("Observacao original", dto.observacaoSecretaria());
        assertEquals("Motivo da recusa", dto.respostaSecretaria());
        assertEquals(7L, dto.secretariaAnalisadorId());
        assertEquals(instante.atOffset(ZoneOffset.UTC), dto.dataAnalise());
    }

    @Test
    void aprovacaoRejeitaDenteClasseOuDisponibilidadeIncompativel() {
        for (String caso : List.of("DENTE", "CLASSE", "ALOCADO", "INATIVO")) {
            var pedido = new SolicitacaoPaciente();
            pedido.setStatusSolicitacao("PENDENTE_APROVACAO");
            pedido.setProcedimentoId(5L); pedido.setDente("11"); pedido.setClasse("I");
            var entrada = new FilaEspera();
            entrada.setProcedimentoId(5L); entrada.setDente("11"); entrada.setClasse("I");
            entrada.setStatusFila("AGUARDANDO");
            switch (caso) {
                case "DENTE" -> entrada.setDente("12");
                case "CLASSE" -> entrada.setClasse("II");
                default -> entrada.setStatusFila(caso);
            }
            when(repository.findById(1L)).thenReturn(Optional.of(pedido));
            when(filaEsperaRepository.findById(25L)).thenReturn(Optional.of(entrada));
            var erro = assertThrows(ResponseStatusException.class, () -> service.aprovarSolicitacao(1L, 25L, 7L));
            assertEquals(409, erro.getStatusCode().value());
            assertEquals("PENDENTE_APROVACAO", pedido.getStatusSolicitacao());
        }
        verify(repository, never()).save(any());
        verify(filaEsperaRepository, never()).save(any());
    }

    @Test
    void aprovacaoNaoReabreSolicitacaoDecidida() {
        for (String status : List.of("APROVADO_E_ALOCADO", "RECUSADO")) {
            var pedido = new SolicitacaoPaciente(); pedido.setStatusSolicitacao(status);
            when(repository.findById(1L)).thenReturn(Optional.of(pedido));
            assertEquals(409, assertThrows(ResponseStatusException.class,
                    () -> service.aprovarSolicitacao(1L, 25L, 7L)).getStatusCode().value());
        }
        verify(repository, never()).save(any());
        verifyNoInteractions(filaEsperaRepository);
    }

    @Test
    void aprovacaoRejeitaEntradaInexistenteSemSalvar() {
        var pedido = new SolicitacaoPaciente(); pedido.setStatusSolicitacao("PENDENTE_APROVACAO");
        when(repository.findById(1L)).thenReturn(Optional.of(pedido));
        when(filaEsperaRepository.findById(25L)).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.aprovarSolicitacao(1L, 25L, 7L)).getStatusCode().value());
        verify(repository, never()).save(any());
        verify(filaEsperaRepository, never()).save(any());
    }
}
