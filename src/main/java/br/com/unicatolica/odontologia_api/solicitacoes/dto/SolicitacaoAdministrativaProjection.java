package br.com.unicatolica.odontologia_api.solicitacoes.dto;

import java.time.LocalDate;
import java.time.Instant;

public interface SolicitacaoAdministrativaProjection {

    Long getId();

    Long getAlunoId();

    String getAlunoNome();

    String getAlunoIdentificacao();

    Long getTurmaId();

    String getTurma();

    String getDisciplina();

    Integer getSemestreAtual();

    Long getProcedimentoId();

    String getProcedimento();

    String getDente();

    String getClasse();

    LocalDate getDataDesejada();

    String getStatusSolicitacao();

    Instant getDataSolicitacao();

    Long getPacienteAlocadoId();

    String getPacienteAlocadoNome();
    String getObservacaoSecretaria();
    String getRespostaSecretaria();
    Long getSecretariaAnalisadorId();
    Instant getDataAnalise();
    Long getFilaEsperaId();
}
