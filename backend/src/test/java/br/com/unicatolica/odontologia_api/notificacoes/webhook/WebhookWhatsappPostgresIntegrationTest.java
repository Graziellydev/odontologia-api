package br.com.unicatolica.odontologia_api.notificacoes.webhook;

import br.com.unicatolica.odontologia_api.notificacoes.service.Lembrete24hService;
import br.com.unicatolica.odontologia_api.notificacoes.service.NotificacaoConclusaoService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "RF009_PG_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:55432/odontologia_rf009_test")
class WebhookWhatsappPostgresIntegrationTest {

    private static final String URL_ISOLADA = "jdbc:postgresql://127.0.0.1:55432/odontologia_rf009_test";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mvc;
    @Autowired private Lembrete24hService lembretes;
    @Autowired private NotificacaoConclusaoService conclusao;

    private Long usuarioId;
    private Long pacienteId;
    private Long procedimentoId;
    private Long disciplinaId;
    private Long turmaId;
    private Long cadeiraId;
    private Long vagaId;
    private Long filaId;
    private Long consultaId;
    private Long notificacaoId;
    private Long consultaAmbiguaId;
    private Long vagaAmbiguaId;
    private Long cadeiraAmbiguaId;
    private Long notificacaoAmbiguaId;
    private String telefone;
    private LocalDate dataConsulta;
    private final List<Long> pacientesOferta = new ArrayList<>();
    private final List<Long> filasOferta = new ArrayList<>();
    private final List<String> telefonesOferta = new ArrayList<>();
    private final List<Long> consultasOferta = new ArrayList<>();
    private final List<Long> vagasOfertaExtra = new ArrayList<>();
    private final List<Long> cadeirasOfertaExtra = new ArrayList<>();
    private final List<Long> procedimentosOfertaExtra = new ArrayList<>();

