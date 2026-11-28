package br.com.unicatolica.odontologia_api.prontuarios.entity;

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
@Table(name = "prontuario_adendos")
@Getter
@Setter
@NoArgsConstructor
public class ProntuarioAdendo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "prontuario_id", nullable = false)
    private Long prontuarioId;

    @Column(name = "autor_id", nullable = false)
    private Long autorId;

    @Column(name = "texto", nullable = false, columnDefinition = "TEXT")
    private String texto;

    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;
}
