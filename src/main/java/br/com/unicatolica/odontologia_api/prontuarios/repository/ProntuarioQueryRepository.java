package br.com.unicatolica.odontologia_api.prontuarios.repository;

import br.com.unicatolica.odontologia_api.prontuarios.dto.ConsultaClinica;
import br.com.unicatolica.odontologia_api.prontuarios.dto.ProntuarioHistoricoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class ProntuarioQueryRepository {
    private final JdbcTemplate jdbc;

    public ConsultaClinica buscarConsulta(long consultaId) {
        List<ConsultaClinica> rows = jdbc.query("""
                SELECT c.id, c.paciente_id, c.aluno_id, c.turma_id, c.procedimento_id,
                       c.dente, c.classe, c.status_consulta AS status, v.data, v.turno,
                       p.nome_completo AS paciente, u.nome AS aluno, pr.nome AS procedimento,
                       ca.numero AS cadeira
                FROM consultas c
                JOIN vagas v ON v.id = c.vaga_id
                JOIN pacientes p ON p.id = c.paciente_id
                JOIN usuarios u ON u.id = c.aluno_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                JOIN cadeiras ca ON ca.id = v.cadeira_id
                WHERE c.id = ? AND v.data = CURRENT_DATE
                """, (rs, row) -> new ConsultaClinica(
                rs.getLong("id"), rs.getLong("paciente_id"), rs.getLong("aluno_id"),
                rs.getLong("turma_id"), nullableLong(rs, "procedimento_id"),
                rs.getString("dente"), rs.getString("classe"), rs.getString("status"),
                rs.getObject("data", LocalDate.class), rs.getString("turno"),
                rs.getString("paciente"), rs.getString("aluno"), rs.getString("procedimento"),
                rs.getString("cadeira")), consultaId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public ConsultaClinica buscarConsultaDoAluno(long pacienteId, long alunoId, boolean permitirRealizado) {
        List<ConsultaClinica> rows = jdbc.query("""
                SELECT c.id, c.paciente_id, c.aluno_id, c.turma_id, c.procedimento_id,
                       c.dente, c.classe, c.status_consulta AS status, v.data, v.turno,
                       p.nome_completo AS paciente, u.nome AS aluno, pr.nome AS procedimento,
                       ca.numero AS cadeira
                FROM consultas c
                JOIN vagas v ON v.id = c.vaga_id
                JOIN pacientes p ON p.id = c.paciente_id
                JOIN usuarios u ON u.id = c.aluno_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                JOIN cadeiras ca ON ca.id = v.cadeira_id
                WHERE c.paciente_id = ? AND c.aluno_id = ?
                  AND (c.status_consulta IN ('AGENDADO', 'CONFIRMADA_24H')
                       OR (? = TRUE AND c.status_consulta = 'REALIZADO'))
                  AND v.data = CURRENT_DATE
                ORDER BY v.turno, c.id
                LIMIT 1
                """, (rs, row) -> new ConsultaClinica(
                rs.getLong("id"), rs.getLong("paciente_id"), rs.getLong("aluno_id"),
                rs.getLong("turma_id"), nullableLong(rs, "procedimento_id"),
                rs.getString("dente"), rs.getString("classe"), rs.getString("status"),
                rs.getObject("data", LocalDate.class), rs.getString("turno"),
                rs.getString("paciente"), rs.getString("aluno"), rs.getString("procedimento"),
                rs.getString("cadeira")), pacienteId, alunoId, permitirRealizado);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public ConsultaClinica buscarConsultaDoPaciente(long pacienteId) {
        List<ConsultaClinica> rows = jdbc.query("""
                SELECT c.id, c.paciente_id, c.aluno_id, c.turma_id, c.procedimento_id,
                       c.dente, c.classe, c.status_consulta AS status, v.data, v.turno,
                       p.nome_completo AS paciente, u.nome AS aluno, pr.nome AS procedimento,
                       ca.numero AS cadeira
                FROM consultas c
                JOIN vagas v ON v.id = c.vaga_id
                JOIN pacientes p ON p.id = c.paciente_id
                JOIN usuarios u ON u.id = c.aluno_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                JOIN cadeiras ca ON ca.id = v.cadeira_id
                WHERE c.paciente_id = ?
                  AND c.status_consulta IN ('AGENDADO', 'CONFIRMADA_24H', 'REALIZADO')
                  AND v.data = CURRENT_DATE
                ORDER BY v.turno, c.id DESC
                LIMIT 1
                """, (rs, row) -> new ConsultaClinica(
                rs.getLong("id"), rs.getLong("paciente_id"), rs.getLong("aluno_id"),
                rs.getLong("turma_id"), nullableLong(rs, "procedimento_id"),
                rs.getString("dente"), rs.getString("classe"), rs.getString("status"),
                rs.getObject("data", LocalDate.class), rs.getString("turno"),
                rs.getString("paciente"), rs.getString("aluno"), rs.getString("procedimento"),
                rs.getString("cadeira")), pacienteId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public ProntuarioHistoricoResponse.PacienteResumo buscarPaciente(long pacienteId) {
        List<ProntuarioHistoricoResponse.PacienteResumo> rows = jdbc.query("""
                SELECT id, nome_completo, numero_prontuario,
                       EXTRACT(YEAR FROM age(CURRENT_DATE, data_nascimento))::int AS idade
                FROM pacientes WHERE id = ?
                """, (rs, row) -> new ProntuarioHistoricoResponse.PacienteResumo(
                rs.getLong("id"), rs.getString("nome_completo"), rs.getString("numero_prontuario"),
                rs.getInt("idade")), pacienteId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    @SuppressWarnings("unchecked")
    public ProntuarioHistoricoResponse.AnamneseResumo buscarAnamneseMaisRecente(long pacienteId) {
        List<ProntuarioHistoricoResponse.AnamneseResumo> rows = jdbc.query("""
                SELECT queixa_principal, historia_queixa, respostas_questionario::text AS respostas,
                       pressao_arterial, pulso_bpm, temperatura_c, frequencia_respiratoria,
                       medo_dentista_escala, frequencia_escovacao, usa_fio_dental,
                       usa_enxaguante, limpa_lingua
                FROM paciente_anamnese WHERE paciente_id = ?
                ORDER BY atualizado_em DESC, criado_em DESC LIMIT 1
                """, (rs, row) -> new ProntuarioHistoricoResponse.AnamneseResumo(
                rs.getString("queixa_principal"), rs.getString("historia_queixa"),
                parseJson(rs.getString("respostas")), rs.getString("pressao_arterial"),
                nullableInt(rs, "pulso_bpm"), rs.getBigDecimal("temperatura_c"),
                nullableInt(rs, "frequencia_respiratoria"), nullableInt(rs, "medo_dentista_escala"),
                nullableInt(rs, "frequencia_escovacao"), nullableBoolean(rs, "usa_fio_dental"),
                nullableBoolean(rs, "usa_enxaguante"), nullableBoolean(rs, "limpa_lingua")), pacienteId);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<ProntuarioHistoricoResponse.OdontogramaItem> listarOdontograma(long pacienteId) {
        return jdbc.query("""
                SELECT id, dente, face_dente, condicao_dente, observacao, atualizado_em
                FROM paciente_odontograma WHERE paciente_id = ?
                ORDER BY dente, face_dente NULLS FIRST
                """, (rs, row) -> new ProntuarioHistoricoResponse.OdontogramaItem(
                rs.getLong("id"), rs.getString("dente"), rs.getString("face_dente"),
                rs.getString("condicao_dente"), rs.getString("observacao"),
                rs.getObject("atualizado_em", OffsetDateTime.class)), pacienteId);
    }

    public List<ProntuarioHistoricoResponse.EvolucaoItem> listarEvolucoes(long pacienteId) {
        return jdbc.query("""
                SELECT pr.id, pr.consulta_id, pr.procedimento_id, proc.nome AS procedimento,
                       pr.dente, pr.classe, pr.evolucao_clinica, u.nome AS aluno,
                       pr.assinatura_digital, pr.data_registro
                FROM prontuarios pr
                JOIN procedimentos proc ON proc.id = pr.procedimento_id
                JOIN usuarios u ON u.id = pr.aluno_id
                WHERE pr.paciente_id = ?
                ORDER BY pr.data_registro DESC, pr.id DESC
                """, (rs, row) -> new ProntuarioHistoricoResponse.EvolucaoItem(
                rs.getLong("id"), rs.getLong("consulta_id"), rs.getLong("procedimento_id"),
                rs.getString("procedimento"), rs.getString("dente"), rs.getString("classe"),
                rs.getString("evolucao_clinica"), rs.getString("aluno"),
                rs.getString("assinatura_digital"), rs.getObject("data_registro", OffsetDateTime.class),
                listarAdendos(rs.getLong("id"))), pacienteId);
    }

    public int atualizarObservacao(long pacienteId, String dente, String face, String condicao, String observacao) {
        if (condicao == null || condicao.isBlank()) {
            return jdbc.update("""
                    UPDATE paciente_odontograma SET observacao = ?
                    WHERE paciente_id = ? AND dente = ? AND COALESCE(face_dente, 'GERAL') = COALESCE(?, 'GERAL')
                    """, observacao, pacienteId, dente, face);
        }
        return jdbc.update("""
                INSERT INTO paciente_odontograma (paciente_id, dente, face_dente, condicao_dente, observacao)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (paciente_id, dente, (COALESCE(face_dente, 'GERAL')))
                DO UPDATE SET observacao = EXCLUDED.observacao, condicao_dente = EXCLUDED.condicao_dente
                """, pacienteId, dente, face, condicao, observacao);
    }

    public boolean procedimentoPermitido(long turmaId, long procedimentoId, String dente, String classe) {
        Boolean permitido = jdbc.queryForObject("""
                SELECT fn_procedimento_permitido(t.disciplina_id, ?, ?, ?)
                FROM turmas t JOIN disciplinas d ON d.id = t.disciplina_id
                WHERE t.id = ?
                """, Boolean.class, procedimentoId, dente, classe, turmaId);
        return Boolean.TRUE.equals(permitido);
    }

    private List<ProntuarioHistoricoResponse.AdendoItem> listarAdendos(long prontuarioId) {
        return jdbc.query("""
                SELECT a.id, a.autor_id, u.nome AS autor, a.texto, a.criado_em
                FROM prontuario_adendos a JOIN usuarios u ON u.id = a.autor_id
                WHERE a.prontuario_id = ? ORDER BY a.criado_em, a.id
                """, (rs, row) -> new ProntuarioHistoricoResponse.AdendoItem(
                rs.getLong("id"), rs.getLong("autor_id"), rs.getString("autor"),
                rs.getString("texto"), rs.getObject("criado_em", OffsetDateTime.class)), prontuarioId);
    }

    private static Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer nullableInt(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Boolean nullableBoolean(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        boolean value = rs.getBoolean(column);
        return rs.wasNull() ? null : value;
    }

    private static Map<String, Object> parseJson(String json) {
        try {
            return new tools.jackson.databind.ObjectMapper().readValue(json,
                    new tools.jackson.core.type.TypeReference<>() { });
        } catch (Exception ignored) {
            return Map.of();
        }
    }
}
