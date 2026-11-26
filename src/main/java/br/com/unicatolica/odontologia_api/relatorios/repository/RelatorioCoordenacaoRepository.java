package br.com.unicatolica.odontologia_api.relatorios.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RelatorioCoordenacaoRepository {
    private final JdbcTemplate jdbc;

    public RelatorioCoordenacaoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    private record QueryFiltro(String semestre, LocalDate inicio, LocalDate fim, String especialidade, String regiao) {}

    public List<EventoPaciente> buscarEventos(String semestre, LocalDate inicio, LocalDate fim, String especialidade, String regiao) {
        QueryFiltro f = new QueryFiltro(semestre, inicio, fim, especialidade, regiao);
        String consultas = """
                SELECT p.id paciente_id, p.data_nascimento, p.bairro, p.cidade, v.data data_evento
                FROM consultas c JOIN pacientes p ON p.id=c.paciente_id
                JOIN vagas v ON v.id=c.vaga_id JOIN turmas t ON t.id=c.turma_id
                LEFT JOIN procedimentos pr ON pr.id=c.procedimento_id
                WHERE (? IS NULL OR t.semestre_letivo=?)
                  AND (? IS NULL OR v.data>=?) AND (? IS NULL OR v.data<=?)
                  AND (? IS NULL OR pr.especialidade ILIKE ?)
                  AND (?='TODOS' OR lower(trim(p.cidade))=lower(trim(?)))
                UNION ALL
                SELECT p.id, p.data_nascimento, p.bairro, p.cidade, (tr.data_triagem AT TIME ZONE 'UTC')::date
                FROM triagens tr JOIN pacientes p ON p.id=tr.paciente_id
                WHERE ? IS NULL AND (? IS NULL OR tr.data_triagem::date>=?)
                  AND (? IS NULL OR tr.data_triagem::date<=?)
                  AND (? IS NULL OR EXISTS (SELECT 1 FROM triagem_itens ti JOIN procedimentos pr ON pr.id=ti.procedimento_id WHERE ti.triagem_id=tr.id AND pr.especialidade ILIKE ?))
                  AND (?='TODOS' OR lower(trim(p.cidade))=lower(trim(?)))
                """;
        return jdbc.query(consultas, ps -> {
            Object esp = f.especialidade() == null ? null : "%" + f.especialidade() + "%";
            ps.setObject(1, f.semestre()); ps.setObject(2, f.semestre()); ps.setObject(3, f.inicio()); ps.setObject(4, f.inicio()); ps.setObject(5, f.fim()); ps.setObject(6, f.fim()); ps.setObject(7, esp); ps.setObject(8, esp); ps.setObject(9, f.regiao()); ps.setObject(10, f.regiao());
            ps.setObject(11, f.semestre()); ps.setObject(12, f.inicio()); ps.setObject(13, f.inicio()); ps.setObject(14, f.fim()); ps.setObject(15, f.fim()); ps.setObject(16, esp); ps.setObject(17, esp); ps.setObject(18, f.regiao()); ps.setObject(19, f.regiao());
        }, (rs, n) -> new EventoPaciente(rs.getLong("paciente_id"), rs.getDate("data_nascimento").toLocalDate(),
                rs.getString("bairro"), rs.getString("cidade"), rs.getDate("data_evento").toLocalDate()));
    }

    public List<Contagem> buscarDemanda(boolean triagem, String semestre, LocalDate inicio, LocalDate fim, String especialidade, String regiao) {
        String from = triagem
            ? "FROM triagens tr JOIN pacientes p ON p.id=tr.paciente_id JOIN triagem_itens ti ON ti.triagem_id=tr.id JOIN procedimentos pr ON pr.id=ti.procedimento_id"
            : "FROM consultas c JOIN pacientes p ON p.id=c.paciente_id JOIN vagas v ON v.id=c.vaga_id JOIN turmas t ON t.id=c.turma_id JOIN procedimentos pr ON pr.id=c.procedimento_id";
        String date = triagem ? "tr.data_triagem::date" : "v.data";
        String semester = triagem ? "? IS NULL" : "(? IS NULL OR t.semestre_letivo=?)";
        String sql = "SELECT pr.nome categoria, COUNT(*) quantidade " + from + " WHERE " + semester
                + " AND (? IS NULL OR " + date + ">=?) AND (? IS NULL OR " + date + "<=?)"
                + " AND (? IS NULL OR pr.especialidade ILIKE ?) AND (?='TODOS' OR lower(trim(p.cidade))=lower(trim(?))) GROUP BY pr.nome ORDER BY COUNT(*) DESC, pr.nome";
        return jdbc.query(sql, ps -> {
            Object esp = especialidade == null ? null : "%" + especialidade + "%";
            ps.setObject(1, semestre); if (!triagem) ps.setObject(2, semestre);
            int b = triagem ? 2 : 3;
            ps.setObject(b++, inicio); ps.setObject(b++, inicio); ps.setObject(b++, fim); ps.setObject(b++, fim);
            ps.setObject(b++, esp); ps.setObject(b++, esp); ps.setObject(b++, regiao); ps.setObject(b, regiao);
        }, (rs, n) -> new Contagem(rs.getString("categoria"), rs.getLong("quantidade")));
    }

    public record EventoPaciente(long id, LocalDate nascimento, String bairro, String cidade, LocalDate dataEvento) {}
    public record Contagem(String categoria, long quantidade) {}
}
