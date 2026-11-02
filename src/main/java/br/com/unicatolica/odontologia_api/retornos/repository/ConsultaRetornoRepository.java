package br.com.unicatolica.odontologia_api.retornos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.unicatolica.odontologia_api.retornos.entity.ConsultaRetorno;

public interface ConsultaRetornoRepository extends JpaRepository<ConsultaRetorno, Long> {

    // Linha da lista de atendimentos do dia (os nomes batem com os apelidos do SQL)
    interface AtendimentoView {
        Long getConsultaId();
        String getPaciente();
        String getProcedimento();
        String getDente();
        String getClasse();
        String getData();
        String getTurno();
    }

    @Query(value = """
            SELECT c.id                               AS "consultaId",
                   p.nome_completo                    AS "paciente",
                   COALESCE(pr.nome, 'Não informado') AS "procedimento",
                   c.dente                            AS "dente",
                   c.classe                           AS "classe",
                   CAST(v.data AS text)               AS "data",
                   v.turno                            AS "turno"
            FROM consultas c
            JOIN pacientes p ON p.id = c.paciente_id
            JOIN vagas v ON v.id = c.vaga_id
            LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
            WHERE c.aluno_id = :alunoId
              AND c.status_consulta IN ('AGENDADO', 'CONFIRMADA_24H')
              AND v.data = CAST(now() AT TIME ZONE 'America/Sao_Paulo' AS date)
            ORDER BY v.turno, c.id
            """, nativeQuery = true)
    List<AtendimentoView> listarDeHoje(@Param("alunoId") Long alunoId);

    // Evita duplicar a mesma demanda aguardando na fila (índice único do banco)
    @Query(value = """
            SELECT COUNT(*) FROM fila_espera
            WHERE paciente_id = :pacienteId
              AND procedimento_id = :procedimentoId
              AND COALESCE(dente, '') = :dente
              AND COALESCE(classe, '') = :classe
              AND status_fila = 'AGUARDANDO'
            """, nativeQuery = true)
    long contarDemandaAguardando(@Param("pacienteId") Long pacienteId,
                                 @Param("procedimentoId") Long procedimentoId,
                                 @Param("dente") String dente,
                                 @Param("classe") String classe);

    @Query(value = """
            SELECT nivel_urgencia FROM triagens
            WHERE paciente_id = :pacienteId
            ORDER BY data_triagem DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<String> ultimaUrgenciaDaTriagem(@Param("pacienteId") Long pacienteId);
}