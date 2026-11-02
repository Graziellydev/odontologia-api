package br.com.unicatolica.odontologia_api.retornos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "consultas")
@Getter
@Setter
@NoArgsConstructor
public class ConsultaRetorno {

    @Id
    private Long id;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "aluno_id")
    private Long alunoId;

    @Column(name = "procedimento_id")
    private Long procedimentoId;

    @Column(name = "dente")
    private String dente;

    @Column(name = "classe")
    private String classe;

    @Column(name = "fila_espera_id")
    private Long filaEsperaId;

    @Column(name = "status_consulta")
    private String statusConsulta;

    @Column(name = "retorno_sugerido")
    private Boolean retornoSugerido;

    @Column(name = "justificativa_retorno")
    private String justificativaRetorno;

    @Version
    @Column(name = "versao")
    private Long versao;
}