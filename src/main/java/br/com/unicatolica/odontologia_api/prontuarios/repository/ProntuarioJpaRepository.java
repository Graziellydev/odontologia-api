package br.com.unicatolica.odontologia_api.prontuarios.repository;

import br.com.unicatolica.odontologia_api.prontuarios.entity.Prontuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProntuarioJpaRepository extends JpaRepository<Prontuario, Long> {
    Optional<Prontuario> findByConsultaId(Long consultaId);
}
