package br.com.unicatolica.odontologia_api.prontuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "prontuarios")
@Getter
@Setter
@NoArgsConstructor
public class Prontuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "consulta_id", nullable = false, unique = true)
    private Long consultaId;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    @Column(name = "procedimento_id", nullable = false)
    private Long procedimentoId;

    @Column(name = "dente", length = 2)
    private String dente;

    @Column(name = "classe", length = 3)
    private String classe;

    @Column(name = "evolucao_clinica", nullable = false, columnDefinition = "TEXT")
    private String evolucaoClinica;

    @Column(name = "assinatura_digital", nullable = false, columnDefinition = "TEXT")
    private String assinaturaDigital;

    @Column(name = "data_registro", nullable = false)
    private OffsetDateTime dataRegistro;
}
