package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.AlunoAgendamentoDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.DisponibilidadeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.FilaAgendamentoDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.NotificacaoConsultaDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AgendaConsultaRepository {

    private final JdbcTemplate jdbc;

    public List<FilaAgendamentoDTO> listarFilaDisponivel() {
        return jdbc.query("""
                SELECT f.id, f.paciente_id, p.nome_completo, f.procedimento_id,
                       pr.nome AS procedimento, pr.especialidade, f.dente, f.classe,
                       f.status_fila, f.origem
                FROM fila_espera f
                JOIN pacientes p ON p.id = f.paciente_id AND p.situacao = 'ATIVO'
                JOIN procedimentos pr ON pr.id = f.procedimento_id AND pr.ativo
                WHERE f.status_fila IN ('AGUARDANDO', 'ALOCADO')
                  AND NOT EXISTS (
                      SELECT 1 FROM consultas c
                      WHERE c.fila_espera_id = f.id
                        AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                  )
                ORDER BY CASE f.nivel_urgencia
                           WHEN 'ALTA' THEN 1 WHEN 'PNE' THEN 2
                           WHEN 'MEDIA' THEN 3 ELSE 4 END,
                         f.prioridade_reagendamento DESC, f.data_entrada_fila
                """, (rs, row) -> new FilaAgendamentoDTO(
                rs.getLong("id"), rs.getLong("paciente_id"), rs.getString("nome_completo"),
                rs.getLong("procedimento_id"), rs.getString("procedimento"),
                rs.getString("especialidade"), rs.getString("dente"), rs.getString("classe"),
                rs.getString("status_fila"), rs.getString("origem")));
    }

    public List<AlunoAgendamentoDTO> listarAlunosElegiveis(
            long filaId, LocalDate data, String turno) {
        return jdbc.query("""
                SELECT DISTINCT u.id AS aluno_id, u.nome AS aluno,
                       t.id AS turma_id, t.codigo_turma AS turma
                FROM fila_espera f
                JOIN matriculas_turma m ON m.aluno_id IS NOT NULL
                JOIN usuarios u ON u.id = m.aluno_id AND u.perfil = 'ALUNO' AND u.ativo
                JOIN turmas t ON t.id = m.turma_id AND t.ativa
                JOIN disciplinas d ON d.id = t.disciplina_id AND d.ativo
                WHERE f.id = ?
                  AND fn_procedimento_permitido(d.id, f.procedimento_id, f.dente, f.classe)
                  AND (NOT EXISTS (
                         SELECT 1 FROM solicitacoes_pacientes sp
                         WHERE sp.fila_espera_id = f.id
                           AND sp.status_solicitacao = 'APROVADO_E_ALOCADO'
                       ) OR EXISTS (
                         SELECT 1 FROM solicitacoes_pacientes sp
                         WHERE sp.fila_espera_id = f.id
                           AND sp.status_solicitacao = 'APROVADO_E_ALOCADO'
                           AND sp.aluno_id = u.id AND sp.turma_id = t.id
                       ))
                  AND NOT EXISTS (
                      SELECT 1 FROM consultas c JOIN vagas v ON v.id = c.vaga_id
                      WHERE c.aluno_id = u.id AND v.data = ? AND v.turno = ?
                        AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM consultas c JOIN vagas v ON v.id = c.vaga_id
                      WHERE c.paciente_id = f.paciente_id AND v.data = ? AND v.turno = ?
                        AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                  )
                ORDER BY u.nome, t.codigo_turma
                """, (rs, row) -> new AlunoAgendamentoDTO(
                rs.getLong("aluno_id"), rs.getString("aluno"),
                rs.getLong("turma_id"), rs.getString("turma")),
                filaId, data, turno, data, turno);
    }

    public List<DisponibilidadeDTO> listarDisponibilidade(
            LocalDate inicio, LocalDate fim, Long filaId, Long alunoId, Long consultaIgnorada) {
        return jdbc.query("""
                SELECT v.data, v.turno, COUNT(*) AS vagas_disponiveis
                FROM v_vagas_disponiveis v
                WHERE v.data BETWEEN ? AND ?
                  AND (CAST(? AS BIGINT) IS NULL OR NOT EXISTS (
                      SELECT 1 FROM fila_espera f
                      JOIN consultas c ON c.paciente_id = f.paciente_id
                      JOIN vagas ocupada ON ocupada.id = c.vaga_id
                      WHERE f.id = ? AND ocupada.data = v.data AND ocupada.turno = v.turno
                        AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                        AND (CAST(? AS BIGINT) IS NULL OR c.id <> ?)
                  ))
                  AND (CAST(? AS BIGINT) IS NULL OR NOT EXISTS (
                      SELECT 1 FROM consultas c
                      JOIN vagas ocupada ON ocupada.id = c.vaga_id
                      WHERE c.aluno_id = ? AND ocupada.data = v.data AND ocupada.turno = v.turno
                        AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                        AND (CAST(? AS BIGINT) IS NULL OR c.id <> ?)
                  ))
                GROUP BY v.data, v.turno
                ORDER BY v.data, v.turno
                """, (rs, row) -> new DisponibilidadeDTO(
                rs.getObject("data", LocalDate.class), rs.getString("turno"),
                rs.getInt("vagas_disponiveis")),
                inicio, fim, filaId, filaId, consultaIgnorada, consultaIgnorada,
                alunoId, alunoId, consultaIgnorada, consultaIgnorada);
    }

    public List<AgendamentoResponseDTO> listarAgendamentos(
            LocalDate data, Long alunoId, String status, String busca) {
        StringBuilder sql = new StringBuilder("""
                SELECT c.id, c.paciente_id, p.nome_completo AS paciente,
                       c.aluno_id, u.nome AS aluno, c.turma_id, c.fila_espera_id,
                       c.procedimento_id, pr.nome AS procedimento, pr.especialidade,
                       c.dente, c.classe, c.vaga_id, ca.numero AS cadeira,
                       v.data, v.turno, c.status_consulta AS status, c.criado_em,
                       c.consulta_origem_id, NULL::BIGINT AS notificacao_id
                FROM consultas c
                JOIN pacientes p ON p.id = c.paciente_id
                JOIN usuarios u ON u.id = c.aluno_id
                JOIN vagas v ON v.id = c.vaga_id
                JOIN cadeiras ca ON ca.id = v.cadeira_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                WHERE 1 = 1
                """);
        List<Object> parametros = new ArrayList<>();

        if (data != null) {
            sql.append(" AND v.data = ?");
            parametros.add(java.sql.Date.valueOf(data));
        }
        if (alunoId != null) {
            sql.append(" AND c.aluno_id = ?");
            parametros.add(alunoId);
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND c.status_consulta = ?");
            parametros.add(status.trim());
        }
        if (busca != null && !busca.isBlank()) {
            sql.append(" AND LOWER(p.nome_completo) LIKE LOWER(?)");
            parametros.add("%" + busca.trim() + "%");
        }

        sql.append(" ORDER BY v.data DESC, v.turno, p.nome_completo, c.id DESC");
        return jdbc.query(sql.toString(), (rs, row) -> resposta(rs, null), parametros.toArray());
    }

    public AgendamentoResponseDTO buscarAgendamento(long consultaId) {
        return buscarAgendamento(consultaId, null);
    }

    public AgendamentoResponseDTO buscarAgendamento(long consultaId, Long notificacaoId) {
        return jdbc.query("""
                SELECT c.id, c.paciente_id, p.nome_completo AS paciente,
                       c.aluno_id, u.nome AS aluno, c.turma_id, c.fila_espera_id,
                       c.procedimento_id, pr.nome AS procedimento, pr.especialidade,
                       c.dente, c.classe, c.vaga_id, ca.numero AS cadeira,
                       v.data, v.turno, c.status_consulta AS status, c.criado_em,
                       c.consulta_origem_id, NULL::BIGINT AS notificacao_id
                FROM consultas c
                JOIN pacientes p ON p.id = c.paciente_id
                JOIN usuarios u ON u.id = c.aluno_id
                JOIN vagas v ON v.id = c.vaga_id
                JOIN cadeiras ca ON ca.id = v.cadeira_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                WHERE c.id = ?
                """, rs -> rs.next() ? resposta(rs, notificacaoId) : null, consultaId);
    }

    public List<NotificacaoConsultaDTO> listarNotificacoes(Long consultaId) {
        return jdbc.query("""
                SELECT n.id, n.paciente_id, n.consulta_id, n.tipo_mensagem, n.conteudo_mensagem,
                       n.status_envio, n.criado_em, n.resposta_recebida, n.respondido_em, p.telefone
                FROM notificacoes_whatsapp n
                JOIN pacientes p ON p.id = n.paciente_id
                WHERE (CAST(? AS BIGINT) IS NULL OR n.consulta_id = ?
                   OR n.consulta_id IN (SELECT id FROM consultas WHERE consulta_origem_id = ?))
                ORDER BY n.criado_em DESC, n.id DESC
                LIMIT 200
                """, (rs, row) -> new NotificacaoConsultaDTO(
                rs.getLong("id"), rs.getLong("paciente_id"), rs.getLong("consulta_id"),
                rs.getString("tipo_mensagem"), rs.getString("conteudo_mensagem"),
                rs.getString("status_envio"), offset(rs, "criado_em"), true,
                rs.getString("telefone"),
                br.com.unicatolica.odontologia_api.notificacoes.service.LinkWhatsapp.criar(
                        rs.getString("telefone"), rs.getString("conteudo_mensagem")),
                rs.getString("resposta_recebida"), offset(rs, "respondido_em")),
                consultaId, consultaId, consultaId);
    }

    public String buscarTemplate(String codigo) {
        List<String> conteudos = jdbc.query("""
                SELECT conteudo FROM templates_mensagem WHERE codigo = ? AND ativo = TRUE
                """, (rs, row) -> rs.getString("conteudo"), codigo);
        return conteudos.isEmpty() ? null : conteudos.get(0);
    }

    public boolean existeConflitoDeAluno(long alunoId, LocalDate data, String turno, Long consultaIgnorada) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM consultas c JOIN vagas v ON v.id = c.vaga_id
                    WHERE c.aluno_id = ? AND v.data = ? AND v.turno = ?
                      AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                      AND (CAST(? AS BIGINT) IS NULL OR c.id <> ?)
                )
                """, Boolean.class, alunoId, data, turno, consultaIgnorada, consultaIgnorada);
        return Boolean.TRUE.equals(existe);
    }

    public boolean existeConflitoDePaciente(long pacienteId, LocalDate data, String turno, Long consultaIgnorada) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM consultas c JOIN vagas v ON v.id = c.vaga_id
                    WHERE c.paciente_id = ? AND v.data = ? AND v.turno = ?
                      AND c.status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                      AND (CAST(? AS BIGINT) IS NULL OR c.id <> ?)
                )
                """, Boolean.class, pacienteId, data, turno, consultaIgnorada, consultaIgnorada);
        return Boolean.TRUE.equals(existe);
    }

    public boolean existeConsultaAtivaNaFila(long filaId, Long consultaIgnorada) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM consultas
                    WHERE fila_espera_id = ?
                      AND status_consulta NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')
                      AND (CAST(? AS BIGINT) IS NULL OR id <> ?)
                )
                """, Boolean.class, filaId, consultaIgnorada, consultaIgnorada);
        return Boolean.TRUE.equals(existe);
    }

    public boolean alunoPodeAtender(long filaId, long alunoId, long turmaId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM fila_espera f
                    JOIN matriculas_turma m ON m.aluno_id = ? AND m.turma_id = ?
                    JOIN usuarios u ON u.id = m.aluno_id AND u.perfil = 'ALUNO' AND u.ativo
                    JOIN turmas t ON t.id = m.turma_id AND t.ativa
                    JOIN disciplinas d ON d.id = t.disciplina_id AND d.ativo
                    WHERE f.id = ?
                      AND fn_procedimento_permitido(d.id, f.procedimento_id, f.dente, f.classe)
                      AND (NOT EXISTS (
                             SELECT 1 FROM solicitacoes_pacientes sp
                             WHERE sp.fila_espera_id = f.id
                               AND sp.status_solicitacao = 'APROVADO_E_ALOCADO'
                           ) OR EXISTS (
                             SELECT 1 FROM solicitacoes_pacientes sp
                             WHERE sp.fila_espera_id = f.id
                               AND sp.status_solicitacao = 'APROVADO_E_ALOCADO'
                               AND sp.aluno_id = ? AND sp.turma_id = ?
                           ))
                )
                """, Boolean.class, alunoId, turmaId, filaId, alunoId, turmaId);
        return Boolean.TRUE.equals(existe);
    }

    public boolean existeOutraEntradaAguardando(long filaId) {
        Boolean existe = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM fila_espera atual
                    JOIN fila_espera outra ON outra.paciente_id = atual.paciente_id
                      AND outra.procedimento_id = atual.procedimento_id
                      AND COALESCE(outra.dente, '') = COALESCE(atual.dente, '')
                      AND COALESCE(outra.classe, '') = COALESCE(atual.classe, '')
                      AND outra.status_fila = 'AGUARDANDO'
                    WHERE atual.id = ? AND outra.id <> atual.id
                )
                """, Boolean.class, filaId);
        return Boolean.TRUE.equals(existe);
    }

    private static AgendamentoResponseDTO resposta(ResultSet rs, Long notificacaoId) throws SQLException {
        long procedimento = rs.getLong("procedimento_id");
        Long procedimentoId = rs.wasNull() ? null : procedimento;
        long fila = rs.getLong("fila_espera_id");
        Long filaId = rs.wasNull() ? null : fila;
        long origem = rs.getLong("consulta_origem_id");
        Long origemId = rs.wasNull() ? null : origem;
        return new AgendamentoResponseDTO(
                rs.getLong("id"), rs.getLong("paciente_id"), rs.getString("paciente"),
                rs.getLong("aluno_id"), rs.getString("aluno"), rs.getLong("turma_id"),
                filaId, procedimentoId, rs.getString("procedimento"), rs.getString("especialidade"),
                rs.getString("dente"), rs.getString("classe"), rs.getLong("vaga_id"),
                rs.getString("cadeira"), rs.getObject("data", LocalDate.class),
                rs.getString("turno"), rs.getString("status"), offset(rs, "criado_em"),
                origemId, notificacaoId, notificacaoId != null);
    }

    private static OffsetDateTime offset(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant().atOffset(ZoneOffset.UTC);
    }
}