    @DynamicPropertySource
    static void configurarBancoIsolado(DynamicPropertyRegistry properties) {
        String url = System.getenv("RF009_PG_URL");
        if (!URL_ISOLADA.equals(url)) {
            throw new IllegalStateException("RF009_PG_URL must target the disposable RF009 database.");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        properties.add("spring.flyway.enabled", () -> "true");
        properties.add("spring.security.user.password", () -> "rf009-test-only");
        properties.add("app.whatsapp.webhook-token", () -> "rf009-webhook-test-token");
    }

    @BeforeEach
    void prepararFixture() {
        pacientesOferta.clear();
        filasOferta.clear();
        telefonesOferta.clear();
        consultasOferta.clear();
        vagasOfertaExtra.clear();
        cadeirasOfertaExtra.clear();
        procedimentosOfertaExtra.clear();
        String sufixo = UUID.randomUUID().toString().replace("-", "");
        long cpfNumero = Math.abs((long) sufixo.hashCode()) + 10_000_000_000L;
        String cpf = String.format("%011d", cpfNumero % 100_000_000_000L);
        telefone = String.format("119%08d", Math.abs((long) sufixo.hashCode()) % 100_000_000L);
        dataConsulta = LocalDate.now().plusDays(1);

        usuarioId = inserirId("INSERT INTO usuarios (nome,email,senha_hash,perfil) VALUES (?,?,?,'ALUNO') RETURNING id",
                "RF009 aluno", "rf009-" + sufixo + "@test.invalid", "hash");
        pacienteId = inserirId("""
                INSERT INTO pacientes (nome_completo,cpf,data_nascimento,telefone,bairro,cidade,uf)
                VALUES (?,?,DATE '1990-01-01',?,'Teste','Fortaleza','CE') RETURNING id
                """, "RF009 paciente", cpf, telefone);
        procedimentoId = inserirId("""
                INSERT INTO procedimentos (nome,especialidade,exige_dente,exige_classe)
                VALUES (?,?,TRUE,TRUE) RETURNING id
                """, "RF009-" + sufixo, "Odontologia");
        disciplinaId = inserirId("INSERT INTO disciplinas (nome,periodo_curso) VALUES (?,5) RETURNING id",
                "RF009-" + sufixo);
        jdbc.update("INSERT INTO disciplina_procedimentos (disciplina_id,procedimento_id) VALUES (?,?)",
                disciplinaId, procedimentoId);
        turmaId = inserirId("""
                INSERT INTO turmas (disciplina_id,semestre_letivo,codigo_turma)
                VALUES (?,'2026.2',?) RETURNING id
                """, disciplinaId, "RF009-" + sufixo);
        cadeiraId = inserirId("INSERT INTO cadeiras (numero) VALUES (?) RETURNING id", sufixo.substring(0, 8));
        vagaId = inserirId("INSERT INTO vagas (cadeira_id,data,turno) VALUES (?,?,'MANHA') RETURNING id",
                cadeiraId, dataConsulta);
        filaId = inserirId("""
                INSERT INTO fila_espera (paciente_id,procedimento_id,dente,classe,nivel_urgencia,status_fila)
                VALUES (?,?,'11','I','ALTA','ALOCADO') RETURNING id
                """, pacienteId, procedimentoId);
        consultaId = inserirId("""
                INSERT INTO consultas (paciente_id,aluno_id,turma_id,procedimento_id,dente,classe,vaga_id,
                    fila_espera_id,status_consulta)
                VALUES (?,?,?,?,'11','I',?,?,'AGENDADO') RETURNING id
                """, pacienteId, usuarioId, turmaId, procedimentoId, vagaId, filaId);
    }

    @AfterEach
    void removerFixture() {
        if (pacienteId != null) jdbc.update("DELETE FROM notificacoes_whatsapp WHERE paciente_id=?", pacienteId);
        if (consultaId != null) {
            jdbc.update("DELETE FROM notificacoes_whatsapp WHERE consulta_id=?", consultaId);
            jdbc.update("DELETE FROM consulta_historico WHERE consulta_id=?", consultaId);
            jdbc.update("DELETE FROM consultas WHERE id=?", consultaId);
        }
        if (consultaAmbiguaId != null) {
            jdbc.update("DELETE FROM notificacoes_whatsapp WHERE consulta_id=?", consultaAmbiguaId);
            jdbc.update("DELETE FROM consulta_historico WHERE consulta_id=?", consultaAmbiguaId);
            jdbc.update("DELETE FROM consultas WHERE id=?", consultaAmbiguaId);
        }
        for (Long id : filasOferta) {
            jdbc.update("DELETE FROM notificacoes_whatsapp WHERE paciente_id IN "
                    + "(SELECT paciente_id FROM fila_espera WHERE id=?)", id);
            jdbc.update("DELETE FROM consulta_historico WHERE consulta_id IN "
                    + "(SELECT id FROM consultas WHERE fila_espera_id=?)", id);
            jdbc.update("DELETE FROM consultas WHERE fila_espera_id=?", id);
            jdbc.update("DELETE FROM fila_espera WHERE id=?", id);
        }
        for (Long id : consultasOferta) {
            jdbc.update("DELETE FROM notificacoes_whatsapp WHERE consulta_id=?", id);
            jdbc.update("DELETE FROM consulta_historico WHERE consulta_id=?", id);
            jdbc.update("DELETE FROM consultas WHERE id=?", id);
        }
        for (Long id : pacientesOferta) jdbc.update("DELETE FROM pacientes WHERE id=?", id);
        for (Long id : procedimentosOfertaExtra) jdbc.update("DELETE FROM procedimentos WHERE id=?", id);
        for (Long id : vagasOfertaExtra) jdbc.update("DELETE FROM vagas WHERE id=?", id);
        for (Long id : cadeirasOfertaExtra) jdbc.update("DELETE FROM cadeiras WHERE id=?", id);
        if (vagaAmbiguaId != null) jdbc.update("DELETE FROM vagas WHERE id=?", vagaAmbiguaId);
        if (cadeiraAmbiguaId != null) jdbc.update("DELETE FROM cadeiras WHERE id=?", cadeiraAmbiguaId);
        if (filaId != null) jdbc.update("DELETE FROM fila_espera WHERE id=?", filaId);
        if (vagaId != null) jdbc.update("DELETE FROM vagas WHERE id=?", vagaId);
        if (cadeiraId != null) jdbc.update("DELETE FROM cadeiras WHERE id=?", cadeiraId);
        if (turmaId != null) jdbc.update("DELETE FROM turmas WHERE id=?", turmaId);
        if (disciplinaId != null) jdbc.update("DELETE FROM disciplina_procedimentos WHERE disciplina_id=?", disciplinaId);
        if (procedimentoId != null) jdbc.update("DELETE FROM procedimentos WHERE id=?", procedimentoId);
        if (disciplinaId != null) jdbc.update("DELETE FROM disciplinas WHERE id=?", disciplinaId);
        if (pacienteId != null) jdbc.update("DELETE FROM pacientes WHERE id=?", pacienteId);
        if (usuarioId != null) jdbc.update("DELETE FROM usuarios WHERE id=?", usuarioId);
    }

    @Test
    void criaLembreteD1EImpedeDuplicidadeEmExecucoesConcorrentes() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primeira = executor.submit(() -> { inicio.await(); return lembretes.processar(LocalDate.now()); });
            var segunda = executor.submit(() -> { inicio.await(); return lembretes.processar(LocalDate.now()); });
            inicio.countDown();
            primeira.get(15, TimeUnit.SECONDS);
            segunda.get(15, TimeUnit.SECONDS);
        }
        assertEquals(1, contar("LEMBRETE_24H"));
        assertEquals("PENDENTE", statusNotificacao("LEMBRETE_24H"));
    }

    @Test
    void confirmaConsultaPorEndpointERespostaDuplicadaNaoDuplicaHistorico() throws Exception {
        criarLembrete();
        postar("1", "CONFIRMADA");
        postar("SIM", "NAO_ENCONTRADA");

        assertEquals("CONFIRMADA_24H", jdbc.queryForObject(
                "SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaId));
        assertEquals("1", jdbc.queryForObject(
                "SELECT resposta_recebida FROM notificacoes_whatsapp WHERE id=?", String.class, notificacaoId));
        assertEquals("PACIENTE_WHATSAPP", jdbc.queryForObject(
                "SELECT origem FROM consulta_historico WHERE consulta_id=? ORDER BY id DESC LIMIT 1",
                String.class, consultaId));
        assertEquals(1, contar("COMPROVANTE_AGENDAMENTO"));
    }

    @Test
    void aceitaRespostaTextualDeConfirmacao() throws Exception {
        criarLembrete();
        postar("CONFIRMAR", "CONFIRMADA");
        assertEquals("CONFIRMADA_24H", jdbc.queryForObject(
                "SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaId));
    }

    @Test
    void tokenConfiguradoExigeCabecalhoERejeitaAntesDeAlterarEstado() throws Exception {
        criarLembrete();
        String corpo = "{\"telefone\":\"" + telefone + "\",\"mensagem\":\"1\"}";
        mvc.perform(post("/api/webhooks/whatsapp").contentType("application/json").content(corpo))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/webhooks/whatsapp").header("X-Webhook-Token", "invalido")
                        .contentType("application/json").content(corpo))
                .andExpect(status().isUnauthorized());
        assertEquals("AGENDADO", jdbc.queryForObject(
                "SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaId));
        assertEquals("PENDENTE", jdbc.queryForObject(
                "SELECT status_envio FROM notificacoes_whatsapp WHERE id=?", String.class, notificacaoId));

        postar("1", "CONFIRMADA");
        assertEquals("CONFIRMADA_24H", jdbc.queryForObject(
                "SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaId));
    }

    @Test
    void pesquisaDeSatisfacaoFicaPendenteEIdempotenteAposConclusao() {
        jdbc.update("UPDATE consultas SET status_consulta='REALIZADO' WHERE id=?", consultaId);
        assertEquals(1, conclusao.registrarPesquisaSatisfacao(consultaId));
        assertEquals(0, conclusao.registrarPesquisaSatisfacao(consultaId));
        assertEquals("PENDENTE", statusNotificacao("PESQUISA_SATISFACAO"));
        assertEquals(1, contar("PESQUISA_SATISFACAO"));
    }

    @Test
    void cancelaConsultaPorRespostaTextualLiberaFilaERegistraAuditoria() throws Exception {
        criarLembrete();
        postar("CANCELAR", "CANCELADA");
        postar("2", "NAO_ENCONTRADA");

        assertEquals("CANCELADO_PACIENTE", jdbc.queryForObject(
                "SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaId));
        assertEquals("AGUARDANDO", jdbc.queryForObject(
                "SELECT status_fila FROM fila_espera WHERE id=?", String.class, filaId));
        assertEquals("2", jdbc.queryForObject(
                "SELECT resposta_recebida FROM notificacoes_whatsapp WHERE id=?", String.class, notificacaoId));
        assertEquals("PACIENTE_WHATSAPP", jdbc.queryForObject(
                "SELECT origem FROM consulta_historico WHERE consulta_id=? ORDER BY id DESC LIMIT 1",
                String.class, consultaId));
        assertEquals(1, contar("AVISO_CANCELAMENTO"));
    }

    @RepeatedTest(30)
    void umaSoChamadaRapidaOcupaAOfertaEmAceitesConcorrentes() throws Exception {
        criarPacienteNaFila("RF009 oferta A");
        criarPacienteNaFila("RF009 oferta B");
        criarLembrete();
        postar("2", "CANCELADA");
        assertEquals(2, contar("OFERTA_VAGA_DESISTENCIA"));

        CountDownLatch inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<String> a = executor.submit(() -> { inicio.await(); return respostaOferta(telefonesOferta.get(0)); });
            Future<String> b = executor.submit(() -> { inicio.await(); return respostaOferta(telefonesOferta.get(1)); });
            inicio.countDown();
            String resultadoA = a.get(20, TimeUnit.SECONDS);
            String resultadoB = b.get(20, TimeUnit.SECONDS);
            assertEquals(1, (resultadoA.contains("VAGA_CONFIRMADA") ? 1 : 0)
                    + (resultadoB.contains("VAGA_CONFIRMADA") ? 1 : 0));
            assertEquals(1, (resultadoA.contains("VAGA_INDISPONIVEL") ? 1 : 0)
                    + (resultadoB.contains("VAGA_INDISPONIVEL") ? 1 : 0));
        }

        assertEquals(1, jdbc.queryForObject("""
                SELECT COUNT(*) FROM consultas WHERE vaga_id=?
                  AND status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE')
                """, Integer.class, vagaId));
        assertEquals(1, filasOferta.stream().mapToInt(id -> jdbc.queryForObject(
                "SELECT COUNT(*) FROM fila_espera WHERE id=? AND status_fila='ALOCADO'", Integer.class, id)).sum());
        assertEquals(2, contarRespostasOferta());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM notificacoes_whatsapp "
                + "WHERE paciente_id IN (?,?) AND tipo_mensagem='COMPROVANTE_AGENDAMENTO'",
                Integer.class, pacientesOferta.get(0), pacientesOferta.get(1)));
    }

    @RepeatedTest(3)
    void duasVagasDistintasPodemSerAceitasEmParalelo() throws Exception {
        criarPacienteNaFila("RF009 oferta A");
        criarPacienteNaFila("RF009 oferta B");
        criarLembrete();
        postar("2", "CANCELADA");

        jdbc.update("UPDATE notificacoes_whatsapp SET status_envio='CONFIRMADO_RESPOSTA', "
                + "resposta_recebida='1', respondido_em=now() WHERE paciente_id=? "
                + "AND consulta_id=? AND tipo_mensagem='OFERTA_VAGA_DESISTENCIA'",
                pacientesOferta.get(1), consultaId);

        Long cadeiraExtra = inserirId("INSERT INTO cadeiras (numero) VALUES (?) RETURNING id", "RF009XTRA");
        Long vagaExtra = inserirId("INSERT INTO vagas (cadeira_id,data,turno) VALUES (?,?, 'TARDE') RETURNING id",
                cadeiraExtra, dataConsulta.plusDays(2));
        Long consultaExtra = inserirId("""
                INSERT INTO consultas (paciente_id,aluno_id,turma_id,procedimento_id,dente,classe,vaga_id,
                    fila_espera_id,status_consulta)
                VALUES (?,?,?,?, '11','I',?,?,'CANCELADO_PACIENTE') RETURNING id
                """, pacienteId, usuarioId, turmaId, procedimentoId, vagaExtra, filaId);
        jdbc.update("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,status_envio)
                VALUES (?,?,'OFERTA_VAGA_DESISTENCIA','Oferta de teste','PENDENTE')
                """, pacientesOferta.get(1), consultaExtra);
        consultasOferta.add(consultaExtra);
        vagasOfertaExtra.add(vagaExtra);
        cadeirasOfertaExtra.add(cadeiraExtra);

        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Future<String> a = executor.submit(() -> { inicio.await(); return respostaOferta(telefonesOferta.get(0)); });
            Future<String> b = executor.submit(() -> { inicio.await(); return respostaOferta(telefonesOferta.get(1)); });
            inicio.countDown();
            assertEquals(true, a.get(20, TimeUnit.SECONDS).contains("VAGA_CONFIRMADA"));
            assertEquals(true, b.get(20, TimeUnit.SECONDS).contains("VAGA_CONFIRMADA"));
        }

        assertEquals(1, contarConsultasAtivas(vagaId));
        assertEquals(1, contarConsultasAtivas(vagaExtra));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM consultas WHERE vaga_id IN (?,?) "
                + "AND status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE')",
                Integer.class, vagaId, vagaExtra));
        assertEquals(2, filasOferta.stream().mapToInt(id -> jdbc.queryForObject(
                "SELECT COUNT(*) FROM fila_espera WHERE id=? AND status_fila='ALOCADO'", Integer.class, id)).sum());
    }

    @Test
    void chamadaRapidaFiltraCompatibilidadeESeguePrioridadeAtualDaFila() throws Exception {
        for (int i = 0; i < 6; i++) criarPacienteNaFila("RF009 compatibilidade " + i);
        jdbc.update("UPDATE fila_espera SET nivel_urgencia='PNE', data_entrada_fila=? WHERE id=?",
                java.time.OffsetDateTime.parse("2020-01-01T00:00:00Z"), filasOferta.get(0));
        jdbc.update("UPDATE fila_espera SET nivel_urgencia='ALTA', data_entrada_fila=?, prioridade_reagendamento=FALSE WHERE id=?",
                java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"), filasOferta.get(1));
        jdbc.update("UPDATE fila_espera SET nivel_urgencia='ALTA', data_entrada_fila=?, prioridade_reagendamento=TRUE WHERE id=?",
                java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z"), filasOferta.get(2));
        jdbc.update("UPDATE fila_espera SET dente='12' WHERE id=?", filasOferta.get(3));
        jdbc.update("UPDATE fila_espera SET classe='II' WHERE id=?", filasOferta.get(4));
        Long outroProcedimento = inserirId("INSERT INTO procedimentos (nome,especialidade) VALUES (?,?) RETURNING id",
                "RF009 procedimento incompatível", "Odontologia");
        procedimentosOfertaExtra.add(outroProcedimento);
        jdbc.update("UPDATE fila_espera SET procedimento_id=? WHERE id=?", outroProcedimento, filasOferta.get(5));

        criarLembrete();
        postar("2", "CANCELADA");

        List<Long> notificados = jdbc.query("""
                SELECT n.paciente_id FROM notificacoes_whatsapp n
                WHERE n.consulta_id=? AND n.tipo_mensagem='OFERTA_VAGA_DESISTENCIA'
                ORDER BY n.id
                """, (rs, row) -> rs.getLong(1), consultaId);
        assertEquals(List.of(pacientesOferta.get(2), pacientesOferta.get(1), pacientesOferta.get(0)), notificados);
        assertEquals(3, contar("OFERTA_VAGA_DESISTENCIA"));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM notificacoes_whatsapp "
                + "WHERE consulta_id=? AND tipo_mensagem='OFERTA_VAGA_DESISTENCIA' AND status_envio='PENDENTE'",
                Integer.class, consultaId));
    }

    @Test
    void rejeitaTelefoneInvalidoSemCorrespondenciaEAmbiguidadeSemMutarConsultas() throws Exception {
        postarComTelefone("12", "1", "TELEFONE_INVALIDO");
        postarComTelefone("5511888888888", "1", "NAO_ENCONTRADA");
        criarLembrete();

        cadeiraAmbiguaId = inserirId("INSERT INTO cadeiras (numero) VALUES (?) RETURNING id", "AMBIGUA1");
        vagaAmbiguaId = inserirId("INSERT INTO vagas (cadeira_id,data,turno) VALUES (?,?, 'TARDE') RETURNING id",
                cadeiraAmbiguaId, dataConsulta.plusDays(1));
        consultaAmbiguaId = inserirId("""
                INSERT INTO consultas (paciente_id,aluno_id,turma_id,procedimento_id,dente,classe,vaga_id,
                    fila_espera_id,status_consulta)
                VALUES (?,?,?,?,'11','I',?,?,'AGENDADO') RETURNING id
                """, pacienteId, usuarioId, turmaId, procedimentoId, vagaAmbiguaId, filaId);
        notificacaoAmbiguaId = inserirId("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,status_envio)
                VALUES (?,?,'LEMBRETE_24H','Segundo lembrete','PENDENTE') RETURNING id
                """, pacienteId, consultaAmbiguaId);

        postarComTelefone(telefone, "1", "AMBIGUA");
        assertEquals("AGENDADO", jdbc.queryForObject("SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaId));
        assertEquals("AGENDADO", jdbc.queryForObject("SELECT status_consulta FROM consultas WHERE id=?", String.class, consultaAmbiguaId));
        assertEquals("PENDENTE", jdbc.queryForObject("SELECT status_envio FROM notificacoes_whatsapp WHERE id=?", String.class, notificacaoId));
        assertEquals("PENDENTE", jdbc.queryForObject("SELECT status_envio FROM notificacoes_whatsapp WHERE id=?", String.class, notificacaoAmbiguaId));
    }

    private void criarLembrete() {
        notificacaoId = inserirId("""
                INSERT INTO notificacoes_whatsapp
                    (paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,status_envio)
                VALUES (?,?,'LEMBRETE_24H','Confirme sua consulta','PENDENTE') RETURNING id
                """, pacienteId, consultaId);
    }

    private void criarPacienteNaFila(String nome) {
        String sufixo = UUID.randomUUID().toString().replace("-", "");
        String cpf = String.format("%011d", (Math.abs((long) sufixo.hashCode()) + 10_000_000_000L) % 100_000_000_000L);
        String contato = String.format("119%08d", Math.abs((long) sufixo.hashCode()) % 100_000_000L);
        Long novoPaciente = inserirId("""
                INSERT INTO pacientes (nome_completo,cpf,data_nascimento,telefone,bairro,cidade,uf)
                VALUES (?,?,DATE '1990-01-01',?,'Teste','Fortaleza','CE') RETURNING id
                """, nome, cpf, contato);
        Long novaFila = inserirId("""
                INSERT INTO fila_espera (paciente_id,procedimento_id,dente,classe,nivel_urgencia,status_fila)
                VALUES (?,?,'11','I','ALTA','AGUARDANDO') RETURNING id
                """, novoPaciente, procedimentoId);
        pacientesOferta.add(novoPaciente);
        filasOferta.add(novaFila);
        telefonesOferta.add(contato);
    }

    private String respostaOferta(String numero) throws Exception {
        return mvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", "rf009-webhook-test-token")
                        .contentType("application/json")
                        .content("{\"telefone\":\"" + numero + "\",\"mensagem\":\"1\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    private void postar(String mensagem, String resultado) throws Exception {
        postarComTelefone(telefone, mensagem, resultado);
    }

    private void postarComTelefone(String numero, String mensagem, String resultado) throws Exception {
        mvc.perform(post("/api/webhooks/whatsapp")
                        .header("X-Webhook-Token", "rf009-webhook-test-token")
                        .contentType("application/json")
                        .content("{\"telefone\":\"" + numero + "\",\"mensagem\":\"" + mensagem + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultado").value(resultado));
    }

    private Long inserirId(String sql, Object... args) {
        return jdbc.queryForObject(sql, Long.class, args);
    }

    private int contar(String tipo) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notificacoes_whatsapp WHERE consulta_id=? AND tipo_mensagem=?",
                Integer.class, consultaId, tipo);
    }

    private int contarConsultasAtivas(Long idVaga) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM consultas WHERE vaga_id=? "
                + "AND status_consulta NOT IN ('CANCELADO_SECRETARIA','CANCELADO_PACIENTE')",
                Integer.class, idVaga);
    }

    private int contarRespostasOferta() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM notificacoes_whatsapp "
                + "WHERE consulta_id=? AND tipo_mensagem='OFERTA_VAGA_DESISTENCIA' "
                + "AND status_envio='CONFIRMADO_RESPOSTA' AND resposta_recebida='1'",
                Integer.class, consultaId);
    }

    private String statusNotificacao(String tipo) {
        return jdbc.queryForObject("SELECT status_envio FROM notificacoes_whatsapp WHERE consulta_id=? AND tipo_mensagem=?",
                String.class, consultaId, tipo);
    }
}
