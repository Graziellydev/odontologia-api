package br.com.unicatolica.odontologia_api.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Modalidade;
import br.com.unicatolica.odontologia_api.repository.DesempenhoAcademicoRepository.LinhaDesempenho;

class DesempenhoAcademicoRepositoryTest {

    private JdbcTemplate jdbc;
    private DesempenhoAcademicoRepository repository;

    @BeforeEach
    void prepararBanco() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        repository = new DesempenhoAcademicoRepository(jdbc);

        jdbc.execute("CREATE TABLE usuarios (id BIGINT PRIMARY KEY, nome VARCHAR(100), perfil VARCHAR(30), matricula_ou_registro VARCHAR(50))");
        jdbc.execute("CREATE TABLE disciplinas (id BIGINT PRIMARY KEY, nome VARCHAR(100))");
        jdbc.execute("CREATE TABLE turmas (id BIGINT PRIMARY KEY, disciplina_id BIGINT, codigo_turma VARCHAR(50), semestre_letivo VARCHAR(6))");
        jdbc.execute("CREATE TABLE matriculas_turma (turma_id BIGINT, aluno_id BIGINT)");
        jdbc.execute("CREATE TABLE disciplina_procedimentos (disciplina_id BIGINT, procedimento_id BIGINT, quantidade_exigida INT, ativo BOOLEAN)");
        jdbc.execute("CREATE TABLE procedimentos (id BIGINT PRIMARY KEY, nome VARCHAR(100))");
        jdbc.execute("CREATE TABLE consultas (id BIGINT PRIMARY KEY, status_consulta VARCHAR(25))");
        jdbc.execute("CREATE TABLE prontuarios (id BIGINT PRIMARY KEY, aluno_id BIGINT, turma_id BIGINT, procedimento_id BIGINT, consulta_id BIGINT UNIQUE)");

        jdbc.update("INSERT INTO usuarios VALUES (1, 'Ana', 'ALUNO', 'A001'), (2, 'Bia', 'ALUNO', 'A002'), (3, 'Caio', 'ALUNO', 'A003')");
        jdbc.update("INSERT INTO disciplinas VALUES (11, 'Dentística'), (12, 'Endodontia'), (13, 'Periodontia'), (14, 'Ortodontia')");
        jdbc.update("INSERT INTO turmas VALUES (10, 11, 'A', '2026.1'), (20, 12, 'A', '2026.1'), (30, 13, 'A', '2026.2'), (40, 14, 'B', '2026.1')");
        jdbc.update("INSERT INTO matriculas_turma VALUES (10, 1), (20, 1), (20, 2), (30, 3), (40, 3)");
        jdbc.update("INSERT INTO procedimentos VALUES (21, 'Restauração')");
        jdbc.update("INSERT INTO disciplina_procedimentos VALUES (11, 21, 2, TRUE), (12, 21, 1, TRUE), (13, 21, 5, TRUE), (14, 21, 4, TRUE)");
        jdbc.update("INSERT INTO consultas VALUES (101, 'REALIZADO'), (102, 'AGENDADO'), (103, 'CANCELADO_SECRETARIA'), (104, 'CANCELADO_PACIENTE'), (105, 'FALTOU_PACIENTE'), (106, 'FALTOU_ALUNO'), (107, 'REALIZADO')");
        jdbc.update("INSERT INTO prontuarios VALUES (1, 1, 10, 21, 101), (2, 1, 10, 21, 102), (3, 1, 10, 21, 103), (4, 1, 10, 21, 104), (5, 1, 10, 21, 105), (6, 1, 10, 21, 106), (7, 2, 20, 21, 107)");
    }

    @Test
    void filtraPeriodoCodigoEStatusEConsolidaMaisDeUmaDisciplina() {
        List<LinhaDesempenho> turma = repository.buscar(Modalidade.TURMA, "A", "2026.1");

        assertEquals(3, turma.size());
        assertTrue(turma.stream().allMatch(linha -> "2026.1".equals(linha.semestre())));
        assertEquals(2, linha(turma, 1L, 10L).quantidadeConcluida());
        assertEquals(0, linha(turma, 1L, 20L).quantidadeConcluida());
        assertEquals(1, linha(turma, 2L, 20L).quantidadeConcluida());
    }

    @Test
    void alunoIncluiMetasDeTodasAsTurmasNoPeriodoMasNaoDeOutroPeriodo() {
        List<LinhaDesempenho> aluno = repository.buscar(Modalidade.ALUNO, "A001", "2026.1");

        assertEquals(2, aluno.size());
        assertTrue(aluno.stream().allMatch(linha -> Long.valueOf(1).equals(linha.alunoId())));
        assertEquals(2, aluno.stream().mapToLong(LinhaDesempenho::quantidadeConcluida).sum());
    }

    private LinhaDesempenho linha(List<LinhaDesempenho> linhas, long alunoId, long turmaId) {
        return linhas.stream()
                .filter(item -> Long.valueOf(alunoId).equals(item.alunoId())
                        && Long.valueOf(turmaId).equals(item.turmaId()))
                .findFirst()
                .orElseThrow();
    }
}
