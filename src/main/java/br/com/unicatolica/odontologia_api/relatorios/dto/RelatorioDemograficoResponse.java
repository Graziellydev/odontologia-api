package br.com.unicatolica.odontologia_api.relatorios.dto;

import java.util.List;

public record RelatorioDemograficoResponse(FiltrosRelatorioResponse filtrosAplicados,
        BaseAnalise baseAnalise, DistribuicaoItemResponse faixaMaisFrequente,
        DistribuicaoItemResponse origemMaisFrequente,
        List<DistribuicaoItemResponse> distribuicaoFaixaEtaria,
        List<DistribuicaoItemResponse> distribuicaoTerritorial) {
    public record BaseAnalise(long totalPacientesUnicos, String descricao) {}
}
