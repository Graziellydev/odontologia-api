package br.com.unicatolica.odontologia_api.consultas.dto;

import java.time.LocalDate;

public record TurnoGradeDTO(
        LocalDate data,
        String turno,
        Long cadeiraId,
        String cadeiraNumero,
        Long vagaId,
        boolean bloqueada,
        String motivoBloqueio,
        String situacao,
        Long consultaId,
        String paciente,
        String aluno,
        String disciplina,
        String procedimento,
        String statusConsulta) { }
