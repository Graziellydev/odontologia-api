package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    interface AusenciaProjection {
        Long getConsultaId();
        java.time.LocalDate getData();
        String getAusencia();
        String getVinculoPaciente();
        String getVinculoAluno();
        String getSituacao();
        String getProximoPasso();
    }

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Consulta c where c.id = :id")
    Optional<Consulta> buscarPorIdParaAtualizacao(@Param("id") Long id);

    @Query(value = """
            SELECT c.id AS "consultaId",
                   v.data AS "data",
                   CASE c.status_consulta
                       WHEN 'FALTOU_PACIENTE' THEN 'Paciente'
                       WHEN 'FALTOU_ALUNO' THEN 'Aluno'
                   END AS "ausencia",
                   p.nome_completo AS "vinculoPaciente",
                   u.nome AS "vinculoAluno",
                   CASE WHEN EXISTS (
                       SELECT 1 FROM consultas reagendada
                       WHERE reagendada.consulta_origem_id = c.id
                         AND reagendada.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                   ) THEN 'Reagendada' ELSE 'Pendente' END AS "situacao",
                   CASE WHEN EXISTS (
                       SELECT 1 FROM consultas reagendada
                       WHERE reagendada.consulta_origem_id = c.id
                         AND reagendada.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                   ) THEN 'Ver reagendamento' ELSE 'Reagendar' END AS "proximoPasso"
            FROM consultas c
            JOIN vagas v ON v.id = c.vaga_id
            JOIN pacientes p ON p.id = c.paciente_id
            JOIN usuarios u ON u.id = c.aluno_id
            WHERE c.status_consulta IN ('FALTOU_PACIENTE', 'FALTOU_ALUNO')
              AND (:mes IS NULL OR EXTRACT(MONTH FROM v.data) = :mes)
              AND (:quemFaltou IS NULL
                   OR (:quemFaltou = 'PACIENTE' AND c.status_consulta = 'FALTOU_PACIENTE')
                   OR (:quemFaltou = 'ALUNO' AND c.status_consulta = 'FALTOU_ALUNO'))
            ORDER BY v.data DESC, c.id DESC
            """, nativeQuery = true)
    List<AusenciaProjection> listarAusencias(@Param("mes") Integer mes,
                                             @Param("quemFaltou") String quemFaltou);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM consultas
                WHERE vaga_id = :vagaId
                  AND status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
            )
            """, nativeQuery = true)
    boolean existeConsultaAtivaNaVaga(@Param("vagaId") Long vagaId);
}
