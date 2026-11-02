package br.com.unicatolica.odontologia_api.dto;

import java.util.List;

public record MetricasCoordenadorResponse(
        String semestre,
        long totalAtendimentosConcluidos,
        List<PorSemestre> porSemestre,
        List<PorDisciplina> porDisciplina,
        List<PorEspecialidade> porEspecialidade) {

    public record PorSemestre(String semestre, long total) {
    }

    public record PorDisciplina(String disciplina, long total) {
    }

    public record PorEspecialidade(String especialidade, long total) {
    }
}