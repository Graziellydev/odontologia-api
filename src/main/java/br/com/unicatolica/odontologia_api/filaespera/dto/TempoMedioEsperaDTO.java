package br.com.unicatolica.odontologia_api.filaespera.dto;

public interface TempoMedioEsperaDTO {

    String getEspecialidade();

    Double getMediaDias();

    Long getTotalAtendidos();

    Boolean getGargalo();
}