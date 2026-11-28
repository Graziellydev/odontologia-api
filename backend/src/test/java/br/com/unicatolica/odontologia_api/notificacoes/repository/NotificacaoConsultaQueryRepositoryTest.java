package br.com.unicatolica.odontologia_api.notificacoes.repository;

import br.com.unicatolica.odontologia_api.notificacoes.dto.NotificacaoResumoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificacaoConsultaQueryRepositoryTest {

    private NotificacaoConsultaQueryRepository repository;

    @BeforeEach
    void configurarBancoEmMemoria() {
        var dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:notificacoes;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE",
                "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE pacientes (id BIGINT PRIMARY KEY, nome_completo VARCHAR(150), telefone VARCHAR(20))");
        jdbc.execute("CREATE TABLE consultas (id BIGINT PRIMARY KEY, vaga_id BIGINT, procedimento_id BIGINT, dente VARCHAR(3), classe VARCHAR(3))");
        jdbc.execute("CREATE TABLE vagas (id BIGINT PRIMARY KEY, cadeira_id BIGINT, data DATE, turno VARCHAR(20))");
        jdbc.execute("CREATE TABLE cadeiras (id BIGINT PRIMARY KEY, numero VARCHAR(10))");
        jdbc.execute("CREATE TABLE procedimentos (id BIGINT PRIMARY KEY, nome VARCHAR(100))");
        jdbc.execute("""
                CREATE TABLE notificacoes_whatsapp (
                    id BIGINT PRIMARY KEY, paciente_id BIGINT, consulta_id BIGINT,
                    tipo_mensagem VARCHAR(50), conteudo_mensagem VARCHAR(500),
                    status_envio VARCHAR(25), criado_em TIMESTAMP WITH TIME ZONE,
                    resposta_recebida VARCHAR(10), respondido_em TIMESTAMP WITH TIME ZONE)
                """);
        jdbc.update("INSERT INTO consultas VALUES (10, 20, 30, '11', 'II'), (11, 21, 30, '12', 'I')");
        jdbc.update("INSERT INTO vagas VALUES (20, 40, DATE '2026-11-24', 'MANHA'), (21, 40, DATE '2026-11-25', 'TARDE')");
        jdbc.update("INSERT INTO cadeiras VALUES (40, '1')");
        jdbc.update("INSERT INTO procedimentos VALUES (30, 'Restauração')");
        jdbc.update("INSERT INTO pacientes VALUES (1, 'Ana', '+55 (11) 99999-9999')");
        jdbc.update("INSERT INTO notificacoes_whatsapp (id,paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,status_envio,criado_em) VALUES (1, 1, 10, 'LEMBRETE_24H', 'Oi Ana', 'PENDENTE', ?)",
                Timestamp.from(Instant.parse("2026-11-23T13:00:00Z")));
        jdbc.update("INSERT INTO notificacoes_whatsapp (id,paciente_id,consulta_id,tipo_mensagem,conteudo_mensagem,status_envio,criado_em) VALUES (2, 1, 11, 'AVISO_CANCELAMENTO', 'Cancelada', 'ERRO', ?)",
                Timestamp.from(Instant.parse("2026-11-22T13:00:00Z")));
        repository = new NotificacaoConsultaQueryRepository(jdbc);
    }

    @Test
    void aplicaPeriodoTipoEStatusEConstroiLink() {
        List<NotificacaoResumoDTO> resultado = repository.listar(
                LocalDate.of(2026, 11, 23), LocalDate.of(2026, 11, 23),
                "LEMBRETE_24H", "PENDENTE");

        assertEquals(1, resultado.size());
        assertEquals("Ana", resultado.getFirst().paciente());
        assertEquals("https://wa.me/5511999999999?text=Oi%20Ana", resultado.getFirst().linkWhatsapp());
    }

    @Test
    void retornaListaVaziaQuandoFiltrosNaoCorrespondem() {
        assertEquals(0, repository.listar(null, null, "NAO_EXISTE", null).size());
    }
}
