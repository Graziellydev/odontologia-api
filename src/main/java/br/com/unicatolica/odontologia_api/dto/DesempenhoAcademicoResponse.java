package br.com.unicatolica.odontologia_api.dto;

import java.util.List;

public record DesempenhoAcademicoResponse(
        Modalidade modalidade,
        String identificador,
        String semestre,
        String nomeAluno,
        String matriculaAluno,
        String codigoTurma,
        Resumo resumo,
        List<LinhaHistorico> historico,
        List<AlunoComparativo> alunos) {

    public DesempenhoAcademicoResponse {
        historico = List.copyOf(historico);
        alunos = List.copyOf(alunos);
    }

    public enum Modalidade {
        ALUNO,
        TURMA
    }

    public record Resumo(
            long exigidos,
            long concluidos,
            long pendentes,
            int progressoPercentual) {
    }

    public record LinhaHistorico(
            String disciplina,
            String procedimento,
            long exigidos,
            long concluidos,
            long pendentes,
            int progressoPercentual) {
    }

    public record AlunoComparativo(
            String nomeAluno,
            String matriculaAluno,
            long exigidos,
            long concluidos,
            long pendentes,
            int progressoPercentual) {
    }
}
