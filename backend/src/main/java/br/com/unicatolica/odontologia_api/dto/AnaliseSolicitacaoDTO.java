package br.com.unicatolica.odontologia_api.dto;

import jakarta.validation.constraints.NotNull;

public record AnaliseSolicitacaoDTO(@NotNull Long secretariaId, String observacao) {}
