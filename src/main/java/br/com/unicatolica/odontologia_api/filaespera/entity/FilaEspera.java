package br.com.unicatolica.odontologia_api.filaespera.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "fila_espera")
@Getter
@Setter
@NoArgsConstructor
public class FilaEspera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "triagem_item_id")
    private Long triagemItemId;

    @Column(name = "origem", nullable = false, length = 20)
    private String origem = "TRIAGEM";

    @Column(name = "procedimento_id", nullable = false)
    private Long procedimentoId;

    @Column(name = "dente", length = 2)
    private String dente;

    @Column(name = "classe", length = 3)
    private String classe;

    @Column(name = "nivel_urgencia", nullable = false, length = 10)
    private String nivelUrgencia;

    @Column(name = "prioridade_reagendamento", nullable = false)
    private Boolean prioridadeReagendamento = false;

    @Column(name = "status_fila", nullable = false, length = 15)
    private String statusFila = "AGUARDANDO";

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @Column(name = "data_entrada_fila", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime dataEntradaFila;

    @Column(name = "data_saida_fila")
    private OffsetDateTime dataSaidaFila;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;
}