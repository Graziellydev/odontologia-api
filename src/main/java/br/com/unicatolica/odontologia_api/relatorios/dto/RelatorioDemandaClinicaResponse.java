package br.com.unicatolica.odontologia_api.relatorios.dto;

import java.util.List;

public record RelatorioDemandaClinicaResponse(FiltrosRelatorioResponse filtrosAplicados,
        long totalItensTriagem, long totalConsultas, List<DistribuicaoItemResponse> porProcedimento,
        List<DistribuicaoItemResponse> porEspecialidade, String aviso) {}
