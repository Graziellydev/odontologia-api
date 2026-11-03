package br.com.unicatolica.odontologia_api.consultas.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AgendamentoResponseDTO(
        Long id, Long pacienteId, String paciente, Long alunoId, String aluno,
        Long turmaId, Long filaEsperaId, Long procedimentoId, String procedimento,
        String especialidade, String dente, String classe, Long vagaId, String cadeira,
        LocalDate data, String turno, String status, OffsetDateTime criadoEm,
        Long consultaOrigemId, Long notificacaoId, boolean notificacaoSimulada) { }
