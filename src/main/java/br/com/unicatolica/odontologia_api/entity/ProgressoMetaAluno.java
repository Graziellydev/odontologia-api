package br.com.unicatolica.odontologia_api.entity;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Immutable
@Table(name = "v_progresso_metas_aluno")
@IdClass(ProgressoMetaAlunoId.class)
@Getter
@NoArgsConstructor

public class ProgressoMetaAluno {

    @Id
    @Column(name = "aluno_id")
    private Long alunoId;

    @Id
    @Column(name = "turma_id")
    private Long turmaId;

    @Id
    @Column(name = "procedimento_id")
    private Long procedimentoId;

    @Column(name = "semestre_letivo")
    private String semestreLetivo;

    @Column(name = "disciplina_id")
    private Long disciplinaId;

    @Column(name = "disciplina")
    private String disciplina;

    @Column(name = "procedimento")
    private String procedimento;

    @Column(name = "quantidade_exigida")
    private Integer quantidadeExigida;

    @Column(name = "quantidade_concluida")
    private Long quantidadeConcluida;
}