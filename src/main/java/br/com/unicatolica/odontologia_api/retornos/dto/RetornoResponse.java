package br.com.unicatolica.odontologia_api.retornos.dto;

public record RetornoResponse(
        Long consultaId,
        boolean retornoSugerido,
        Long filaEsperaId,
        String mensagem) {
}