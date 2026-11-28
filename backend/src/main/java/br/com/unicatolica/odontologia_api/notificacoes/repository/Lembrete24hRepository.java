package br.com.unicatolica.odontologia_api.notificacoes.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class Lembrete24hRepository {

    private final JdbcTemplate jdbc;

    public List<CandidatoLembrete24h> listarCandidatos(LocalDate dataConsulta) {
        return jdbc.query("""
                SELECT c.id AS consulta_id, c.paciente_id, p.nome_completo,
                       v.data AS data_consulta, v.turno, tm.conteudo AS template
                FROM consultas c
                JOIN vagas v ON v.id = c.vaga_id
                JOIN pacientes p ON p.id = c.paciente_id
                LEFT JOIN templates_mensagem tm
                       ON tm.codigo = 'LEMBRETE_24H' AND tm.ativo = TRUE
                WHERE v.data = ?
                  AND c.status_consulta = 'AGENDADO'
                  AND NOT EXISTS (
                      SELECT 1 FROM notificacoes_whatsapp n
                      WHERE n.consulta_id = c.id
                        AND n.tipo_mensagem = 'LEMBRETE_24H'
                  )
                ORDER BY c.id
                """, (rs, row) -> new CandidatoLembrete24h(
                rs.getLong("consulta_id"),
                rs.getLong("paciente_id"),
                rs.getString("nome_completo"),
                rs.getObject("data_consulta", LocalDate.class),
                rs.getString("turno"),
                rs.getString("template")), dataConsulta);
    }

    public int inserirSeAusente(CandidatoLembrete24h candidato, String conteudo) {
        return jdbc.update("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id, consulta_id, tipo_mensagem, conteudo_mensagem,
                     enviado_por_id, status_envio)
                VALUES (?, ?, 'LEMBRETE_24H', ?, NULL, 'PENDENTE')
                ON CONFLICT DO NOTHING
                """, candidato.pacienteId(), candidato.consultaId(), conteudo);
    }
}
