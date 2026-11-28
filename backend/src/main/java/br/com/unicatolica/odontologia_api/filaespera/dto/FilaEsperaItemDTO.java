package br.com.unicatolica.odontologia_api.filaespera.dto;

import java.time.Instant;

public interface FilaEsperaItemDTO {

    Long getId();

    Long getPacienteId();

    String getPacienteNome();

    Long getProcedimentoId();

    String getProcedimentoNome();

    String getEspecialidade();

    String getDente();

    String getClasse();

    String getNivelUrgencia();

    Boolean getPrioridadeReagendamento();

    String getStatusFila();

    Instant getDataEntradaFila();

    Integer getDiasEspera();

    String getObservacao();

    String getOrigem();
}