package br.com.unicatolica.odontologia_api.notificacoes.webhook;

import br.com.unicatolica.odontologia_api.notificacoes.service.LinkWhatsapp;
import br.com.unicatolica.odontologia_api.notificacoes.service.NotificacaoErrorLogger;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class WebhookWhatsappService {
    private static final Logger log = LoggerFactory.getLogger(WebhookWhatsappService.class);
    private static final Pattern CONFIRMACAO = Pattern.compile("^(?:1\\b|CONFIRMAR\\b|SIM\\b).*");
    private static final Pattern CANCELAMENTO = Pattern.compile("^(?:2\\b|CANCELAR\\b|NAO\\b).*");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JdbcTemplate jdbc;

    @Transactional
    public RespostaWebhookDTO processar(String telefone, String mensagem) {
        try {
            String canonico = LinkWhatsapp.telefoneCanonico(telefone);
            if (canonico == null) return new RespostaWebhookDTO("TELEFONE_INVALIDO");
            String resposta = normalizarMensagem(mensagem);
            boolean confirmar = CONFIRMACAO.matcher(resposta).matches();
            boolean cancelar = CANCELAMENTO.matcher(resposta).matches();
            if (!confirmar && !cancelar) return new RespostaWebhookDTO("RESPOSTA_NAO_RECONHECIDA");
            List<NotificacaoPendenteWebhook> pendentes = buscarPendentes(canonico);
            if (pendentes.isEmpty()) return new RespostaWebhookDTO("NAO_ENCONTRADA");
            if (pendentes.size() != 1) return new RespostaWebhookDTO("AMBIGUA");
            NotificacaoPendenteWebhook notificacao = pendentes.getFirst();
            if ("OFERTA_VAGA_DESISTENCIA".equals(notificacao.tipo())) {
                if (!confirmar) return new RespostaWebhookDTO("RESPOSTA_NAO_RECONHECIDA");
                return aceitarVaga(notificacao);
            }
            if (!"LEMBRETE_24H".equals(notificacao.tipo())) return new RespostaWebhookDTO("NAO_ENCONTRADA");
            return confirmar ? confirmarConsulta(notificacao) : cancelarConsulta(notificacao);
        } catch (RuntimeException falha) {
            NotificacaoErrorLogger.registrar(log, "whatsapp_webhook_processing_failed", falha);
            throw falha;
        }
    }

    private List<NotificacaoPendenteWebhook> buscarPendentes(String telefoneCanonico) {
        String semDdi = telefoneCanonico.substring(2);
        return jdbc.query("""
                SELECT n.id AS notificacao_id, n.paciente_id, n.consulta_id,
                       n.tipo_mensagem, p.telefone, c.status_consulta, c.fila_espera_id
                FROM notificacoes_whatsapp n
                JOIN pacientes p ON p.id = n.paciente_id
                JOIN consultas c ON c.id = n.consulta_id
                WHERE n.status_envio = 'PENDENTE'
                  AND n.tipo_mensagem IN ('LEMBRETE_24H', 'OFERTA_VAGA_DESISTENCIA')
                  AND (regexp_replace(p.telefone, '[^0-9]', '', 'g') = ?
                       OR regexp_replace(p.telefone, '[^0-9]', '', 'g') = ?)
                  AND ((n.tipo_mensagem = 'LEMBRETE_24H'
                        AND c.status_consulta IN ('AGENDADO', 'CONFIRMADA_24H'))
                    OR (n.tipo_mensagem = 'OFERTA_VAGA_DESISTENCIA'
                        AND c.status_consulta = 'CANCELADO_PACIENTE'))
                ORDER BY n.id
                """, (rs, row) -> {
            long fila = rs.getLong("fila_espera_id");
            Long filaId = rs.wasNull() ? null : fila;
            return new NotificacaoPendenteWebhook(rs.getLong("notificacao_id"),
                    rs.getLong("paciente_id"), rs.getLong("consulta_id"),
                    rs.getString("tipo_mensagem"), rs.getString("telefone"),
                    rs.getString("status_consulta"), filaId);
        }, telefoneCanonico, semDdi);
    }

    private RespostaWebhookDTO confirmarConsulta(NotificacaoPendenteWebhook n) {
        if (!"AGENDADO".equals(n.statusConsulta())) return new RespostaWebhookDTO("JA_PROCESSADA");
        definirOrigemAuditoria();
        int atualizadas = jdbc.update("""
                UPDATE consultas SET status_consulta='CONFIRMADA_24H', versao=versao+1
                WHERE id=? AND status_consulta='AGENDADO'
                """, n.consultaId());
        if (atualizadas != 1) return new RespostaWebhookDTO("JA_PROCESSADA");
        registrarResposta(n.notificacaoId(), "1");
        gravarMensagem(n.pacienteId(), n.consultaId(),
                "COMPROVANTE_AGENDAMENTO", "Presenca confirmada. Ate amanha!");
        return new RespostaWebhookDTO("CONFIRMADA");
    }

    private RespostaWebhookDTO cancelarConsulta(NotificacaoPendenteWebhook n) {
        if (!"AGENDADO".equals(n.statusConsulta()) && !"CONFIRMADA_24H".equals(n.statusConsulta()))
            return new RespostaWebhookDTO("JA_PROCESSADA");
        if (n.filaEsperaId() == null) return new RespostaWebhookDTO("NAO_ELEGIVEL");
        jdbc.queryForObject("SELECT id FROM fila_espera WHERE id=? FOR UPDATE", Long.class, n.filaEsperaId());
        definirOrigemAuditoria();
        int atualizadas = jdbc.update("""
                UPDATE consultas SET status_consulta='CANCELADO_PACIENTE', versao=versao+1
                WHERE id=? AND status_consulta IN ('AGENDADO','CONFIRMADA_24H')
                """, n.consultaId());
        if (atualizadas != 1) return new RespostaWebhookDTO("JA_PROCESSADA");
        boolean outraAguardando = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM fila_espera atual JOIN fila_espera outra
                  ON outra.paciente_id=atual.paciente_id
                 AND outra.procedimento_id=atual.procedimento_id
                 AND COALESCE(outra.dente,'')=COALESCE(atual.dente,'')
                 AND COALESCE(outra.classe,'')=COALESCE(atual.classe,'')
                 AND outra.status_fila='AGUARDANDO'
                 WHERE atual.id=? AND outra.id<>atual.id)
                """, Boolean.class, n.filaEsperaId()));
        jdbc.update("UPDATE fila_espera SET status_fila=?, data_saida_fila=? WHERE id=?",
                outraAguardando ? "INATIVO" : "AGUARDANDO",
                outraAguardando ? java.time.OffsetDateTime.now() : null, n.filaEsperaId());
        registrarResposta(n.notificacaoId(), "2");
        gravarAvisoCancelamento(n.consultaId(), n.pacienteId());
        notificarChamadaRapida(n.consultaId());
        return new RespostaWebhookDTO("CANCELADA");
    }

    private RespostaWebhookDTO aceitarVaga(NotificacaoPendenteWebhook n) {
        Map<String,Object> c = jdbc.queryForMap("""
                SELECT c.id,c.aluno_id,c.turma_id,c.procedimento_id,c.dente,c.classe,c.vaga_id,
                       c.status_consulta,v.data AS data_vaga,v.turno,v.bloqueada
                FROM consultas c JOIN vagas v ON v.id=c.vaga_id WHERE c.id=? FOR UPDATE OF c
                """, n.consultaId());
        if (!"CANCELADO_PACIENTE".equals(c.get("status_consulta")))
            return indisponivel(n, "A vaga nao esta mais disponivel.");
        long vagaId=((Number)c.get("vaga_id")).longValue();
        Map<String,Object> v=jdbc.queryForMap("SELECT id,bloqueada FROM vagas WHERE id=? FOR UPDATE",vagaId);
        if (Boolean.TRUE.equals(v.get("bloqueada")) || Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM consultas WHERE vaga_id=?
                AND status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'))
                """,Boolean.class,vagaId)))
            return indisponivel(n,"A vaga foi preenchida por outra demanda.");
        Long filaId=selecionarDemanda(n.pacienteId(),c);
        if(filaId==null) return indisponivel(n,"A demanda nao e mais elegivel para esta vaga.");
        definirOrigemAuditoria();
        Number novaId=jdbc.queryForObject("""
                INSERT INTO consultas (paciente_id,aluno_id,turma_id,procedimento_id,dente,classe,
                    vaga_id,fila_espera_id,criado_por_id,status_consulta)
                SELECT ?,c.aluno_id,c.turma_id,c.procedimento_id,c.dente,c.classe,c.vaga_id,?,NULL,'AGENDADO'
                FROM consultas c WHERE c.id=? RETURNING id
                """,Number.class,n.pacienteId(),filaId,n.consultaId());
        jdbc.update("UPDATE fila_espera SET status_fila='ALOCADO',data_saida_fila=now() WHERE id=?",filaId);
        registrarResposta(n.notificacaoId(),"1");
        gravarMensagem(n.pacienteId(),novaId.longValue(),"COMPROVANTE_AGENDAMENTO",
                "Sua vaga foi confirmada. A Secretaria acompanhara o agendamento.");
        jdbc.update("""
                UPDATE notificacoes_whatsapp SET conteudo_mensagem =
                  'Esta vaga foi preenchida por outra demanda. A Secretaria avisara se surgir nova oportunidade.'
                WHERE consulta_id=? AND tipo_mensagem='OFERTA_VAGA_DESISTENCIA'
                  AND id<>? AND status_envio='PENDENTE'
                """,n.consultaId(),n.notificacaoId());
        return new RespostaWebhookDTO("VAGA_CONFIRMADA");
    }

    private Long selecionarDemanda(Long pacienteId, Map<String,Object> c) {
        Number procedimento=(Number)c.get("procedimento_id");
        if(procedimento==null) return null;
        List<Long> filas=jdbc.query("""
                SELECT f.id FROM fila_espera f
                JOIN pacientes p ON p.id=f.paciente_id AND p.situacao='ATIVO'
                JOIN procedimentos pr ON pr.id=f.procedimento_id AND pr.ativo
                JOIN turmas t ON t.id=? AND t.ativa
                JOIN disciplinas d ON d.id=t.disciplina_id AND d.ativo
                JOIN vagas v ON v.id=?
                WHERE f.paciente_id=? AND f.status_fila='AGUARDANDO'
                  AND f.procedimento_id=? AND f.dente IS NOT DISTINCT FROM ?
                  AND f.classe IS NOT DISTINCT FROM ?
                  AND fn_procedimento_permitido(d.id,f.procedimento_id,f.dente,f.classe)
                  AND NOT EXISTS (SELECT 1 FROM consultas a WHERE a.fila_espera_id=f.id
                    AND a.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'))
                  AND NOT EXISTS (SELECT 1 FROM consultas cp JOIN vagas vp ON vp.id=cp.vaga_id
                    WHERE cp.paciente_id=f.paciente_id AND vp.data=v.data AND vp.turno=v.turno
                    AND cp.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'))
                  AND NOT EXISTS (SELECT 1 FROM consultas ca JOIN vagas va ON va.id=ca.vaga_id
                    WHERE ca.aluno_id=? AND va.data=v.data AND va.turno=v.turno
                    AND ca.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'))
                ORDER BY CASE f.nivel_urgencia WHEN 'ALTA' THEN 1 WHEN 'PNE' THEN 2
                         WHEN 'MEDIA' THEN 3 ELSE 4 END,
                         f.data_entrada_fila ASC,f.prioridade_reagendamento DESC
                LIMIT 1 FOR UPDATE OF f SKIP LOCKED
                """,(rs,row)->rs.getLong(1),c.get("turma_id"),c.get("vaga_id"),pacienteId,
                procedimento.longValue(),c.get("dente"),c.get("classe"),c.get("aluno_id"));
        return filas.isEmpty()?null:filas.getFirst();
    }

    private RespostaWebhookDTO indisponivel(NotificacaoPendenteWebhook n,String mensagem) {
        registrarResposta(n.notificacaoId(),"1");
        jdbc.update("UPDATE notificacoes_whatsapp SET conteudo_mensagem=? WHERE id=?",mensagem,n.notificacaoId());
        return new RespostaWebhookDTO("VAGA_INDISPONIVEL");
    }

    private void notificarChamadaRapida(long consultaId) {
        jdbc.update("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,enviado_por_id,status_envio)
                SELECT f.paciente_id,c.id,'OFERTA_VAGA_DESISTENCIA',
                  replace(replace(replace(tm.conteudo,'{{nome_paciente}}',p.nome_completo),
                    '{{data}}',to_char(v.data,'DD/MM/YYYY')),
                    '{{turno}}',CASE v.turno WHEN 'MANHA' THEN 'manha' ELSE 'tarde' END),
                  NULL,'PENDENTE'
                FROM consultas c JOIN vagas v ON v.id=c.vaga_id
                JOIN turmas t ON t.id=c.turma_id JOIN disciplinas d ON d.id=t.disciplina_id AND d.ativo
                JOIN fila_espera f ON f.status_fila='AGUARDANDO'
                  AND f.procedimento_id=c.procedimento_id AND f.dente IS NOT DISTINCT FROM c.dente
                  AND f.classe IS NOT DISTINCT FROM c.classe
                JOIN pacientes p ON p.id=f.paciente_id AND p.situacao='ATIVO'
                JOIN procedimentos pr ON pr.id=f.procedimento_id AND pr.ativo
                JOIN templates_mensagem tm ON tm.codigo='OFERTA_VAGA_DESISTENCIA' AND tm.ativo=TRUE
                WHERE c.id=? AND c.status_consulta='CANCELADO_PACIENTE'
                  AND f.id IS DISTINCT FROM c.fila_espera_id
                  AND fn_procedimento_permitido(d.id,f.procedimento_id,f.dente,f.classe)
                  AND NOT EXISTS (SELECT 1 FROM consultas a WHERE a.fila_espera_id=f.id
                    AND a.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'))
                  AND NOT EXISTS (SELECT 1 FROM consultas cp JOIN vagas vp ON vp.id=cp.vaga_id
                    WHERE cp.paciente_id=f.paciente_id AND vp.data=v.data AND vp.turno=v.turno
                    AND cp.status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE'))
                ORDER BY CASE f.nivel_urgencia WHEN 'ALTA' THEN 1 WHEN 'PNE' THEN 2
                         WHEN 'MEDIA' THEN 3 ELSE 4 END,
                         f.data_entrada_fila ASC,f.prioridade_reagendamento DESC
                ON CONFLICT DO NOTHING
                """,consultaId);
    }

    private void gravarAvisoCancelamento(long consultaId,Long pacienteId) {
        Map<String,Object> d=jdbc.queryForMap("""
                SELECT p.nome_completo,v.data,v.turno,tm.conteudo
                FROM consultas c JOIN pacientes p ON p.id=c.paciente_id
                JOIN vagas v ON v.id=c.vaga_id
                LEFT JOIN templates_mensagem tm ON tm.codigo='AVISO_CANCELAMENTO' AND tm.ativo=TRUE
                WHERE c.id=?
                """,consultaId);
        String template=(String)d.get("conteudo");
        if(template==null) template="Ola, {{nome_paciente}}! Sua consulta de {{data}} foi cancelada. Voce permanece na fila de espera.";
        String turno="MANHA".equals(d.get("turno"))?"manha":"tarde";
        String mensagem=template.replace("{{nome_paciente}}",(String)d.get("nome_completo"))
                .replace("{{data}}",DATA_BR.format(((java.sql.Date)d.get("data")).toLocalDate()))
                .replace("{{turno}}",turno);
        jdbc.update("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,enviado_por_id,status_envio)
                VALUES (?,?,'AVISO_CANCELAMENTO',?,NULL,'PENDENTE')
                """,pacienteId,consultaId,mensagem);
    }

    private void gravarMensagem(Long pacienteId,Long consultaId,String tipo,String conteudo) {
        jdbc.update("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,enviado_por_id,status_envio)
                VALUES (?,?,?, ?,NULL,'PENDENTE')
                """,pacienteId,consultaId,tipo,conteudo);
    }

    private void registrarResposta(Long id,String resposta) {
        jdbc.update("""
                UPDATE notificacoes_whatsapp SET resposta_recebida=?,respondido_em=now(),
                    status_envio='CONFIRMADO_RESPOSTA'
                WHERE id=? AND status_envio='PENDENTE'
                """,resposta,id);
    }

    private void definirOrigemAuditoria() {
        jdbc.queryForObject("SELECT set_config('app.usuario_id','',true)",String.class);
        jdbc.queryForObject("SELECT set_config('app.origem','PACIENTE_WHATSAPP',true)",String.class);
    }

    private static String normalizarMensagem(String mensagem) {
        String texto=Normalizer.normalize(mensagem==null?"":mensagem,Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","");
        return texto.trim().toUpperCase(Locale.ROOT);
    }
}

