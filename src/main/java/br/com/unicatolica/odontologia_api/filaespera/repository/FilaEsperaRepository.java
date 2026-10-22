package br.com.unicatolica.odontologia_api.filaespera.repository;

import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaItemDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.TempoMedioEsperaDTO;
import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FilaEsperaRepository
        extends JpaRepository<FilaEspera, Long> {

    @Query(value = """
            SELECT
                f.id AS "id",
                f.paciente_id AS "pacienteId",
                p.nome_completo AS "pacienteNome",
                f.procedimento_id AS "procedimentoId",
                pr.nome AS "procedimentoNome",
                pr.especialidade AS "especialidade",
                f.dente AS "dente",
                f.classe AS "classe",
                f.nivel_urgencia AS "nivelUrgencia",
                f.prioridade_reagendamento AS "prioridadeReagendamento",
                f.status_fila AS "statusFila",
                f.data_entrada_fila AS "dataEntradaFila",
                CAST(
                    CURRENT_DATE - CAST(f.data_entrada_fila AS DATE)
                    AS INTEGER
                ) AS "diasEspera",
                f.observacao AS "observacao"
            FROM fila_espera f
            JOIN pacientes p ON p.id = f.paciente_id
            JOIN procedimentos pr ON pr.id = f.procedimento_id
            WHERE f.status_fila = 'AGUARDANDO'
              AND (
                  CAST(:procedimentoId AS BIGINT) IS NULL
                  OR f.procedimento_id = :procedimentoId
              )
              AND (
                  CAST(:especialidade AS VARCHAR) IS NULL
                  OR LOWER(pr.especialidade)
                     LIKE LOWER(CONCAT('%', :especialidade, '%'))
              )
              AND (
                  CAST(:dente AS VARCHAR) IS NULL
                  OR f.dente = :dente
              )
              AND (
                  CAST(:classe AS VARCHAR) IS NULL
                  OR f.classe = :classe
              )
              AND (
                  CAST(:nivelUrgencia AS VARCHAR) IS NULL
                  OR UPPER(f.nivel_urgencia) = UPPER(:nivelUrgencia)
              )
              AND (
                  CAST(:diasEsperaMin AS INTEGER) IS NULL
                  OR CURRENT_DATE - CAST(f.data_entrada_fila AS DATE)
                     >= :diasEsperaMin
              )
              AND (
                  CAST(:diasEsperaMax AS INTEGER) IS NULL
                  OR CURRENT_DATE - CAST(f.data_entrada_fila AS DATE)
                     <= :diasEsperaMax
              )
            ORDER BY
                CASE UPPER(f.nivel_urgencia)
                    WHEN 'ALTA' THEN 1
                    WHEN 'PNE' THEN 2
                    WHEN 'MEDIA' THEN 3
                    WHEN 'MÉDIA' THEN 3
                    WHEN 'BAIXA' THEN 4
                    ELSE 5
                END,
                f.data_entrada_fila ASC,
                f.prioridade_reagendamento DESC
            """, nativeQuery = true)
    List<FilaEsperaItemDTO> pesquisarFila(
            @Param("procedimentoId") Long procedimentoId,
            @Param("especialidade") String especialidade,
            @Param("dente") String dente,
            @Param("classe") String classe,
            @Param("nivelUrgencia") String nivelUrgencia,
            @Param("diasEsperaMin") Integer diasEsperaMin,
            @Param("diasEsperaMax") Integer diasEsperaMax);

    @Query(value = """
            WITH primeiros_atendimentos AS (
                SELECT
                    tr.triagem_item_id,
                    MIN(v.data) AS data_primeiro_atendimento
                FROM tratamentos tr
                JOIN consultas c
                    ON c.tratamento_id = tr.id
                   AND c.status_consulta = 'REALIZADO'
                JOIN vagas v
                    ON v.id = c.vaga_id
                WHERE tr.triagem_item_id IS NOT NULL
                GROUP BY tr.triagem_item_id
            )
            SELECT
                pr.especialidade AS "especialidade",
                CAST(
                    ROUND(
                        AVG(
                            (pa.data_primeiro_atendimento
                             - CAST(t.data_triagem AS DATE))::numeric
                        ), 2
                    ) AS DOUBLE PRECISION
                ) AS "mediaDias",
                COUNT(DISTINCT ti.id) AS "totalAtendidos",
                (
                    AVG(
                        (pa.data_primeiro_atendimento
                         - CAST(t.data_triagem AS DATE))::numeric
                    ) > 30
                ) AS "gargalo"
            FROM triagem_itens ti
            JOIN triagens t
                ON t.id = ti.triagem_id
            JOIN procedimentos pr
                ON pr.id = ti.procedimento_id
            JOIN primeiros_atendimentos pa
                ON pa.triagem_item_id = ti.id
            WHERE pa.data_primeiro_atendimento
                  >= CAST(t.data_triagem AS DATE)
            GROUP BY pr.especialidade
            ORDER BY AVG(
                (pa.data_primeiro_atendimento
                 - CAST(t.data_triagem AS DATE))::numeric
            ) DESC
            """, nativeQuery = true)
    List<TempoMedioEsperaDTO> calcularTempoMedioPorEspecialidade();
}