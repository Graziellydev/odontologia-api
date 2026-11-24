package br.com.unicatolica.odontologia_api.notificacoes.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificacaoConclusaoService {

    private final JdbcTemplate jdbc;

    @Transactional
    public int registrarPesquisaSatisfacao(long consultaId) {
        return jdbc.update("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id, consulta_id, tipo_mensagem, conteudo_mensagem,
                     enviado_por_id, status_envio)
                SELECT c.paciente_id, c.id, 'PESQUISA_SATISFACAO',
                       replace(tm.conteudo, '{{nome_paciente}}', p.nome_completo),
                       NULL, 'PENDENTE'
                FROM consultas c
                JOIN pacientes p ON p.id = c.paciente_id
                JOIN templates_mensagem tm
                  ON tm.codigo = 'PESQUISA_SATISFACAO' AND tm.ativo = TRUE
                WHERE c.id = ? AND c.status_consulta = 'REALIZADO'
                  AND NOT EXISTS (
                      SELECT 1 FROM notificacoes_whatsapp n
                      WHERE n.consulta_id = c.id
                        AND n.tipo_mensagem = 'PESQUISA_SATISFACAO'
                  )
                ON CONFLICT DO NOTHING
                """, consultaId);
    }
}
