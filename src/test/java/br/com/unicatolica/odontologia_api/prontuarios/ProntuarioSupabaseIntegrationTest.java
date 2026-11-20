package br.com.unicatolica.odontologia_api.prontuarios;

import br.com.unicatolica.odontologia_api.prontuarios.security.ProntuarioNetworkFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static br.com.unicatolica.odontologia_api.CsrfTestSupport.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Executes against the test Supabase schema and rolls every fixture back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("supabase-test")
@TestPropertySource(properties = {
        "app.prontuario.allowed-cidrs=127.0.0.1/32,::1/128",
        "app.prontuario.trusted-proxy-cidrs=127.0.0.1/32,::1/128",
        "app.prontuario.validar-turno-estrito=false"
})
@EnabledIfEnvironmentVariable(named = "SUPABASE_TEST_JDBC_URL", matches = "jdbc:postgresql:.*")
@Transactional
class ProntuarioSupabaseIntegrationTest {
    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper objectMapper;

    private Fixture fixture;

    @BeforeEach
    void prepararDados() {
        fixture = criarFixture();
    }

    @Test
    void bloqueiaIpForaDaRedeComMensagemExata() throws Exception {
        mvc.perform(get("/api/prontuarios/paciente/{id}", fixture.pacienteId())
                        .with(aluno())
                        .with(remote("203.0.113.45")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(ProntuarioNetworkFilter.BLOCK_MESSAGE));
    }

    @Test
    void bloqueiaAlunoSemConsultaVinculadaNoDia() throws Exception {
        mvc.perform(get("/api/prontuarios/paciente/{id}", fixture.pacienteSemConsultaId())
                        .with(aluno()).with(remote("127.0.0.1")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("consulta ativa")));
    }

    @Test
    void retornaHistoricoAnamneseEEstadoAtualDoOdontograma() throws Exception {
        inserirEvolucaoAnterior();
        mvc.perform(get("/api/prontuarios/paciente/{id}", fixture.pacienteId())
                        .with(aluno()).with(remote("127.0.0.1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paciente.id").value(fixture.pacienteId()))
                .andExpect(jsonPath("$.anamnese.queixaPrincipal").value("Sensibilidade"))
                .andExpect(jsonPath("$.odontograma[0].dente").value("36"))
                .andExpect(jsonPath("$.evolucoes[0].evolucaoClinica").value("Registro anterior"));
    }

    @Test
    void gravaEvolucaoAssinaConcluiETriggerBloqueiaUpdate() throws Exception {
        long prontuarioId = criarEvolucao(true);
        String assinatura = jdbc.queryForObject(
                "SELECT assinatura_digital FROM prontuarios WHERE id = ?", String.class, prontuarioId);
        assertNotNull(assinatura);
        org.junit.jupiter.api.Assertions.assertTrue(assinatura.startsWith("v1;sha256="));
        assertEquals("REALIZADO", jdbc.queryForObject(
                "SELECT status_consulta FROM consultas WHERE id = ?", String.class, fixture.consultaId()));
        assertEquals("ALUNO", jdbc.queryForObject(
                "SELECT origem FROM consulta_historico WHERE consulta_id = ? AND status_novo = 'REALIZADO' ORDER BY id DESC LIMIT 1",
                String.class, fixture.consultaId()));
        assertEquals(fixture.alunoId(), jdbc.queryForObject(
                "SELECT alterado_por_id FROM consulta_historico WHERE consulta_id = ? AND status_novo = 'REALIZADO' ORDER BY id DESC LIMIT 1",
                Long.class, fixture.consultaId()));
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE prontuarios SET evolucao_clinica = 'alterada' WHERE id = ?", prontuarioId));
    }

    @Test
    void triggerBloqueiaDeleteDeProntuario() throws Exception {
        long prontuarioId = criarEvolucao(false);
        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM prontuarios WHERE id = ?", prontuarioId));
    }

    @Test
    void criaAdendoAposAssinaturaEConclusao() throws Exception {
        long prontuarioId = criarEvolucao(true);
        mvc.perform(post("/api/prontuarios/{id}/adendos", prontuarioId)
                        .with(aluno()).with(remote("127.0.0.1"))
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of("texto", "Ajuste complementar"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autorId").value(fixture.alunoId()));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM prontuario_adendos WHERE prontuario_id = ?", Integer.class, prontuarioId));
    }

    @Test
    void triggerBloqueiaUpdateEDeleteDeAdendo() throws Exception {
        long prontuarioId = criarEvolucao(false);
        long adendoId = jdbc.queryForObject("""
                INSERT INTO prontuario_adendos (prontuario_id, autor_id, texto)
                VALUES (?, ?, 'Adendo de teste') RETURNING id
                """, Long.class, prontuarioId, fixture.alunoId());
        assertThrows(DataAccessException.class, () -> jdbc.update(
                "UPDATE prontuario_adendos SET texto = 'alterado' WHERE id = ?", adendoId));
    }

    @Test
    void triggerBloqueiaDeleteDeAdendo() throws Exception {
        long prontuarioId = criarEvolucao(false);
        long adendoId = jdbc.queryForObject("""
                INSERT INTO prontuario_adendos (prontuario_id, autor_id, texto)
                VALUES (?, ?, 'Adendo de teste') RETURNING id
                """, Long.class, prontuarioId, fixture.alunoId());
        assertThrows(DataAccessException.class,
                () -> jdbc.update("DELETE FROM prontuario_adendos WHERE id = ?", adendoId));
    }

    private long criarEvolucao(boolean concluir) throws Exception {
        String json = """
                {"consultaId":%d,"procedimentoId":%d,"dente":"36","classe":"I",
                 "evolucaoClinica":"Restauração realizada","concluirAtendimento":%s,
                 "observacoesOdontograma":[{"dente":"36","observacao":"Sem intercorrências"}]}
                """.formatted(fixture.consultaId(), fixture.procedimentoId(), concluir);
        String response = mvc.perform(post("/api/prontuarios")
                        .with(aluno()).with(remote("127.0.0.1"))
                        .with(csrf())
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assinaturaDigital").value(org.hamcrest.Matchers.startsWith("v1;sha256=")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private void inserirEvolucaoAnterior() {
        jdbc.update("""
                INSERT INTO prontuarios (paciente_id, aluno_id, consulta_id, turma_id, procedimento_id,
                                         dente, classe, evolucao_clinica, assinatura_digital)
                VALUES (?, ?, ?, ?, ?, '36', 'I', 'Registro anterior', 'v1;sha256=anterior')
                """, fixture.pacienteId(), fixture.alunoId(), fixture.consultaId(),
                fixture.turmaId(), fixture.procedimentoId());
    }

    private Fixture criarFixture() {
        long disciplinaId = inserir("INSERT INTO disciplinas (nome, periodo_curso) VALUES (?, 6) RETURNING id",
                "Clinica " + UUID.randomUUID());
        long procedimentoId = inserir("INSERT INTO procedimentos (nome, especialidade) VALUES (?, 'Dentística') RETURNING id",
                "Procedimento " + UUID.randomUUID());
        jdbc.update("INSERT INTO disciplina_procedimentos (disciplina_id, procedimento_id, restricao_dente) VALUES (?, ?, 'QUALQUER')",
                disciplinaId, procedimentoId);
        long alunoId = inserir("""
                INSERT INTO usuarios (nome, email, senha_hash, perfil, matricula_ou_registro)
                VALUES ('Aluno Integração', ?, 'hash', 'ALUNO', ?) RETURNING id
                """, "aluno-" + UUID.randomUUID() + "@teste.invalid", "M" + UUID.randomUUID());
        String email = jdbc.queryForObject("SELECT email FROM usuarios WHERE id = ?", String.class, alunoId);
        String cpf = String.format("%011d", Math.abs(System.nanoTime()) % 100_000_000_000L);
        long pacienteId = inserir("""
                INSERT INTO pacientes (nome_completo, cpf, data_nascimento, telefone, bairro, cidade)
                VALUES ('Paciente Integração', ?, '1990-01-01', '85999990000', 'Centro', 'Quixadá') RETURNING id
                """, cpf);
        String cpf2 = String.format("%011d", (Math.abs(System.nanoTime()) + 1) % 100_000_000_000L);
        long pacienteSemConsulta = inserir("""
                INSERT INTO pacientes (nome_completo, cpf, data_nascimento, telefone, bairro, cidade)
                VALUES ('Paciente sem consulta', ?, '1985-01-01', '85999990001', 'Centro', 'Quixadá') RETURNING id
                """, cpf2);
        long turmaId = inserir("""
                INSERT INTO turmas (disciplina_id, semestre_letivo, codigo_turma)
                VALUES (?, '2026.2', ?) RETURNING id
                """, disciplinaId, "T" + UUID.randomUUID());
        jdbc.update("INSERT INTO matriculas_turma (turma_id, aluno_id) VALUES (?, ?)", turmaId, alunoId);
        long cadeiraId = inserir("INSERT INTO cadeiras (numero) VALUES (?) RETURNING id",
                "C" + UUID.randomUUID().toString().substring(0, 9));
        long vagaId = inserir("INSERT INTO vagas (cadeira_id, data, turno) VALUES (?, CURRENT_DATE, 'TARDE') RETURNING id",
                cadeiraId);
        jdbc.update("""
                INSERT INTO consultas (paciente_id, aluno_id, turma_id, procedimento_id, dente, classe, vaga_id)
                VALUES (?, ?, ?, ?, '36', 'I', ?)
                """, pacienteId, alunoId, turmaId, procedimentoId, vagaId);
        long consultaId = jdbc.queryForObject("SELECT id FROM consultas WHERE vaga_id = ?", Long.class, vagaId);
        jdbc.update("""
                INSERT INTO paciente_anamnese (paciente_id, queixa_principal, respostas_questionario)
                VALUES (?, 'Sensibilidade', '{"fumante":false}'::jsonb)
                """, pacienteId);
        jdbc.update("""
                INSERT INTO paciente_odontograma (paciente_id, dente, condicao_dente, observacao)
                VALUES (?, '36', 'CARIADO', 'Lesão inicial')
                """, pacienteId);
        return new Fixture(alunoId, email, pacienteId, pacienteSemConsulta, turmaId, procedimentoId, consultaId);
    }

    private long inserir(String sql, Object... parametros) {
        return jdbc.queryForObject(sql, Long.class, parametros);
    }

    private RequestPostProcessor aluno() {
        return request -> {
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                    fixture.alunoEmail(), null, List.of(new SimpleGrantedAuthority("ALUNO"))));
            MockHttpSession session = new MockHttpSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            request.setSession(session);
            return request;
        };
    }

    private static RequestPostProcessor remote(String ip) {
        return request -> { request.setRemoteAddr(ip); return request; };
    }

    private record Fixture(long alunoId, String alunoEmail, long pacienteId, long pacienteSemConsultaId,
                           long turmaId, long procedimentoId, long consultaId) { }
}
