package br.com.unicatolica.odontologia_api.dto;

import java.util.List;

public record PainelMetasResponse(
        Long alunoId,
        String nomeAluno,
        String semestreLetivo,
        Long totalExigido,
        Long totalConcluido,
        Long saldoPendente,
        int percentualConcluido,
        boolean metaCumprida,
        List<ProgressoDisciplinaResponse> progressoPorDisciplina,
        List<MetaProcedimentoResponse> procedimentos) {
}