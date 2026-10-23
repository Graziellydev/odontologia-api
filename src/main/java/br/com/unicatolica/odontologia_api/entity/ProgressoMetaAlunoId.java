package br.com.unicatolica.odontologia_api.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode

public class ProgressoMetaAlunoId implements Serializable {
    private Long alunoId;
    private Long turmaId;
    private Long procedimentoId;
}

