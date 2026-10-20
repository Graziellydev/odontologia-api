package br.com.unicatolica.odontologia_api.dto;

import java.util.List;

public record PainelMetasResponse(
    Long alunoId,
    boolean metaCumprida,
    List<MetaProcedimentoResponse> pendencias) {
}