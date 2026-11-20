package br.com.unicatolica.odontologia_api.retornos.dto;

public record AtendimentoDoTurnoDTO(
        Long consultaId,
        Long pacienteId,
        Long procedimentoId,
        String cadeira,
        String paciente,
        String procedimento,
        String dente,
        String classe,
        String data,
        String turno,
        String status) {
}
