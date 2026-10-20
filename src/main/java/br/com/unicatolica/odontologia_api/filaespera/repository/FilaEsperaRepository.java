package br.com.unicatolica.odontologia_api.filaespera.repository;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FilaEsperaRepository extends JpaRepository<FilaEspera, Long> {

    List<FilaEspera> findAllByOrderByDataEntradaFilaAsc();

    List<FilaEspera> findByStatusFilaOrderByDataEntradaFilaAsc(String statusFila);
}