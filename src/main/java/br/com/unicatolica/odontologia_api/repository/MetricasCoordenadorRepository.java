package br.com.unicatolica.odontologia_api.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MetricasCoordenadorRepository {

    private static final String COLUNAS_VIEW = """
            SELECT semestre_letivo, disciplina, especialidade, total_concluidos
            FROM v_atendimentos_semestre
            """;

    private static final RowMapper<LinhaAtendimento> LINHA_MAPPER = (rs, rowNum) -> new LinhaAtendimento(
            rs.getString("semestre_letivo"),
            rs.getString("disciplina"),
            rs.getString("especialidade"),
            rs.getLong("total_concluidos"));

    private final JdbcTemplate jdbcTemplate;

    public MetricasCoordenadorRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> listarSemestres() {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT semestre_letivo
                FROM turmas
                ORDER BY semestre_letivo DESC
                """, String.class);
    }

    public List<LinhaAtendimento> buscarTodos() {
        return jdbcTemplate.query(COLUNAS_VIEW + """
                ORDER BY semestre_letivo DESC, disciplina ASC, especialidade ASC NULLS LAST
                """, LINHA_MAPPER);
    }

    public List<LinhaAtendimento> buscarPorSemestre(String semestre) {
        return jdbcTemplate.query(COLUNAS_VIEW + """
                WHERE semestre_letivo = ?
                ORDER BY semestre_letivo DESC, disciplina ASC, especialidade ASC NULLS LAST
                """, LINHA_MAPPER, semestre);
    }

    public record LinhaAtendimento(
            String semestre,
            String disciplina,
            String especialidade,
            long totalConcluidos) {
    }
}