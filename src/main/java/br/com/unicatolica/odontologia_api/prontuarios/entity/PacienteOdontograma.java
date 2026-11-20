package br.com.unicatolica.odontologia_api.prontuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "paciente_odontograma")
public class PacienteOdontograma {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "dente", nullable = false, length = 2)
    private String dente;

    @Column(name = "face_dente", length = 10)
    private String faceDente;

    @Column(name = "condicao_dente", nullable = false, length = 50)
    private String condicaoDente;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "atualizado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;
}
