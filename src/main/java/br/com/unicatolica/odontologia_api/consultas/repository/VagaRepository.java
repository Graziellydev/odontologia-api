package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vaga v where v.id = :id")
    Optional<Vaga> buscarPorIdParaAtualizacao(@Param("id") Long id);
}
