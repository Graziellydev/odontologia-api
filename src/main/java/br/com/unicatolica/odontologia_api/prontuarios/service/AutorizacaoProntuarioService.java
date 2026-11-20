package br.com.unicatolica.odontologia_api.prontuarios.service;

import br.com.unicatolica.odontologia_api.prontuarios.dto.ConsultaClinica;
import br.com.unicatolica.odontologia_api.prontuarios.repository.ProntuarioQueryRepository;
import br.com.unicatolica.odontologia_api.prontuarios.security.ProntuarioProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
public class AutorizacaoProntuarioService {
    public static final String VINCULO_BLOCK_MESSAGE =
            "Acesso restrito: não há consulta ativa vinculada a este aluno para o paciente no turno atual.";
    private static final ZoneId CLINIC_ZONE = ZoneId.of("America/Sao_Paulo");

    private final ProntuarioQueryRepository consultas;
    private final ProntuarioProperties properties;
    private final Clock clock;

    public AutorizacaoProntuarioService(ProntuarioQueryRepository consultas,
                                        ProntuarioProperties properties,
                                        Clock clock) {
        this.consultas = consultas;
        this.properties = properties;
        this.clock = clock;
    }

    public ConsultaClinica exigirConsultaDoTurno(long pacienteId, long alunoId, boolean permitirRealizado) {
        ConsultaClinica consulta = consultas.buscarConsultaDoAluno(pacienteId, alunoId, permitirRealizado);
        return validarConsulta(consulta, permitirRealizado);
    }

    public ConsultaClinica exigirConsulta(long consultaId, long alunoId, boolean permitirRealizado) {
        ConsultaClinica consulta = consultas.buscarConsulta(consultaId);
        if (consulta == null || !Long.valueOf(alunoId).equals(consulta.alunoId())) return negar();
        return validarConsulta(consulta, permitirRealizado);
    }

    public ConsultaClinica exigirConsultaDoProntuario(long consultaId, long alunoId, long pacienteId,
                                                       boolean permitirRealizado) {
        ConsultaClinica consulta = exigirConsulta(consultaId, alunoId, permitirRealizado);
        if (!Long.valueOf(pacienteId).equals(consulta.pacienteId())) return negar();
        return consulta;
    }

    private ConsultaClinica validarConsulta(ConsultaClinica consulta, boolean permitirRealizado) {
        if (consulta == null || !("AGENDADO".equals(consulta.status())
                || "CONFIRMADA_24H".equals(consulta.status())
                || permitirRealizado && "REALIZADO".equals(consulta.status()))) return negar();
        if (properties.isValidarTurnoEstrito()) {
            LocalTime now = LocalTime.now(clock.withZone(CLINIC_ZONE));
            boolean turnoValido = switch (consulta.turno()) {
                case "MANHA" -> !now.isBefore(LocalTime.of(8, 0)) && now.isBefore(LocalTime.NOON);
                case "TARDE" -> !now.isBefore(LocalTime.of(13, 0)) && now.isBefore(LocalTime.of(17, 0));
                default -> false;
            };
            if (!turnoValido) return negar();
        }
        return consulta;
    }

    private static <T> T negar() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, VINCULO_BLOCK_MESSAGE);
    }
}
