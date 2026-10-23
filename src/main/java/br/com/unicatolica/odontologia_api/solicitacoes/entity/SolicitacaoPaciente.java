package br.com.unicatolica.odontologia_api.solicitacoes.entity;

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

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitacoes_pacientes")
@Getter
@Setter
@NoArgsConstructor
public class SolicitacaoPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    @Column(name = "procedimento_id", nullable = false)
    private Long procedimentoId;

    @Column(name = "dente", nullable = false, length = 2)
    private String dente;

    @Column(name = "classe", length = 3)
    private String classe;

    @Column(name = "data_desejada")
    private LocalDate dataDesejada;

    @Column(name = "status_solicitacao", nullable = false, length = 25)
    private String statusSolicitacao = "PENDENTE_APROVACAO";

    @Column(name = "fila_espera_id")
    private Long filaEsperaId;

    @Column(name = "paciente_alocado_id")
    private Long pacienteAlocadoId;

    @Column(name = "secretaria_analisador_id")
    private Long secretariaAnalisadorId;

    @Column(name = "data_analise")
    private OffsetDateTime dataAnalise;

    @Column(name = "observacao_secretaria", columnDefinition = "TEXT")
    private String observacaoSecretaria;

    @Column(name = "resposta_secretaria", columnDefinition = "TEXT")
    private String respostaSecretaria;

    @Column(name = "data_solicitacao", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime dataSolicitacao;

    @Column(name = "atualizado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(name = "versao", nullable = false)
    private Long versao;
}