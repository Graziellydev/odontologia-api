package br.com.unicatolica.odontologia_api.notificacoes.repository;

import java.time.LocalDate;

public record CandidatoLembrete24h(
        Long consultaId,
        Long pacienteId,
        String nomePaciente,
        LocalDate dataConsulta,
        String turno,
        String template) {
}
