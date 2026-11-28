package br.com.unicatolica.odontologia_api.satisfacao.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RelatorioSatisfacaoRepository {

    // Só entram respostas de verdade: pesquisa sem resposta (nota nula) fica de fora
    private static final String BASE = """
            FROM avaliacoes_satisfacao a
            JOIN consultas c ON c.id = a.consulta_id
            JOIN turmas t ON t.id = c.turma_id
            JOIN usuarios u ON u.id = c.aluno_id
            WHERE a.nota IS NOT NULL
            """;

    private static final String FILTRO_SEMESTRE = " AND t.semestre_letivo = ? ";

    private static final RowMapper<LinhaAvaliacao> LINHA_MAPPER = (rs, rowNum) -> new LinhaAvaliacao(
            rs.getInt("nota"),
            rs.getObject("data_resposta", OffsetDateTime.class),
            rs.getString("aluno"),
            rs.getString("comentario"));

    private final JdbcTemplate jdbcTemplate;

    public RelatorioSatisfacaoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> listarSemestres() {
        return jdbcTemplate.queryForList("""
                SELECT DISTINCT semestre_letivo
                FROM turmas
                ORDER BY semestre_letivo DESC
                """, String.class);
    }

    public Resumo resumo(String semestre) {
        String sql = "SELECT COUNT(*) AS total, ROUND(AVG(a.nota), 2) AS media "
                + BASE + filtro(semestre);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            BigDecimal media = rs.getBigDecimal("media");
            return new Resumo(rs.getLong("total"), media == null ? null : media.doubleValue());
        }, argumentos(semestre));
    }

    public Map<Integer, Long> contagemPorNota(String semestre) {
        String sql = "SELECT a.nota AS nota, COUNT(*) AS total "
                + BASE + filtro(semestre) + " GROUP BY a.nota";
        Map<Integer, Long> contagem = new LinkedHashMap<>();
        jdbcTemplate.query(sql,
                (RowCallbackHandler) rs -> contagem.put(rs.getInt("nota"), rs.getLong("total")),
                argumentos(semestre));
        return contagem;
    }

    public List<LinhaAvaliacao> listar(String semestre, int limite, long deslocamento) {
        String sql = "SELECT a.nota AS nota, a.data_resposta AS data_resposta, "
                + "u.nome AS aluno, a.comentario AS comentario "
                + BASE + filtro(semestre)
                + " ORDER BY a.data_resposta DESC, a.id DESC LIMIT ? OFFSET ?";
        List<Object> argumentos = new ArrayList<>();
        if (semestre != null) {
            argumentos.add(semestre);
        }
        argumentos.add(limite);
        argumentos.add(deslocamento);
        return jdbcTemplate.query(sql, LINHA_MAPPER, argumentos.toArray());
    }

    private static String filtro(String semestre) {
        return semestre == null ? "" : FILTRO_SEMESTRE;
    }

    private static Object[] argumentos(String semestre) {
        return semestre == null ? new Object[0] : new Object[] { semestre };
    }

    public record Resumo(long total, Double media) {
    }

    public record LinhaAvaliacao(int nota, OffsetDateTime dataResposta, String aluno, String comentario) {
    }
}