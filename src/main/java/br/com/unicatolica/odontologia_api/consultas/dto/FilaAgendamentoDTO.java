package br.com.unicatolica.odontologia_api.consultas.dto;

public record FilaAgendamentoDTO(
        Long filaEsperaId, Long pacienteId, String paciente, Long procedimentoId,
        String procedimento, String especialidade, String dente, String classe,
        String statusFila, String origem) { }
