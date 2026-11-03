package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.RemarcacaoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.NotificacaoWhatsapp;
import br.com.unicatolica.odontologia_api.consultas.entity.StatusConsulta;
import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import br.com.unicatolica.odontologia_api.consultas.repository.AgendaConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.ConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaRepository;
import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.model.Paciente;
import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.PacienteRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @Mock private AgendaConsultaRepository agenda;
    @Mock private ConsultaRepository consultas;
    @Mock private VagaRepository vagas;
    @Mock private FilaEsperaRepository filas;
    @Mock private PacienteRepository pacientes;
    @Mock private UsuarioRepository usuarios;
    @Mock private NotificacaoAgendamentoService notificacaoService;
    @Mock private EntityManager entityManager;
    @Mock private Query query;
    @InjectMocks private AgendamentoService service;

    @Test
    void criaConsultaEConsomeEntradaDaFilaComNotificacaoSimulada() {
        configurarHistorico();
        FilaEspera fila = fila("AGUARDANDO");
        Paciente paciente = paciente();
        Usuario aluno = aluno();
        Vaga vaga = vaga(50L, LocalDate.now().plusDays(5), "MANHA");
        NotificacaoWhatsapp notificacao = new NotificacaoWhatsapp();
        notificacao.setId(70L);
        AgendamentoResponseDTO esperado = resposta(60L, "AGENDADO", 70L);

        when(filas.buscarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(fila));
        when(pacientes.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(paciente));
        when(usuarios.buscarPorIdParaAtualizacao(40L)).thenReturn(Optional.of(aluno));
        when(agenda.alunoPodeAtender(10L, 40L, 45L)).thenReturn(true);
        when(agenda.existeConsultaAtivaNaFila(10L, null)).thenReturn(false);
        when(agenda.existeConflitoDePaciente(20L, vaga.getData(), "MANHA", null)).thenReturn(false);
        when(agenda.existeConflitoDeAluno(40L, vaga.getData(), "MANHA", null)).thenReturn(false);
        when(vagas.reservarPrimeiraDisponivel(vaga.getData(), "MANHA")).thenReturn(Optional.of(vaga));
        when(consultas.saveAndFlush(any(Consulta.class))).thenAnswer(invocacao -> {
            Consulta salva = invocacao.getArgument(0);
            salva.setId(60L);
            return salva;
        });
        when(notificacaoService.registrar(any(), any(), any(), anyString(), anyString(), any()))
                .thenReturn(notificacao);
        when(agenda.buscarAgendamento(60L, 70L)).thenReturn(esperado);

        AgendamentoResponseDTO resultado = service.agendar(new AgendamentoRequestDTO(
                10L, 40L, 45L, vaga.getData(), AgendamentoRequestDTO.Turno.MANHA), 5L);

        assertEquals(60L, resultado.id());
        assertEquals(70L, resultado.notificacaoId());
        assertEquals("ALOCADO", fila.getStatusFila());
        verify(filas).saveAndFlush(fila);
        verify(consultas).saveAndFlush(any(Consulta.class));
    }

    @Test
    void impedeAgendamentoQuandoNaoHaVaga() {
        FilaEspera fila = fila("AGUARDANDO");
        LocalDate data = LocalDate.now().plusDays(4);
        when(filas.buscarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(fila));
        when(pacientes.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(paciente()));
        when(usuarios.buscarPorIdParaAtualizacao(40L)).thenReturn(Optional.of(aluno()));
        when(agenda.alunoPodeAtender(10L, 40L, 45L)).thenReturn(true);
        when(agenda.existeConsultaAtivaNaFila(10L, null)).thenReturn(false);
        when(agenda.existeConflitoDePaciente(20L, data, "TARDE", null)).thenReturn(false);
        when(agenda.existeConflitoDeAluno(40L, data, "TARDE", null)).thenReturn(false);
        when(vagas.reservarPrimeiraDisponivel(data, "TARDE")).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.agendar(new AgendamentoRequestDTO(
                        10L, 40L, 45L, data, AgendamentoRequestDTO.Turno.TARDE), 5L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(consultas, never()).saveAndFlush(any());
        verify(filas, never()).save(any());
    }

    @Test
    void indisponibilidadeNaRemarcacaoPreservaConsultaOriginal() {
        Consulta origem = consulta(StatusConsulta.AGENDADO);
        FilaEspera fila = fila("ALOCADO");
        LocalDate novaData = LocalDate.now().plusDays(8);
        when(consultas.buscarPorIdParaAtualizacao(60L)).thenReturn(Optional.of(origem));
        when(filas.buscarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(fila));
        when(pacientes.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(paciente()));
        when(usuarios.buscarPorIdParaAtualizacao(40L)).thenReturn(Optional.of(aluno()));
        when(vagas.findById(50L)).thenReturn(Optional.of(vaga(50L, LocalDate.now().plusDays(2), "MANHA")));
        when(agenda.existeConflitoDePaciente(20L, novaData, "TARDE", 60L)).thenReturn(false);
        when(agenda.existeConflitoDeAluno(40L, novaData, "TARDE", 60L)).thenReturn(false);
        when(vagas.reservarPrimeiraDisponivel(novaData, "TARDE")).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.remarcar(60L, new RemarcacaoRequestDTO(novaData, AgendamentoRequestDTO.Turno.TARDE), 5L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        assertEquals(StatusConsulta.AGENDADO, origem.getStatusConsulta());
        verify(consultas, never()).saveAndFlush(any());
        verify(notificacaoService, never()).registrar(any(), any(), any(), anyString(), anyString(), any());
    }

    @Test
    void cancelarLiberaConsultaEDevolveDemandaParaFila() {
        configurarHistorico();
        Consulta consulta = consulta(StatusConsulta.AGENDADO);
        FilaEspera fila = fila("ALOCADO");
        Paciente paciente = paciente();
        Vaga vaga = vaga(50L, LocalDate.now().plusDays(3), "MANHA");
        NotificacaoWhatsapp notificacao = new NotificacaoWhatsapp();
        notificacao.setId(70L);
        when(consultas.buscarPorIdParaAtualizacao(60L)).thenReturn(Optional.of(consulta));
        when(filas.buscarPorIdParaAtualizacao(10L)).thenReturn(Optional.of(fila));
        when(pacientes.buscarPorIdParaAtualizacao(20L)).thenReturn(Optional.of(paciente));
        when(vagas.findById(50L)).thenReturn(Optional.of(vaga));
        when(agenda.existeOutraEntradaAguardando(10L)).thenReturn(false);
        when(notificacaoService.registrar(consulta, paciente, vaga.getData(), "MANHA", "AVISO_CANCELAMENTO", 5L))
                .thenReturn(notificacao);
        when(agenda.buscarAgendamento(60L, 70L)).thenReturn(resposta(60L, "CANCELADO_SECRETARIA", 70L));

        AgendamentoResponseDTO resultado = service.cancelar(60L, 5L);

        assertEquals(StatusConsulta.CANCELADO_SECRETARIA, consulta.getStatusConsulta());
        assertEquals("AGUARDANDO", fila.getStatusFila());
        assertNotNull(resultado.notificacaoId());
        verify(consultas).saveAndFlush(consulta);
        verify(filas).saveAndFlush(fila);
    }

    private static FilaEspera fila(String status) {
        FilaEspera fila = new FilaEspera();
        fila.setId(10L);
        fila.setPacienteId(20L);
        fila.setProcedimentoId(30L);
        fila.setStatusFila(status);
        fila.setNivelUrgencia("MEDIA");
        return fila;
    }

    private static Paciente paciente() {
        Paciente paciente = new Paciente();
        paciente.setId(20L);
        paciente.setNomeCompleto("Paciente Teste");
        paciente.setSituacao("ATIVO");
        return paciente;
    }

    private static Usuario aluno() {
        Usuario aluno = new Usuario();
        aluno.setId(40L);
        aluno.setPerfil(Perfil.ALUNO);
        aluno.setAtivo(true);
        return aluno;
    }

    private static Vaga vaga(Long id, LocalDate data, String turno) {
        Vaga vaga = new Vaga();
        vaga.setId(id);
        vaga.setData(data);
        vaga.setTurno(turno);
        vaga.setBloqueada(false);
        return vaga;
    }

    private static Consulta consulta(StatusConsulta status) {
        Consulta consulta = new Consulta();
        consulta.setId(60L);
        consulta.setPacienteId(20L);
        consulta.setAlunoId(40L);
        consulta.setTurmaId(45L);
        consulta.setVagaId(50L);
        consulta.setFilaEsperaId(10L);
        consulta.setStatusConsulta(status);
        return consulta;
    }

    private static AgendamentoResponseDTO resposta(Long id, String status, Long notificacaoId) {
        return new AgendamentoResponseDTO(id, 20L, "Paciente Teste", 40L, "Aluno Teste", 45L,
                10L, 30L, "Procedimento", "Especialidade", null, null, 50L, "Cadeira 1",
                LocalDate.now().plusDays(3), "MANHA", status, null, null, notificacaoId, true);
    }

    private void configurarHistorico() {
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
    }
}
