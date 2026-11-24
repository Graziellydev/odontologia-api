package br.com.unicatolica.odontologia_api.satisfacao.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record RelatorioSatisfacaoResponse(
        String semestre,
        Double mediaGeral,
        long totalRespostas,
        List<PorNota> distribuicao,
        List<Avaliacao> avaliacoes,
        int pagina,
        int tamanho,
        int totalPaginas) {

    public record PorNota(int nota, long total) {
    }

    // Sem dados do paciente, de propósito (RN-BUS05 / LGPD)
    public record Avaliacao(int nota, OffsetDateTime dataResposta, String aluno, String comentario) {
    }
}