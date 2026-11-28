package br.com.unicatolica.odontologia_api.consultas.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "notificacoes_whatsapp")
@Getter
@Setter
@NoArgsConstructor
public class NotificacaoWhatsapp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Column(name = "consulta_id")
    private Long consultaId;

    @Column(name = "tipo_mensagem", nullable = false, length = 50)
    private String tipoMensagem;

    @Column(name = "conteudo_mensagem", nullable = false, columnDefinition = "TEXT")
    private String conteudoMensagem;

    @Column(name = "enviado_por_id")
    private Long enviadoPorId;

    @Column(name = "status_envio", nullable = false, length = 25)
    private String statusEnvio = "PENDENTE";

    @Column(name = "criado_em", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime criadoEm;
}
