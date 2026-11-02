package br.com.unicatolica.odontologia_api.retornos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.retornos.dto.RetornoResponse;
import br.com.unicatolica.odontologia_api.retornos.dto.SugestaoRetornoRequest;
import br.com.unicatolica.odontologia_api.retornos.entity.ConsultaRetorno;
import br.com.unicatolica.odontologia_api.retornos.repository.ConsultaRetornoRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@ExtendWith(MockitoExtension.class)
class RetornoServiceTest {

    private static final Long ALUNO_ID = 5L;

    @Mock
    private ConsultaRetornoRepository consultas;

    @Mock
    private FilaEsperaRepository fila;

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private RetornoService service;

    @BeforeEach
    void prepararMocks() {
        // set_config do trigger do histórico
        lenient().when(em.createNativeQuery(anyString())).thenReturn(query);
        lenient().when(query.setParameter(anyString(), any())).thenReturn(query);

        // o banco devolveria o id gerado
        lenient().when(fila.save(any(FilaEspera.class))).thenAnswer(invocacao -> {
            FilaEspera entrada = invocacao.getArgument(0);
            entrada.setId(77L);
            return entrada;
        });
    }

    private ConsultaRetorno consulta(String status) {
        ConsultaRetorno c = new ConsultaRetorno();
        c.setId(10L);
        c.setPacienteId(20L);
        c.setAlunoId(ALUNO_ID);
        c.setProcedimentoId(30L);
        c.setDente("36");
        c.setClasse("II");
        c.setStatusConsulta(status);
        c.setRetornoSugerido(false);
        return c;
    }

    @Test
    void deveSugerirRetornoELevarOPacienteParaAFila() {
        ConsultaRetorno consulta = consulta("AGENDADO");
        consulta.setFilaEsperaId(40L);
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta));

        FilaEspera origem = new FilaEspera();
        origem.setNivelUrgencia("ALTA");
        when(fila.findById(40L)).thenReturn(Optional.of(origem));

        RetornoResponse resposta = service.registrar(
                ALUNO_ID, new SugestaoRetornoRequest(10L, true, "  Falta a obturação  "));

        assertTrue(resposta.retornoSugerido());
        assertEquals(77L, resposta.filaEsperaId());
        assertEquals("REALIZADO", consulta.getStatusConsulta());
        assertTrue(consulta.getRetornoSugerido());
        assertEquals("Falta a obturação", consulta.getJustificativaRetorno());

        ArgumentCaptor<FilaEspera> captor = ArgumentCaptor.forClass(FilaEspera.class);
        verify(fila).save(captor.capture());
        FilaEspera entrada = captor.getValue();
        assertEquals("RETORNO", entrada.getOrigem());
        assertEquals("AGUARDANDO", entrada.getStatusFila());
        assertEquals(20L, entrada.getPacienteId());
        assertEquals(30L, entrada.getProcedimentoId());
        assertEquals("36", entrada.getDente());
        assertEquals("II", entrada.getClasse());
        assertEquals("ALTA", entrada.getNivelUrgencia());

        // o trigger do histórico precisa saber quem alterou o status
        verify(query).setParameter("id", "5");
    }

    @Test
    void deveEncerrarSemRetornoQuandoTratamentoConcluido() {
        ConsultaRetorno consulta = consulta("AGENDADO");
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta));

        RetornoResponse resposta = service.registrar(
                ALUNO_ID, new SugestaoRetornoRequest(10L, false, null));

        assertFalse(resposta.retornoSugerido());
        assertNull(resposta.filaEsperaId());
        assertEquals("REALIZADO", consulta.getStatusConsulta());
        assertFalse(consulta.getRetornoSugerido());
        verify(fila, never()).save(any());
    }

    @Test
    void deveExigirJustificativaParaSugerirRetorno() {
        ConsultaRetorno consulta = consulta("AGENDADO");
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta));

        ResponseStatusException emBranco = assertThrows(ResponseStatusException.class,
                () -> service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, "   ")));
        ResponseStatusException nula = assertThrows(ResponseStatusException.class,
                () -> service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, null)));

        assertEquals(400, emBranco.getStatusCode().value());
        assertEquals(400, nula.getStatusCode().value());
        assertEquals("AGENDADO", consulta.getStatusConsulta());
        verify(fila, never()).save(any());
        verify(consultas, never()).save(any());
    }

    @Test
    void naoDevePermitirEncerrarAtendimentoDeOutroAluno() {
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta("AGENDADO")));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(6L, new SugestaoRetornoRequest(10L, true, "Motivo")));

        assertEquals(403, erro.getStatusCode().value());
        verify(fila, never()).save(any());
    }

    @Test
    void naoDevePermitirEncerrarDuasVezes() {
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta("REALIZADO")));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, "Motivo")));

        assertEquals(409, erro.getStatusCode().value());
        verify(fila, never()).save(any());
    }

    @Test
    void naoDeveDuplicarOPacienteNaFila() {
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta("AGENDADO")));
        when(consultas.contarDemandaAguardando(20L, 30L, "36", "II")).thenReturn(1L);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, "Motivo")));

        assertEquals(409, erro.getStatusCode().value());
        verify(fila, never()).save(any());
    }

    @Test
    void deveRetornarNaoEncontradoQuandoOAtendimentoNaoExiste() {
        when(consultas.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(ALUNO_ID, new SugestaoRetornoRequest(99L, false, null)));

        assertEquals(404, erro.getStatusCode().value());
    }

    @Test
    void deveUsarAUrgenciaDaTriagemQuandoNaoHaFilaDeOrigem() {
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta("AGENDADO")));
        when(consultas.ultimaUrgenciaDaTriagem(20L)).thenReturn(Optional.of("baixa"));

        service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, "Motivo"));

        ArgumentCaptor<FilaEspera> captor = ArgumentCaptor.forClass(FilaEspera.class);
        verify(fila).save(captor.capture());
        assertEquals("BAIXA", captor.getValue().getNivelUrgencia());
    }

    @Test
    void deveUsarMediaQuandoNaoHaUrgenciaConhecida() {
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta("AGENDADO")));

        service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, "Motivo"));

        ArgumentCaptor<FilaEspera> captor = ArgumentCaptor.forClass(FilaEspera.class);
        verify(fila).save(captor.capture());
        assertEquals("MEDIA", captor.getValue().getNivelUrgencia());
    }

    @Test
    void naoDeveSugerirRetornoSemProcedimentoAssociado() {
        ConsultaRetorno consulta = consulta("AGENDADO");
        consulta.setProcedimentoId(null);
        when(consultas.findById(10L)).thenReturn(Optional.of(consulta));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrar(ALUNO_ID, new SugestaoRetornoRequest(10L, true, "Motivo")));

        assertEquals(409, erro.getStatusCode().value());
        verify(fila, never()).save(any());
    }
}