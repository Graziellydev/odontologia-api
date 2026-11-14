package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vaga v where v.id = :id")
    Optional<Vaga> buscarPorIdParaAtualizacao(@Param("id") Long id);

    @Query(value = """
            INSERT INTO vagas (cadeira_id, data, turno, bloqueada)
            VALUES (:cadeiraId, :data, :turno, FALSE)
            ON CONFLICT (cadeira_id, data, turno) DO NOTHING
            """, nativeQuery = true)
    @Modifying
    int criarSeAusente(@Param("cadeiraId") Long cadeiraId, @Param("data") LocalDate data,
                       @Param("turno") String turno);

    @Query(value = """
            SELECT v.* FROM vagas v
            WHERE v.cadeira_id = :cadeiraId AND v.data = :data AND v.turno = :turno
            FOR UPDATE
            """, nativeQuery = true)
    Optional<Vaga> buscarPorCadeiraDataTurnoParaAtualizacao(@Param("cadeiraId") Long cadeiraId,
                                                             @Param("data") LocalDate data,
                                                             @Param("turno") String turno);

    @Query(value = """
            SELECT v.* FROM vagas v
            JOIN cadeiras ca ON ca.id = v.cadeira_id AND ca.ativa = TRUE
            WHERE v.data = :data AND v.turno = :turno AND NOT v.bloqueada
              AND NOT EXISTS (
                  SELECT 1 FROM consultas c
                  WHERE c.vaga_id = v.id
                    AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
              )
            ORDER BY v.id
            LIMIT 1
            FOR UPDATE OF v SKIP LOCKED
            """, nativeQuery = true)
    Optional<Vaga> reservarPrimeiraDisponivel(
            @Param("data") LocalDate data,
            @Param("turno") String turno);
}
