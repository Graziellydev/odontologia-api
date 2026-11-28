package br.com.unicatolica.odontologia_api.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Modalidade;

@Repository
public class DesempenhoAcademicoRepository {

    private static final String QUERY = """
            WITH quantidades_concluidas AS (
                SELECT pr.aluno_id, pr.turma_id, pr.procedimento_id, COUNT(*) AS quantidade_concluida
                FROM prontuarios pr
                JOIN consultas c ON c.id = pr.consulta_id
                JOIN turmas t0 ON t0.id = pr.turma_id
                JOIN usuarios u0 ON u0.id = pr.aluno_id AND u0.perfil = 'ALUNO'
                WHERE t0.semestre_letivo = ?
                  AND c.status_consulta NOT IN (
                      'CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE',
                      'FALTOU_PACIENTE', 'FALTOU_ALUNO')
                  AND ((? = 'ALUNO' AND u0.matricula_ou_registro = ?)
                       OR (? = 'TURMA' AND t0.codigo_turma = ?))
                GROUP BY pr.aluno_id, pr.turma_id, pr.procedimento_id
            )
            SELECT u.id AS aluno_id,
                   u.nome AS nome_aluno,
                   u.matricula_ou_registro AS matricula_aluno,
                   t.id AS turma_id,
                   t.codigo_turma,
                   t.semestre_letivo AS semestre,
                   d.id AS disciplina_id,
                   d.nome AS disciplina,
                   p.id AS procedimento_id,
                   p.nome AS procedimento,
                   dp.quantidade_exigida,
                   COALESCE(q.quantidade_concluida, 0) AS quantidade_concluida
            FROM turmas t
            JOIN disciplinas d ON d.id = t.disciplina_id
            LEFT JOIN matriculas_turma m ON m.turma_id = t.id
            LEFT JOIN usuarios u ON u.id = m.aluno_id AND u.perfil = 'ALUNO'
            LEFT JOIN disciplina_procedimentos dp
                   ON dp.disciplina_id = d.id AND dp.ativo = TRUE AND dp.quantidade_exigida > 0
            LEFT JOIN procedimentos p ON p.id = dp.procedimento_id
            LEFT JOIN quantidades_concluidas q
                   ON q.aluno_id = u.id AND q.turma_id = t.id AND q.procedimento_id = p.id
            WHERE t.semestre_letivo = ?
              AND ((? = 'ALUNO' AND u.matricula_ou_registro = ?)
                   OR (? = 'TURMA' AND t.codigo_turma = ?))
            ORDER BY d.nome, p.nome, u.nome, u.matricula_ou_registro, t.id
            """;

    private static final RowMapper<LinhaDesempenho> LINHA_MAPPER = (rs, rowNum) -> new LinhaDesempenho(
            rs.getObject("aluno_id", Long.class),
            rs.getString("nome_aluno"),
            rs.getString("matricula_aluno"),
            rs.getObject("turma_id", Long.class),
            rs.getString("codigo_turma"),
            rs.getString("semestre"),
            rs.getObject("disciplina_id", Long.class),
            rs.getString("disciplina"),
            rs.getObject("procedimento_id", Long.class),
            rs.getString("procedimento"),
            rs.getObject("quantidade_exigida", Integer.class),
            rs.getLong("quantidade_concluida"));

    private final JdbcTemplate jdbcTemplate;

    public DesempenhoAcademicoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LinhaDesempenho> buscar(Modalidade modalidade, String identificador, String semestre) {
        String tipo = modalidade.name();
        return jdbcTemplate.query(QUERY, LINHA_MAPPER,
                semestre, tipo, identificador, tipo, identificador,
                semestre, tipo, identificador, tipo, identificador);
    }

    public record LinhaDesempenho(
            Long alunoId,
            String nomeAluno,
            String matriculaAluno,
            Long turmaId,
            String codigoTurma,
            String semestre,
            Long disciplinaId,
            String disciplina,
            Long procedimentoId,
            String procedimento,
            Integer quantidadeExigida,
            long quantidadeConcluida) {
    }
}
