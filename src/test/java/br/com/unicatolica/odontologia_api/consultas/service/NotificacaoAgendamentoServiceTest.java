package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.NotificacaoWhatsapp;
import br.com.unicatolica.odontologia_api.consultas.repository.AgendaConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.NotificacaoWhatsappRepository;
import br.com.unicatolica.odontologia_api.model.Paciente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacaoAgendamentoServiceTest {

    @Mock private AgendaConsultaRepository agenda;
    @Mock private NotificacaoWhatsappRepository notificacoes;
    @InjectMocks private NotificacaoAgendamentoService service;

    @Test
    void cancelamentoSimuladoIncluiDataTurnoFilaEPermanecePendenteDeIntegracao() {
        Consulta consulta = new Consulta();
        consulta.setId(31L);
        Paciente paciente = new Paciente();
        paciente.setId(22L);
        paciente.setNomeCompleto("Maria da Silva");
        LocalDate data = LocalDate.of(2026, 12, 4);
        when(agenda.buscarTemplate("AVISO_CANCELAMENTO"))
                .thenReturn("Olá, {{nome_paciente}}! Sua consulta de {{data}} foi cancelada. Você permanece na fila de espera.");
        when(notificacoes.saveAndFlush(any(NotificacaoWhatsapp.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        NotificacaoWhatsapp criada = service.registrar(
                consulta, paciente, data, "TARDE", "AVISO_CANCELAMENTO", 7L);

        assertEquals("PENDENTE", criada.getStatusEnvio());
        assertEquals(22L, criada.getPacienteId());
        assertEquals(31L, criada.getConsultaId());
        assertTrue(criada.getConteudoMensagem().contains("04/12/2026"));
        assertTrue(criada.getConteudoMensagem().contains("Turno: tarde."));
        assertTrue(criada.getConteudoMensagem().contains("fila de espera"));
        verify(notificacoes).saveAndFlush(criada);
    }
}
