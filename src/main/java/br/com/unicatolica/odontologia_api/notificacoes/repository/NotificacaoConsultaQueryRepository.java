package br.com.unicatolica.odontologia_api.notificacoes.repository;

import br.com.unicatolica.odontologia_api.notificacoes.dto.NotificacaoResumoDTO;
import br.com.unicatolica.odontologia_api.notificacoes.service.LinkWhatsapp;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class NotificacaoConsultaQueryRepository {
    private static final ZoneId ZONA_CLINICA = ZoneId.of("America/Fortaleza");
    private final JdbcTemplate jdbc;

    public List<NotificacaoResumoDTO> listar(LocalDate inicio, LocalDate fim, String tipo, String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT n.id, n.paciente_id, n.consulta_id, p.nome_completo, p.telefone,
                       n.tipo_mensagem, n.conteudo_mensagem, n.status_envio, n.criado_em,
                       n.resposta_recebida, n.respondido_em, v.data AS data_consulta, v.turno,
                       ca.numero AS cadeira, pr.nome AS procedimento, c.dente, c.classe
                FROM notificacoes_whatsapp n
                JOIN pacientes p ON p.id = n.paciente_id
                LEFT JOIN consultas c ON c.id = n.consulta_id
                LEFT JOIN vagas v ON v.id = c.vaga_id
                LEFT JOIN cadeiras ca ON ca.id = v.cadeira_id
                LEFT JOIN procedimentos pr ON pr.id = c.procedimento_id
                WHERE 1 = 1
                """);
        List<Object> parametros = new ArrayList<>();
        if (inicio != null) {
            sql.append(" AND n.criado_em >= ?");
            parametros.add(Timestamp.from(inicio.atStartOfDay(ZONA_CLINICA).toInstant()));
        }
        if (fim != null) {
            sql.append(" AND n.criado_em < ?");
            parametros.add(Timestamp.from(fim.plusDays(1).atStartOfDay(ZONA_CLINICA).toInstant()));
        }
        if (tipo != null && !tipo.isBlank()) {
            sql.append(" AND n.tipo_mensagem = ?");
            parametros.add(tipo.trim());
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND n.status_envio = ?");
            parametros.add(status.trim().toUpperCase());
        }
        sql.append(" ORDER BY n.criado_em DESC, n.id DESC LIMIT 200");
        return jdbc.query(sql.toString(), (rs, row) -> {
            String telefone = rs.getString("telefone");
            String conteudo = rs.getString("conteudo_mensagem");
            long consulta = rs.getLong("consulta_id");
            Long consultaId = rs.wasNull() ? null : consulta;
            Timestamp respondido = rs.getTimestamp("respondido_em");
            java.sql.Date dataConsulta = rs.getDate("data_consulta");
            Timestamp criada = rs.getTimestamp("criado_em");
            return new NotificacaoResumoDTO(
                    rs.getLong("id"), rs.getLong("paciente_id"), consultaId,
                    rs.getString("nome_completo"), telefone, rs.getString("tipo_mensagem"),
                    conteudo, rs.getString("status_envio"),
                    criada == null ? null : criada.toInstant().atZone(ZONA_CLINICA).toOffsetDateTime(),
                    LinkWhatsapp.criar(telefone, conteudo), rs.getString("resposta_recebida"),
                    respondido == null ? null : respondido.toInstant().atZone(ZONA_CLINICA).toOffsetDateTime(),
                    dataConsulta == null ? null : dataConsulta.toLocalDate(), rs.getString("turno"),
                    rs.getString("cadeira"), rs.getString("procedimento"),
                    rs.getString("dente"), rs.getString("classe"));
        }, parametros.toArray());
    }
}
