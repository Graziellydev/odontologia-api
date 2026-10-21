package br.com.unicatolica.odontologia_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// Define que esta classe e uma tabela do banco de dados
@Entity
// Especifica o nome exato da tabela no PostgreSQL
@Table(name = "pacientes")
// Gera automaticamente Getters, Setters, toString, equals e hashCode
@Data
// Cria o construtor padrao sem parametros
@NoArgsConstructor
// Cria o construtor com todos os parametros
@AllArgsConstructor
public class Paciente {

    // Define este campo como a Chave Primaria (Primary Key)
    @Id
    // Define que o ID e gerado automaticamente pelo banco (auto-incremento)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mapeia o numero do prontuario (unico e ate 30 caracteres)
    @Column(name = "numero_prontuario", unique = true, length = 30)
    private String numeroProntuario;

    // Mapeia o nome completo (campo obrigatorio, ate 150 caracteres)
    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    // Mapeia o CPF (obrigatorio, unico e exatamente 11 caracteres)
    @Column(nullable = false, unique = true, length = 11)
private String cpf;

    // Mapeia a data de nascimento (campo obrigatorio)
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    // Mapeia o telefone principal (obrigatorio)
    @Column(nullable = false, length = 20)
    private String telefone;

    // Mapeia o telefone alternativo (opcional)
    @Column(name = "telefone_alternativo", length = 20)
    private String telefoneAlternativo;

    // Mapeia o e-mail do paciente
    @Column(length = 150)
    private String email;

    // Mapeia o CEP
    @Column(length = 9)
    private String cep;

    // Mapeia o logradouro (Rua, Avenida, etc.)
    @Column(length = 150)
    private String logradouro;

    // Mapeia o numero da residencia
    @Column(name = "numero_endereco", length = 20)
    private String numeroEndereco;

    // Mapeia o complemento do endereco
    @Column(length = 100)
    private String complemento;

    // Mapeia o bairro (obrigatorio)
    @Column(nullable = false, length = 100)
    private String bairro;

    // Mapeia a cidade (obrigatoria)
    @Column(nullable = false, length = 100)
    private String cidade;

    // Mapeia a sigla do Estado (UF)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 2)
    private String uf;

    // Mapeia o codigo de acesso interno
    @Column(name = "codigo_acesso", unique = true, length = 20)
    private String codigoAcesso;

    // Indica se mora proximo a faculdade
    @Column(name = "mora_proximo", nullable = false)
    private Boolean moraProximo = false;

    // Indica se aceita chamadas de ultima hora
    @Column(name = "aceita_chamada_ultima_hora", nullable = false)
    private Boolean aceitaChamadaUltimaHora = false;

    // Mapeia a disponibilidade de horario
    @Column(name = "disponibilidade_horario", length = 100)
    private String disponibilidadeHorario;

    // Mapeia campo de texto longo para observacoes clinicas
    @Column(columnDefinition = "TEXT")
    private String observacao;

    // Mapeia ID de sistema externo
    @Column(name = "id_externo", length = 50)
    private String idExterno;

    // Mapeia a situacao do cadastro ('ATIVO', 'DESLIGADO', 'EXCLUIDO')
    @Column(nullable = false, length = 15)
    private String situacao = "ATIVO";

    // Indica se assinou o termo de compromisso
    @Column(name = "termo_compromisso_assinado", nullable = false)
    private Boolean termoCompromissoAssinado = false;

    // Mapeia a data e hora da assinatura do termo
    @Column(name = "data_termo_compromisso")
    private OffsetDateTime dataTermoCompromisso;

    // Contador de faltas justificadas
    @Column(name = "faltas_justificadas", nullable = false)
    private Integer faltasJustificadas = 0;

    // Contador de faltas nao justificadas
    @Column(name = "faltas_nao_justificadas", nullable = false)
    private Integer faltasNaoJustificadas = 0;

    // Mapeia o motivo do desligamento do programa
    @Column(name = "motivo_desligamento", columnDefinition = "TEXT")
    private String motivoDesligamento;

    // Mapeia a data e hora do desligamento
    @Column(name = "data_desligamento")
    private OffsetDateTime dataDesligamento;

    // Mapeia o ID do usuario que realizou o desligamento
    @Column(name = "desligado_por_id")
    private Long desligadoPorId;

    // Mapeia a data de criacao (nunca pode ser alterada apos criada)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Mapeia a data da ultima atualizacao
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    // Executa automaticamente antes de salvar o registro pela primeira vez
    @PrePersist
    public void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    // Executa automaticamente antes de qualquer atualizacao do registro
    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
    }
}
