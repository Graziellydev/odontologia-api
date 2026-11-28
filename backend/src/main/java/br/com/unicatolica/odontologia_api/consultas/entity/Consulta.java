package br.com.unicatolica.odontologia_api.consultas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/** Mapeia consultas e mantém as chaves estrangeiras compatíveis com o schema existente. */
@Entity
@Table(name = "consultas")
@Getter
@Setter
@NoArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    @Column(name = "procedimento_id")
    private Long procedimentoId;

    @Column(name = "dente", length = 2)
    private String dente;

    @Column(name = "classe", length = 3)
    private String classe;

    @Column(name = "vaga_id", nullable = false)
    private Long vagaId;

    @Column(name = "fila_espera_id")
    private Long filaEsperaId;

    @Column(name = "consulta_origem_id")
    private Long consultaOrigemId;

    @Column(name = "criado_por_id")
    private Long criadoPorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_consulta", nullable = false, length = 25)
    private StatusConsulta statusConsulta = StatusConsulta.AGENDADO;

    @Column(name = "prontuario_liberado", nullable = false)
    private Boolean prontuarioLiberado = false;

    @Column(name = "falta_justificada")
    private Boolean faltaJustificada;

    @Column(name = "justificativa_falta", columnDefinition = "TEXT")
    private String justificativaFalta;

    @Column(name = "retorno_sugerido", nullable = false)
    private Boolean retornoSugerido = false;

    @Column(name = "justificativa_retorno", columnDefinition = "TEXT")
    private String justificativaRetorno;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;
}
