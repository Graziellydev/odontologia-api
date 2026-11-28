package br.com.unicatolica.odontologia_api;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Read-only Supabase audit; enable with SUPABASE_TEST_JDBC_URL and credentials. */
@SpringBootTest
@ActiveProfiles("supabase-test")
@EnabledIfEnvironmentVariable(named = "SUPABASE_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class SupabaseSchemaAuditIntegrationTest {

    @Autowired private DataSource dataSource;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void migrationsAteV14EstaoAplicadasEValidasSemAlterarOBanco() {
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .cleanDisabled(true)
                .load()
                .validate();

        Set<String> aplicadas = jdbc.query(
                "SELECT version FROM flyway_schema_history WHERE success = TRUE",
                (rs, rowNum) -> rs.getString("version"))
                .stream().collect(Collectors.toSet());
        for (int versao = 1; versao <= 14; versao++) {
            assertTrue(aplicadas.contains(Integer.toString(versao)), "Migration V" + versao + " ausente ou falhou");
        }

        List<String> triggers = jdbc.query(
                "SELECT tgname FROM pg_trigger WHERE NOT tgisinternal",
                (rs, rowNum) -> rs.getString("tgname"));
        assertTrue(triggers.contains("trg_consulta_historico"), "Trigger de histórico de consulta ausente");
        assertTrue(triggers.contains("trg_valida_solicitacao"), "Trigger de validação clínica ausente");
        assertTrue(triggers.contains("trg_prontuarios_imutavel"), "Trigger de imutabilidade do prontuário ausente");
        assertTrue(triggers.contains("trg_adendos_imutavel"), "Trigger de imutabilidade de adendos ausente");
    }

}
