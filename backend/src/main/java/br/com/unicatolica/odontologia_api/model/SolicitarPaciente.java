
package br.com.unicatolica.odontologia_api.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "solicitacoes_pacientes")
@Data
@NoArgsConstructor
public class SolicitarPaciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    @Column(name = "procedimento_id", nullable = false)
    private Long procedimentoId;

    @Column(nullable = false, length = 2)
    private String dente;

    @Column(length = 3)
    private String classe;

    @Column(name = "data_desejada")
    private LocalDate dataDesejada;

    @Column(name = "status_solicitacao", nullable = false)
    private String statusSolicitacao = "PENDENTE_APROVACAO";

    @Column(name = "fila_espera_id")
    private Long filaEsperaId;

    @Column(name = "paciente_alocado_id")
    private Long pacienteAlocadoId;

    @Column(name = "secretaria_analisador_id")
    private Long secretariaAnalisadorId;

    @Column(name = "data_analise")
    private OffsetDateTime dataAnalise;

    @Column(name = "observacao_secretaria")
    private String observacaoSecretaria;

    @Column(name = "resposta_secretaria")
    private String respostaSecretaria;

    @Column(name = "data_solicitacao", insertable = false, updatable = false)
    private OffsetDateTime dataSolicitacao;

    @Column(name = "atualizado_em", insertable = false)
    private OffsetDateTime atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;
}
