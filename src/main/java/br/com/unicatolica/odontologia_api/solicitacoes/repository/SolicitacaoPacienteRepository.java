package br.com.unicatolica.odontologia_api.solicitacoes.repository;

import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitacaoPacienteRepository
        extends JpaRepository<SolicitacaoPaciente, Long> {

    List<SolicitacaoPaciente> findByStatusSolicitacaoOrderByDataSolicitacaoAsc(
            String statusSolicitacao);

    List<SolicitacaoPaciente> findByAlunoIdOrderByDataSolicitacaoDesc(
            Long alunoId);
}