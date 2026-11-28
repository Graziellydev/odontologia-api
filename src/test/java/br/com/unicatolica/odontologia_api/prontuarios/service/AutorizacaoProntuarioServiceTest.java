package br.com.unicatolica.odontologia_api.prontuarios.service;

import br.com.unicatolica.odontologia_api.prontuarios.dto.ConsultaClinica;
import br.com.unicatolica.odontologia_api.prontuarios.repository.ProntuarioQueryRepository;
import br.com.unicatolica.odontologia_api.prontuarios.security.ProntuarioProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutorizacaoProntuarioServiceTest {
    @Mock private ProntuarioQueryRepository queries;
    private ProntuarioProperties properties;
    private AutorizacaoProntuarioService service;

    @BeforeEach
    void configurar() {
        properties = new ProntuarioProperties();
        properties.setValidarTurnoEstrito(false);
        service = new AutorizacaoProntuarioService(queries, properties,
                Clock.fixed(Instant.parse("2026-11-20T14:00:00Z"), ZoneId.of("UTC")));
    }

    @Test
    void bloqueiaAlunoSemConsultaDiretaCom403() {
        when(queries.buscarConsultaDoAluno(3L, 7L, false)).thenReturn(null);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.exigirConsultaDoTurno(3L, 7L, false));
        assertEquals(403, exception.getStatusCode().value());
        assertEquals(AutorizacaoProntuarioService.VINCULO_BLOCK_MESSAGE, exception.getReason());
    }

    @Test
    void leituraPosConclusaoAceitaRealizadoMasCriacaoInicialNao() {
        ConsultaClinica realizado = consulta("REALIZADO", "TARDE");
        when(queries.buscarConsultaDoAluno(3L, 7L, true)).thenReturn(realizado);
        when(queries.buscarConsultaDoAluno(3L, 7L, false)).thenReturn(realizado);

        assertNotNull(service.exigirConsultaDoTurno(3L, 7L, true));
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.exigirConsultaDoTurno(3L, 7L, false));
        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void turnoEstritoAplicaLimitesInicioInclusivoEFimExclusivo() {
        properties.setValidarTurnoEstrito(true);
        service = new AutorizacaoProntuarioService(queries, properties,
                Clock.fixed(Instant.parse("2026-11-20T16:30:00Z"), ZoneId.of("UTC")));
        when(queries.buscarConsultaDoAluno(3L, 7L, false)).thenReturn(consulta("AGENDADO", "TARDE"));
        assertNotNull(service.exigirConsultaDoTurno(3L, 7L, false));
    }

    @Test
    void turnoDaManhaPermite0730EBloqueia1200() {
        properties.setValidarTurnoEstrito(true);
        when(queries.buscarConsultaDoAluno(3L, 7L, false)).thenReturn(consulta("AGENDADO", "MANHA"));
        service = servicoNoHorario("2026-11-20T10:30:00Z"); // 07:30 em São Paulo
        assertNotNull(service.exigirConsultaDoTurno(3L, 7L, false));
        service = servicoNoHorario("2026-11-20T15:00:00Z"); // 12:00 em São Paulo
        assertThrows(ResponseStatusException.class, () -> service.exigirConsultaDoTurno(3L, 7L, false));
    }

    @Test
    void turnoDaTardePermite1330EBloqueia1800() {
        properties.setValidarTurnoEstrito(true);
        when(queries.buscarConsultaDoAluno(3L, 7L, false)).thenReturn(consulta("AGENDADO", "TARDE"));
        service = servicoNoHorario("2026-11-20T16:30:00Z"); // 13:30 em São Paulo
        assertNotNull(service.exigirConsultaDoTurno(3L, 7L, false));
        service = servicoNoHorario("2026-11-20T21:00:00Z"); // 18:00 em São Paulo
        assertThrows(ResponseStatusException.class, () -> service.exigirConsultaDoTurno(3L, 7L, false));
    }

    @Test
    void janelaInvalidaEhRejeitada() {
        properties.setValidarTurnoEstrito(true);
        properties.getTurnoManha().setInicio(LocalTime.NOON);
        properties.getTurnoManha().setFim(LocalTime.of(7, 30));
        when(queries.buscarConsultaDoAluno(3L, 7L, false)).thenReturn(consulta("AGENDADO", "MANHA"));
        assertThrows(IllegalStateException.class, () -> service.exigirConsultaDoTurno(3L, 7L, false));
    }

    private AutorizacaoProntuarioService servicoNoHorario(String instant) {
        return new AutorizacaoProntuarioService(queries, properties,
                Clock.fixed(Instant.parse(instant), ZoneId.of("UTC")));
    }

    private static ConsultaClinica consulta(String status, String turno) {
        return new ConsultaClinica(1L, 3L, 7L, 11L, 19L, "36", "I", status,
                java.time.LocalDate.of(2026, 11, 20), turno,
                "Paciente teste", "Aluno teste", "Restauração", "01");
    }
}
