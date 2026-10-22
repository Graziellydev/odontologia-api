package br.com.unicatolica.odontologia_api.repository;

import br.com.unicatolica.odontologia_api.model.SolicitarPaciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface SolicitarPacienteRepository extends JpaRepository<SolicitarPaciente, Long> {
    List<SolicitarPaciente> findByStatusSolicitacaoOrderByDataSolicitacaoAsc(String status);
    List<SolicitarPaciente> findByAlunoIdOrderByDataSolicitacaoDesc(Long alunoId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from SolicitarPaciente s where s.id = :id")
    Optional<SolicitarPaciente> buscarParaAtualizar(@org.springframework.data.repository.query.Param("id") Long id);
}
