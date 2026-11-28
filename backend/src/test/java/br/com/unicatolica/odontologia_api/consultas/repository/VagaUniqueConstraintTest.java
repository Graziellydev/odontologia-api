package br.com.unicatolica.odontologia_api.consultas.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VagaUniqueConstraintTest {

    private EmbeddedDatabase database;
    private JdbcTemplate jdbc;

    @BeforeEach void criarDatabaseIsolado() {
        database = new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:sql/vaga-constraint-schema.sql")
                .build();
        jdbc = new JdbcTemplate(database);
        jdbc.update("INSERT INTO cadeiras (id, numero, ativa) VALUES (1, '01', TRUE)");
    }

    @AfterEach void encerrarDatabase() {
        database.shutdown();
    }

    @Test void constraintUkVagaImpedeDuplicarCadeiraDataETurno() {
        LocalDate data = LocalDate.parse("2026-12-01");
        jdbc.update("INSERT INTO vagas (cadeira_id, data, turno) VALUES (1, ?, 'MANHA')", data);

        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO vagas (cadeira_id, data, turno) VALUES (1, ?, 'MANHA')", data));

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM vagas", Integer.class));
    }
}
