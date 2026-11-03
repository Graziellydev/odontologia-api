package br.com.unicatolica.odontologia_api.solicitacoes.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record SolicitacaoAdministrativaDTO(
        Long id,
        Long alunoId,
        String alunoNome,
        String alunoIdentificacao,
        Long turmaId,
        String turma,
        String disciplina,
        Integer semestreAtual,
        Long procedimentoId,
        String procedimento,
        String dente,
        String classe,
        LocalDate dataDesejada,
        String statusSolicitacao,
        OffsetDateTime dataSolicitacao,
        Long pacienteAlocadoId,
        String pacienteAlocadoNome,
        String observacaoSecretaria,
        String respostaSecretaria,
        Long secretariaAnalisadorId,
        OffsetDateTime dataAnalise,
        Long filaEsperaId) {
}
