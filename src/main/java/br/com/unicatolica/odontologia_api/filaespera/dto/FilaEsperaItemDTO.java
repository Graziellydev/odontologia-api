package br.com.unicatolica.odontologia_api.filaespera.dto;

import java.time.OffsetDateTime;

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

    OffsetDateTime getDataEntradaFila();

    Integer getDiasEspera();

    String getObservacao();
}