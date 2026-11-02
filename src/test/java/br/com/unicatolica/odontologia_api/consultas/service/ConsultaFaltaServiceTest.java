package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.dto.FaltaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.ReagendamentoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.StatusConsulta;
import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import br.com.unicatolica.odontologia_api.consultas.repository.ConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaFaltaServiceTest {

    @Mock
    private ConsultaRepository consultas;

    @Mock
    private VagaRepository vagas;

    @Mock
    private EntityManager entityManager;

    @Mock
    private Query query;

    @InjectMocks
    private ConsultaFaltaService service;

    @Test
    void registraFaltaDePacienteEArmazenaJustificativaNormalizada() {
        Consulta consulta = consulta(StatusConsulta.AGENDADO);
        when(consultas.buscarPorIdParaAtualizacao(consulta.getId())).thenReturn(Optional.of(consulta));
        when(entityManager.createNativeQuery("SELECT set_config('app.origem', 'SECRETARIA', true)"))
                .thenReturn(query);

        service.registrarFalta(consulta.getId(), new FaltaRequestDTO(
                FaltaRequestDTO.QuemFaltou.PACIENTE,
                true,
                "  Problema com transporte  "));

        assertEquals(StatusConsulta.FALTOU_PACIENTE, consulta.getStatusConsulta());
        assertTrue(consulta.getFaltaJustificada());
        assertEquals("Problema com transporte", consulta.getJustificativaFalta());
        verify(consultas).save(consulta);
        verify(query).getSingleResult();
    }

    @Test
    void bloqueiaRegistroDeFaltaEmConsultaJaRealizada() {
        Consulta consulta = consulta(StatusConsulta.REALIZADO);
        when(consultas.buscarPorIdParaAtualizacao(consulta.getId())).thenReturn(Optional.of(consulta));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrarFalta(consulta.getId(), new FaltaRequestDTO(
                        FaltaRequestDTO.QuemFaltou.ALUNO, false, null)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(consultas, never()).save(any());
    }

    @Test
    void exigeTextoQuandoFaltaEstaMarcadaComoJustificada() {
        Consulta consulta = consulta(StatusConsulta.CONFIRMADA_24H);
        when(consultas.buscarPorIdParaAtualizacao(consulta.getId())).thenReturn(Optional.of(consulta));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.registrarFalta(consulta.getId(), new FaltaRequestDTO(
                        FaltaRequestDTO.QuemFaltou.PACIENTE, true, "  ")));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(consultas, never()).save(any());
    }

    @Test
    void criaNovaConsultaVinculadaSemAlterarConsultaOriginal() {
        Consulta origem = consulta(StatusConsulta.FALTOU_ALUNO);
        Vaga vaga = new Vaga();
        vaga.setId(88L);
        vaga.setBloqueada(false);
        when(consultas.buscarPorIdParaAtualizacao(origem.getId())).thenReturn(Optional.of(origem));
        when(vagas.buscarPorIdParaAtualizacao(88L)).thenReturn(Optional.of(vaga));
        when(consultas.existeConsultaAtivaNaVaga(88L)).thenReturn(false);
        when(entityManager.createNativeQuery("SELECT set_config('app.origem', 'SECRETARIA', true)"))
                .thenReturn(query);
        when(consultas.saveAndFlush(any(Consulta.class))).thenAnswer(invocacao -> {
            Consulta nova = invocacao.getArgument(0);
            nova.setId(99L);
            return nova;
        });

        Long novaConsultaId = service.reagendar(origem.getId(), new ReagendamentoRequestDTO(88L, true));

        ArgumentCaptor<Consulta> captor = ArgumentCaptor.forClass(Consulta.class);
        verify(consultas).saveAndFlush(captor.capture());
        Consulta criada = captor.getValue();
        assertEquals(99L, novaConsultaId);
        assertEquals(origem.getId(), criada.getConsultaOrigemId());
        assertEquals(88L, criada.getVagaId());
        assertEquals(StatusConsulta.AGENDADO, criada.getStatusConsulta());
        assertEquals(origem.getPacienteId(), criada.getPacienteId());
        assertEquals(origem.getAlunoId(), criada.getAlunoId());
        assertEquals(origem.getTurmaId(), criada.getTurmaId());
        assertEquals(StatusConsulta.FALTOU_ALUNO, origem.getStatusConsulta());
        assertFalse(Boolean.TRUE.equals(criada.getProntuarioLiberado()));
    }

    @Test
    void bloqueiaReagendamentoDeConsultaSemStatusDeFalta() {
        Consulta origem = consulta(StatusConsulta.REALIZADO);
        when(consultas.buscarPorIdParaAtualizacao(origem.getId())).thenReturn(Optional.of(origem));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.reagendar(origem.getId(), new ReagendamentoRequestDTO(88L, false)));

        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(vagas, never()).buscarPorIdParaAtualizacao(any());
        verify(consultas, never()).saveAndFlush(any());
    }

    private Consulta consulta(StatusConsulta status) {
        Consulta consulta = new Consulta();
        consulta.setId(12L);
        consulta.setPacienteId(20L);
        consulta.setAlunoId(30L);
        consulta.setTurmaId(40L);
        consulta.setProcedimentoId(50L);
        consulta.setDente("36");
        consulta.setClasse("II");
        consulta.setVagaId(60L);
        consulta.setFilaEsperaId(70L);
        consulta.setStatusConsulta(status);
        return consulta;
    }
}
