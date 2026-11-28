package br.com.unicatolica.odontologia_api.solicitacoes.repository;

import br.com.unicatolica.odontologia_api.solicitacoes.dto.SolicitacaoAdministrativaProjection;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SolicitacaoPacienteRepository
        extends JpaRepository<SolicitacaoPaciente, Long> {

    List<SolicitacaoPaciente> findByStatusSolicitacaoOrderByDataSolicitacaoAsc(
            String statusSolicitacao);

    List<SolicitacaoPaciente> findByAlunoIdOrderByDataSolicitacaoDesc(
            Long alunoId);

    @Query(value = """
            SELECT
                sp.id AS "id",
                sp.aluno_id AS "alunoId",
                u.nome AS "alunoNome",
                u.matricula_ou_registro AS "alunoIdentificacao",
                sp.turma_id AS "turmaId",
                t.codigo_turma AS "turma",
                d.nome AS "disciplina",
                u.semestre_atual AS "semestreAtual",
                sp.procedimento_id AS "procedimentoId",
                p.nome AS "procedimento",
                sp.dente AS "dente",
                sp.classe AS "classe",
                sp.data_desejada AS "dataDesejada",
                sp.status_solicitacao AS "statusSolicitacao",
                sp.data_solicitacao AS "dataSolicitacao",
                sp.paciente_alocado_id AS "pacienteAlocadoId",
                pac.nome_completo AS "pacienteAlocadoNome",
                sp.observacao_secretaria AS "observacaoSecretaria",
                sp.resposta_secretaria AS "respostaSecretaria",
                sp.secretaria_analisador_id AS "secretariaAnalisadorId",
                sp.data_analise AS "dataAnalise",
                sp.fila_espera_id AS "filaEsperaId"
            FROM solicitacoes_pacientes sp
            JOIN usuarios u ON u.id = sp.aluno_id
            JOIN turmas t ON t.id = sp.turma_id
            JOIN disciplinas d ON d.id = t.disciplina_id
            JOIN procedimentos p ON p.id = sp.procedimento_id
            LEFT JOIN pacientes pac ON pac.id = sp.paciente_alocado_id
            ORDER BY sp.data_solicitacao ASC
            """, nativeQuery = true)
    List<SolicitacaoAdministrativaProjection> listarAdministrativas();
}
