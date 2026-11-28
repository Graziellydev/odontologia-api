package br.com.unicatolica.odontologia_api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// Define que esta classe é uma tabela do banco de dados
@Entity
// Especifica o nome exato da tabela no PostgreSQL
@Table(name = "pacientes")
// Gera automaticamente Getters, Setters, toString, equals e hashCode via Lombok
@Data
// Cria o construtor padrão sem parâmetros
@NoArgsConstructor
// Cria o construtor com todos os parâmetros
@AllArgsConstructor
public class Paciente {

    // Define este campo como a Chave Primária (Primary Key)
    @Id
    // Define que o ID é gerado automaticamente pelo banco (auto-incremento)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mapeia o número do prontuário (único e até 30 caracteres)
    @Column(name = "numero_prontuario", unique = true, length = 30)
    private String numeroProntuario;

    // Mapeia o nome completo (campo obrigatório, até 150 caracteres)
    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    // Mapeia o CPF (obrigatório, único e exatamente 11 caracteres)
    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    // Mapeia a data de nascimento (campo obrigatório)
    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    // Mapeia o telefone principal (obrigatório)
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

    // Mapeia o número da residência
    @Column(name = "numero_endereco", length = 20)
    private String numeroEndereco;

    // Mapeia o complemento do endereço
    @Column(length = 100)
    private String complemento;

    // Mapeia o bairro (obrigatório)
    @Column(nullable = false, length = 100)
    private String bairro;

    // Mapeia a cidade (obrigatória)
    @Column(nullable = false, length = 100)
    private String cidade;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "uf", length = 2)
    private String uf;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "sexo", length = 1)
    private String sexo;

    // Mapeia o código de acesso interno
    @Column(name = "codigo_acesso", unique = true, length = 20)
    private String codigoAcesso;

    // Indica se mora próximo a faculdade
    @Column(name = "mora_proximo", nullable = false)
    private Boolean moraProximo = false;

    // Indica se aceita chamadas de última hora
    @Column(name = "aceita_chamada_ultima_hora", nullable = false)
    private Boolean aceitaChamadaUltimaHora = false;

    // Mapeia a disponibilidade de horário
    @Column(name = "disponibilidade_horario", length = 100)
    private String disponibilidadeHorario;

    // Mapeia campo de texto longo para observações clínicas
    @Column(columnDefinition = "TEXT")
    private String observacao;

    // Mapeia ID de sistema externo
    @Column(name = "id_externo", length = 50)
    private String idExterno;

    // Mapeia a situação do cadastro ('ATIVO', 'DESLIGADO', 'EXCLUIDO')
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

    // Contador de faltas não justificadas
    @Column(name = "faltas_nao_justificadas", nullable = false)
    private Integer faltasNaoJustificadas = 0;

    // Mapeia o motivo do desligamento do programa
    @Column(name = "motivo_desligamento", columnDefinition = "TEXT")
    private String motivoDesligamento;

    // Mapeia a data e hora do desligamento
    @Column(name = "data_desligamento")
    private OffsetDateTime dataDesligamento;

    // Mapeia o ID do usuário que realizou o desligamento
    @Column(name = "desligado_por_id")
    private Long desligadoPorId;

    // Mapeia a data de criação (nunca pode ser alterada após criada)
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    // Mapeia a data da última atualização
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;

    // Executa automaticamente antes de salvar o registro pela primeira vez
    @PrePersist
    public void prePersist() {
        OffsetDateTime agora = OffsetDateTime.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    // Executa automaticamente antes de qualquer atualização do registro
    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
    }
}
