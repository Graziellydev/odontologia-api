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

import java.time.LocalDate;

/** Mapeia a vaga de agenda consumida por uma consulta. */
@Entity
@Table(name = "vagas")
@Getter
@Setter
@NoArgsConstructor
public class Vaga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cadeira_id", nullable = false)
    private Long cadeiraId;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "turno", nullable = false, length = 10)
    private String turno;

    @Column(name = "bloqueada", nullable = false)
    private Boolean bloqueada = false;

    @Column(name = "motivo_bloqueio", length = 200)
    private String motivoBloqueio;
}
