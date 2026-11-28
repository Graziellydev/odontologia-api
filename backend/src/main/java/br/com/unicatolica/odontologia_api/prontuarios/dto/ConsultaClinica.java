package br.com.unicatolica.odontologia_api.prontuarios.dto;

import java.time.LocalDate;

public record ConsultaClinica(Long id, Long pacienteId, Long alunoId, Long turmaId,
                              Long procedimentoId, String dente, String classe,
                              String status, LocalDate data, String turno,
                              String paciente, String aluno, String procedimento,
                              String cadeira) { }
