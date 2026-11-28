package br.com.unicatolica.odontologia_api.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.unicatolica.odontologia_api.entity.ProgressoMetaAluno;
import br.com.unicatolica.odontologia_api.entity.ProgressoMetaAlunoId;

public interface ProgressoMetaAlunoRepository extends JpaRepository<ProgressoMetaAluno, ProgressoMetaAlunoId> {

    List<ProgressoMetaAluno> findByAlunoId(Long alunoId);

    List<ProgressoMetaAluno> findByAlunoIdAndSemestreLetivo(Long alunoId, String semestreLetivo);
}