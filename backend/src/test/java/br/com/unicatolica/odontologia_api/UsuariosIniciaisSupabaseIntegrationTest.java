package br.com.unicatolica.odontologia_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/** Valida a carga inicial, as senhas e a reaplicação idempotente no Supabase de teste. */
@SpringBootTest
@ActiveProfiles("supabase-test")
@EnabledIfEnvironmentVariable(named = "SUPABASE_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
class UsuariosIniciaisSupabaseIntegrationTest {

    private static final List<ContaEsperada> CONTAS = List.of(
            new ContaEsperada("Ana Paula Martins", "secretaria1@unicatolica.edu.br", "SEC202601", "SECRETARIA", "Sec@2026"),
            new ContaEsperada("Beatriz Costa Lima", "secretaria2@unicatolica.edu.br", "SEC202602", "SECRETARIA", "Sec@2026"),
            new ContaEsperada("Mariana Costa Silva", "mariana.costa@aluno.unicatolica.edu.br", "20241001", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Lucas Mendes Oliveira", "lucas.mendes@aluno.unicatolica.edu.br", "20241002", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Gabriel Santos Rocha", "gabriel.santos@aluno.unicatolica.edu.br", "20241003", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Camila Ferreira Alves", "camila.ferreira@aluno.unicatolica.edu.br", "20241004", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Pedro Henrique Sousa", "pedro.sousa@aluno.unicatolica.edu.br", "20241005", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Larissa Almeida Nogueira", "larissa.almeida@aluno.unicatolica.edu.br", "20241006", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Rafael Lima Teixeira", "rafael.lima@aluno.unicatolica.edu.br", "20241007", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Juliana Ribeiro Castro", "juliana.ribeiro@aluno.unicatolica.edu.br", "20241008", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Mateus Cavalcante Dias", "mateus.cavalcante@aluno.unicatolica.edu.br", "20241009", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Isabela Gomes Pereira", "isabela.gomes@aluno.unicatolica.edu.br", "20241010", "ALUNO", "Aluno@2026"),
            new ContaEsperada("Prof. Dr. Ricardo Vasconcelos", "coordenacao.odonto@unicatolica.edu.br", "COORD202601", "COORDENADOR", "Coord@2026"));

    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void migrationPersisteAsTrezeContasComSenhasValidasEPermaneceIdempotente() throws Exception {
        List<ContaPersistida> antes = carregarContas();
        assertEquals(13, antes.size());
        assertEquals(2, antes.stream().filter(conta -> conta.perfil().equals("SECRETARIA")).count());
        assertEquals(10, antes.stream().filter(conta -> conta.perfil().equals("ALUNO")).count());
        assertEquals(1, antes.stream().filter(conta -> conta.perfil().equals("COORDENADOR")).count());

        for (ContaEsperada esperada : CONTAS) {
            ContaPersistida persistida = antes.stream()
                    .filter(conta -> conta.matricula().equals(esperada.matricula()))
                    .findFirst().orElseThrow();
            assertEquals(esperada.nome(), persistida.nome());
            assertEquals(esperada.email(), persistida.email());
            assertEquals(esperada.perfil(), persistida.perfil());
            assertTrue(persistida.ativo());
            assertTrue(passwordEncoder.matches(esperada.senha(), persistida.senhaHash()));
        }

        String migration = new String(
                new ClassPathResource("db/migration/V19__seed_usuarios_iniciais.sql")
                        .getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        jdbc.execute(migration);
        assertEquals(antes, carregarContas(), "A reaplicação não deve duplicar nem sobrescrever as contas");
    }

    private List<ContaPersistida> carregarContas() {
        return jdbc.query("""
                SELECT nome, email, senha_hash, perfil, matricula_ou_registro, ativo
                FROM usuarios
                WHERE matricula_ou_registro IN (
                    'SEC202601', 'SEC202602', '20241001', '20241002', '20241003', '20241004',
                    '20241005', '20241006', '20241007', '20241008', '20241009', '20241010', 'COORD202601'
                )
                ORDER BY matricula_ou_registro
                """, (rs, rowNum) -> new ContaPersistida(
                rs.getString("nome"), rs.getString("email"), rs.getString("senha_hash"),
                rs.getString("perfil"), rs.getString("matricula_ou_registro"), rs.getBoolean("ativo")));
    }

    private record ContaEsperada(String nome, String email, String matricula, String perfil, String senha) {}

    private record ContaPersistida(
            String nome, String email, String senhaHash, String perfil, String matricula, boolean ativo) {}
}
