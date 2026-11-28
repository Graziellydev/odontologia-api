package br.com.unicatolica.odontologia_api.prontuarios.repository;

import br.com.unicatolica.odontologia_api.prontuarios.entity.ProntuarioAdendo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProntuarioAdendoJpaRepository extends JpaRepository<ProntuarioAdendo, Long> {
}
