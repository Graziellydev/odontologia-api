package br.com.unicatolica.odontologia_api.prontuarios.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "paciente_anamnese")
public class PacienteAnamnese {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "queixa_principal", columnDefinition = "TEXT")
    private String queixaPrincipal;

    @Column(name = "historia_queixa", columnDefinition = "TEXT")
    private String historiaQueixa;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "respostas_questionario", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> respostasQuestionario;

    @Column(name = "pressao_arterial", length = 20)
    private String pressaoArterial;

    @Column(name = "pulso_bpm")
    private Integer pulsoBpm;

    @Column(name = "temperatura_c", precision = 4, scale = 1)
    private BigDecimal temperaturaC;

    @Column(name = "frequencia_respiratoria")
    private Integer frequenciaRespiratoria;

    @Column(name = "medo_dentista_escala")
    private Integer medoDentistaEscala;

    @Column(name = "frequencia_escovacao")
    private Integer frequenciaEscovacao;

    @Column(name = "usa_fio_dental")
    private Boolean usaFioDental;

    @Column(name = "usa_enxaguante")
    private Boolean usaEnxaguante;

    @Column(name = "limpa_lingua")
    private Boolean limpaLingua;

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;
}
