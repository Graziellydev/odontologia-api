package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.dto.CadeiraResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.TurnoGradeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaResponseDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class VagaGestaoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public VagaGestaoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<CadeiraResponseDTO> listarCadeirasAtivas() {
        return jdbc.query("""
                SELECT id, numero, ativa
                FROM cadeiras
                WHERE ativa = TRUE
                ORDER BY numero
                """, (rs, row) -> new CadeiraResponseDTO(
                rs.getLong("id"), rs.getString("numero"), rs.getBoolean("ativa")));
    }

    public boolean cadeiraAtivaExiste(long cadeiraId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM cadeiras WHERE id = :id AND ativa = TRUE)
                """, new MapSqlParameterSource("id", cadeiraId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public boolean possuiConsultaVinculada(long vagaId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM consultas WHERE vaga_id = :vagaId)
                """, new MapSqlParameterSource("vagaId", vagaId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public boolean possuiConsultaAtiva(long vagaId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM consultas
                    WHERE vaga_id = :vagaId
                      AND status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE'))
                """, new MapSqlParameterSource("vagaId", vagaId), Boolean.class);
        return Boolean.TRUE.equals(existe);
    }

    public List<VagaResponseDTO> listar(
            LocalDate inicio, LocalDate fim, Long cadeiraId, String turno, Boolean bloqueada) {
        StringBuilder sql = new StringBuilder("""
                SELECT v.id, v.cadeira_id, ca.numero AS cadeira_numero, v.data, v.turno,
                       v.bloqueada, v.motivo_bloqueio
                FROM vagas v
                JOIN cadeiras ca ON ca.id = v.cadeira_id
                WHERE (:inicio IS NULL OR v.data >= :inicio)
                  AND (:fim IS NULL OR v.data <= :fim)
                  AND (:cadeiraId IS NULL OR v.cadeira_id = :cadeiraId)
                  AND (:turno IS NULL OR v.turno = :turno)
                  AND (:bloqueada IS NULL OR v.bloqueada = :bloqueada)
                ORDER BY v.data, v.turno, ca.numero
                """);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("inicio", inicio)
                .addValue("fim", fim)
                .addValue("cadeiraId", cadeiraId)
                .addValue("turno", turno)
                .addValue("bloqueada", bloqueada);
        return jdbc.query(sql.toString(), params, (rs, row) -> new VagaResponseDTO(
                rs.getLong("id"), rs.getLong("cadeira_id"), rs.getString("cadeira_numero"),
                rs.getObject("data", LocalDate.class), rs.getString("turno"),
                rs.getBoolean("bloqueada"), rs.getString("motivo_bloqueio")));
    }

    public VagaResponseDTO buscar(Long id) {
        List<VagaResponseDTO> resultados = jdbc.query("""
                SELECT v.id, v.cadeira_id, ca.numero AS cadeira_numero, v.data, v.turno,
                       v.bloqueada, v.motivo_bloqueio
                FROM vagas v JOIN cadeiras ca ON ca.id = v.cadeira_id
                WHERE v.id = :id
                """, new MapSqlParameterSource("id", id), (rs, row) -> new VagaResponseDTO(
                rs.getLong("id"), rs.getLong("cadeira_id"), rs.getString("cadeira_numero"),
                rs.getObject("data", LocalDate.class), rs.getString("turno"),
                rs.getBoolean("bloqueada"), rs.getString("motivo_bloqueio")));
        return resultados.stream().findFirst().orElse(null);
    }

    public GerarTurnosResponseDTO gerarTurnos(LocalDate inicio, LocalDate fim) {
        return jdbc.queryForObject("""
                WITH dias AS MATERIALIZED (
                    SELECT CAST(dia AS date) AS data
                    FROM generate_series(CAST(:inicio AS date), CAST(:fim AS date), interval '1 day') AS serie(dia)
                ), combinacoes AS MATERIALIZED (
                    SELECT ca.id AS cadeira_id, dias.data, turnos.turno
                    FROM cadeiras ca
                    CROSS JOIN dias
                    CROSS JOIN (VALUES ('MANHA'), ('TARDE')) AS turnos(turno)
                    WHERE ca.ativa = TRUE
                ), inseridas AS (
                    INSERT INTO vagas (cadeira_id, data, turno)
                    SELECT cadeira_id, data, turno FROM combinacoes
                    ON CONFLICT (cadeira_id, data, turno) DO NOTHING
                    RETURNING id
                )
                SELECT CAST((SELECT COUNT(*) FROM inseridas) AS integer) AS criadas,
                       CAST((SELECT COUNT(*) FROM combinacoes) - (SELECT COUNT(*) FROM inseridas) AS integer)
                           AS ja_existentes
                """, new MapSqlParameterSource().addValue("inicio", inicio).addValue("fim", fim),
                (rs, row) -> new GerarTurnosResponseDTO(rs.getInt("criadas"), rs.getInt("ja_existentes")));
    }

    public Long materializarBloqueio(Long cadeiraId, LocalDate data, String turno, String motivo) {
        return jdbc.queryForObject("""
                INSERT INTO vagas (cadeira_id, data, turno, bloqueada, motivo_bloqueio)
                VALUES (:cadeiraId, :data, :turno, TRUE, :motivo)
                ON CONFLICT (cadeira_id, data, turno)
                DO UPDATE SET bloqueada = TRUE, motivo_bloqueio = EXCLUDED.motivo_bloqueio
                RETURNING id
                """, new MapSqlParameterSource()
                .addValue("cadeiraId", cadeiraId)
                .addValue("data", data)
                .addValue("turno", turno)
                .addValue("motivo", motivo), Long.class);
    }

    public List<TurnoGradeDTO> listarGradeSemanal(LocalDate inicio, LocalDate fim,
                                                   String disciplina, String situacao) {
        StringBuilder sql = new StringBuilder("""
                WITH dias AS (
                    SELECT CAST(dia AS date) AS data
                    FROM generate_series(CAST(:inicio AS date), CAST(:fim AS date), interval '1 day') AS serie(dia)
                )
                SELECT dias.data, turnos.turno, ca.id AS cadeira_id, ca.numero AS cadeira_numero,
                       v.id AS vaga_id, COALESCE(v.bloqueada, FALSE) AS bloqueada,
                       v.motivo_bloqueio,
                       CASE
                         WHEN c.id IS NOT NULL THEN 'OCUPADA'
                         WHEN v.bloqueada THEN 'BLOQUEADA'
                         ELSE 'LIVRE'
                       END AS situacao,
                       c.id AS consulta_id, p.nome_completo AS paciente, u.nome AS aluno,
                       di.nome AS disciplina, pr.nome AS procedimento, c.status_consulta
                FROM cadeiras ca
                CROSS JOIN dias
                CROSS JOIN (VALUES ('MANHA'), ('TARDE')) AS turnos(turno)
                LEFT JOIN vagas v ON v.cadeira_id = ca.id AND v.data = dias.data AND v.turno = turnos.turno
                LEFT JOIN consultas c ON c.vaga_id = v.id
                    AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                LEFT JOIN pacientes p ON p.id = c.paciente_id
                LEFT JOIN usuarios u ON u.id = c.aluno_id
                LEFT JOIN turmas t ON t.id = c.turma_id
                LEFT JOIN disciplinas di ON di.id = t.disciplina_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                WHERE ca.ativa = TRUE
                """);
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("inicio", inicio)
                .addValue("fim", fim);
        if (disciplina != null && !disciplina.isBlank()) {
            sql.append(" AND di.nome = :disciplina");
            parametros.addValue("disciplina", disciplina);
        }
        if (situacao != null && !situacao.isBlank()) {
            sql.append("""
                     AND CASE
                           WHEN c.id IS NOT NULL THEN 'OCUPADA'
                           WHEN v.bloqueada THEN 'BLOQUEADA'
                           ELSE 'LIVRE'
                         END = :situacao
                    """);
            parametros.addValue("situacao", situacao);
        }
        sql.append("""
                ORDER BY dias.data,
                         CASE turnos.turno WHEN 'MANHA' THEN 1 ELSE 2 END,
                         ca.numero
                """);
        return jdbc.query(sql.toString(), parametros,
                (rs, row) -> new TurnoGradeDTO(
                        rs.getObject("data", LocalDate.class), rs.getString("turno"),
                        rs.getLong("cadeira_id"), rs.getString("cadeira_numero"),
                        (Long) rs.getObject("vaga_id"), rs.getBoolean("bloqueada"),
                        rs.getString("motivo_bloqueio"), rs.getString("situacao"),
                        (Long) rs.getObject("consulta_id"), rs.getString("paciente"),
                        rs.getString("aluno"), rs.getString("disciplina"),
                        rs.getString("procedimento"), rs.getString("status_consulta")));
    }
}
