package br.com.unicatolica.odontologia_api.prontuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrarAdendoRequest(@NotBlank @Size(max = 20000) String texto) {
}
