package br.com.unicatolica.odontologia_api.solicitacoes.service;

import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.SolicitacaoPacienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Unidade pura: nenhum contexto Spring Boot ou repository real. */
@ExtendWith(MockitoExtension.class)
class RecusaSolicitacaoServiceTest {
    @Mock SolicitacaoPacienteRepository repository;
    @Mock FilaEsperaRepository fila;
    @Mock JdbcTemplate jdbc;
    @InjectMocks SolicitacaoPacienteService service;

    private SolicitacaoPaciente pedido(String status) {
        var pedido = new SolicitacaoPaciente();
        pedido.setId(10L);
        pedido.setStatusSolicitacao(status);
        pedido.setObservacaoSecretaria("Observacao original do aluno");
        return pedido;
    }

    @Test
    void recusaValidaSalvaJustificativaStatusEAutoriaSemAlterarFila() {
        var pedido = pedido("PENDENTE_APROVACAO");
        when(repository.findById(10L)).thenReturn(Optional.of(pedido));
        when(repository.save(any(SolicitacaoPaciente.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = service.recusarSolicitacao(10L, 7L, "  Motivo informado pela secretaria  ");

        var captor = ArgumentCaptor.forClass(SolicitacaoPaciente.class);
        verify(repository, times(1)).save(captor.capture());
        var gravado = captor.getValue();
        assertSame(gravado, resultado);
        assertEquals(10L, gravado.getId());
        assertEquals("RECUSADO", gravado.getStatusSolicitacao());
        assertEquals("Motivo informado pela secretaria", gravado.getRespostaSecretaria());
        assertEquals("Observacao original do aluno", gravado.getObservacaoSecretaria());
        assertEquals(7L, gravado.getSecretariaAnalisadorId());
        assertNotNull(gravado.getDataAnalise());
        assertNull(gravado.getFilaEsperaId());
        assertNull(gravado.getPacienteAlocadoId());
        verifyNoInteractions(fila, jdbc);
    }

    @ParameterizedTest(name = "justificativa invalida [{index}]")
    @NullAndEmptySource
    @ValueSource(strings = { "   ", "\t\n" })
    void justificativaInvalidaNaoPersiste(String motivo) {
        var pedido = pedido("PENDENTE_APROVACAO");
        when(repository.findById(10L)).thenReturn(Optional.of(pedido));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.recusarSolicitacao(10L, 7L, motivo));

        assertEquals(400, erro.getStatusCode().value());
        assertEquals("PENDENTE_APROVACAO", pedido.getStatusSolicitacao());
        assertNull(pedido.getRespostaSecretaria());
        assertNull(pedido.getDataAnalise());
        verify(repository, never()).save(any());
        verifyNoInteractions(fila, jdbc);
    }

    @ParameterizedTest(name = "estado decidido: {0}")
    @ValueSource(strings = { "APROVADO_E_ALOCADO", "RECUSADO" })
    void solicitacaoDecididaNaoPodeSerRecusadaNovamente(String status) {
        var pedido = pedido(status);
        pedido.setRespostaSecretaria("Resposta anterior");
        when(repository.findById(10L)).thenReturn(Optional.of(pedido));

        var erro = assertThrows(ResponseStatusException.class,
                () -> service.recusarSolicitacao(10L, 7L, "Novo motivo"));

        assertEquals(409, erro.getStatusCode().value());
        assertEquals(status, pedido.getStatusSolicitacao());
        assertEquals("Resposta anterior", pedido.getRespostaSecretaria());
        verify(repository, never()).save(any());
        verifyNoInteractions(fila, jdbc);
    }

    @Test
    void falhaDoRepositoryEPropagadaSemResultadoDeSucesso() {
        when(repository.findById(10L)).thenReturn(Optional.of(pedido("PENDENTE_APROVACAO")));
        when(repository.save(any(SolicitacaoPaciente.class)))
                .thenThrow(new DataAccessResourceFailureException("Falha simulada de gravacao"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> service.recusarSolicitacao(10L, 7L, "Motivo valido"));

        verify(repository, times(1)).save(any());
        verifyNoInteractions(fila, jdbc);
        // Mockito nao comprova commit/rollback de uma transacao PostgreSQL.
    }
}
