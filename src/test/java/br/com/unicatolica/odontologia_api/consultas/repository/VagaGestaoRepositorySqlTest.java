package br.com.unicatolica.odontologia_api.consultas.repository;

import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VagaGestaoRepositorySqlTest {

    @Mock NamedParameterJdbcTemplate jdbc;

    @Test void geracaoUsaTodosOsDiasCadeirasAtivasEDeduplicacaoDaUkVaga() {
        var inicio = LocalDate.parse("2026-11-09");
        var fim = LocalDate.parse("2026-11-15");
        var respostaEsperada = new GerarTurnosResponseDTO(28, 0);
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(respostaEsperada);
        VagaGestaoRepository repository = new VagaGestaoRepository(jdbc);

        GerarTurnosResponseDTO resposta = repository.gerarTurnos(inicio, fim);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<SqlParameterSource> parametros = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForObject(sql.capture(), parametros.capture(), any(org.springframework.jdbc.core.RowMapper.class));
        String consulta = sql.getValue().toUpperCase();
        assertTrue(consulta.contains("GENERATE_SERIES"));
        assertTrue(consulta.contains("CA.ATIVA = TRUE"));
        assertTrue(consulta.contains("('MANHA'), ('TARDE')"));
        assertTrue(consulta.contains("ON CONFLICT (CADEIRA_ID, DATA, TURNO) DO NOTHING"));
        assertEquals(inicio, ((MapSqlParameterSource) parametros.getValue()).getValue("inicio"));
        assertEquals(fim, ((MapSqlParameterSource) parametros.getValue()).getValue("fim"));
        assertEquals(respostaEsperada, resposta);
    }

    @Test void gradeDerivaEstadoLivreMesmoSemLinhaDeVaga() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(java.util.List.of());
        VagaGestaoRepository repository = new VagaGestaoRepository(jdbc);

        repository.listarGradeSemanal(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-15"), "Endodontia", "LIVRE");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class));
        String consulta = sql.getValue().toUpperCase();
        assertTrue(consulta.contains("CROSS JOIN DIAS"));
        assertTrue(consulta.contains("LEFT JOIN VAGAS"));
        assertTrue(consulta.contains("DI.NOME = :DISCIPLINA"));
        assertTrue(consulta.contains("END = :SITUACAO"));
        assertTrue(consulta.contains("CASE"));
        assertTrue(consulta.contains("WHEN V.BLOQUEADA THEN 'BLOQUEADA'"));
        assertTrue(consulta.contains("ELSE 'LIVRE'"));
        assertTrue(consulta.contains("NOT IN ('CANCELADO_SECRETARIA', 'CANCELADO_PACIENTE')"));
    }

    @Test void gradeSemFiltrosNaoIncluiParametrosNulosSemTipo() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class)))
                .thenReturn(java.util.List.of());
        VagaGestaoRepository repository = new VagaGestaoRepository(jdbc);

        repository.listarGradeSemanal(LocalDate.parse("2026-11-09"), LocalDate.parse("2026-11-15"), null, null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), any(SqlParameterSource.class), any(org.springframework.jdbc.core.RowMapper.class));
        assertFalse(sql.getValue().contains(":disciplina"));
        assertFalse(sql.getValue().contains(":situacao"));
    }

    @Test void bloqueioMaterializaOuAtualizaPelaChaveDaVaga() {
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(23L);
        VagaGestaoRepository repository = new VagaGestaoRepository(jdbc);

        Long vagaId = repository.materializarBloqueio(2L, LocalDate.parse("2026-11-14"), "TARDE", "Manutenção");

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).queryForObject(sql.capture(), any(SqlParameterSource.class), eq(Long.class));
        String consulta = sql.getValue().toUpperCase();
        assertTrue(consulta.contains("ON CONFLICT (CADEIRA_ID, DATA, TURNO)"));
        assertTrue(consulta.contains("DO UPDATE SET BLOQUEADA = TRUE"));
        assertTrue(consulta.contains("RETURNING ID"));
        assertEquals(23L, vagaId);
    }
}
