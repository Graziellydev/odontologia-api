package br.com.unicatolica.odontologia_api.satisfacao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse;
import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse.PorNota;
import br.com.unicatolica.odontologia_api.satisfacao.repository.RelatorioSatisfacaoRepository;
import br.com.unicatolica.odontologia_api.satisfacao.repository.RelatorioSatisfacaoRepository.LinhaAvaliacao;
import br.com.unicatolica.odontologia_api.satisfacao.repository.RelatorioSatisfacaoRepository.Resumo;

@ExtendWith(MockitoExtension.class)
class RelatorioSatisfacaoServiceTest {

    @Mock
    private RelatorioSatisfacaoRepository repository;

    @InjectMocks
    private RelatorioSatisfacaoService service;

    private static LinhaAvaliacao linha(int nota, String aluno, String comentario) {
        return new LinhaAvaliacao(nota, OffsetDateTime.parse("2026-11-20T10:00:00-03:00"), aluno, comentario);
    }

    @Test
    void deveCalcularMediaDistribuicaoEPaginas() {
        when(repository.resumo(null)).thenReturn(new Resumo(25, 4.52));
        when(repository.contagemPorNota(null)).thenReturn(Map.of(5, 15L, 4, 6L, 3, 4L));
        when(repository.listar(null, 10, 0L)).thenReturn(List.of(linha(5, "Aluno Um", "Ótimo")));

        RelatorioSatisfacaoResponse resposta = service.obterRelatorio(null, 0, 10);

        assertEquals(4.52, resposta.mediaGeral(), 0.001);
        assertEquals(25L, resposta.totalRespostas());
        assertEquals(3, resposta.totalPaginas());
        assertEquals(List.of(5, 4, 3, 2, 1), resposta.distribuicao().stream().map(PorNota::nota).toList());
        assertEquals(List.of(15L, 6L, 4L, 0L, 0L), resposta.distribuicao().stream().map(PorNota::total).toList());
        assertEquals("Aluno Um", resposta.avaliacoes().get(0).aluno());
    }

    @Test
    void semRespostasNaoTemMediaNemPaginas() {
        when(repository.resumo(null)).thenReturn(new Resumo(0, null));

        RelatorioSatisfacaoResponse resposta = service.obterRelatorio(null, 0, 10);

        assertNull(resposta.mediaGeral());
        assertEquals(0L, resposta.totalRespostas());
        assertEquals(0, resposta.totalPaginas());
        assertEquals(0, resposta.avaliacoes().size());
        assertEquals(5, resposta.distribuicao().size());
    }

    @Test
    void deveLimitarTamanhoEPaginaInvalidos() {
        when(repository.resumo(null)).thenReturn(new Resumo(0, null));

        RelatorioSatisfacaoResponse resposta = service.obterRelatorio(null, -3, 500);

        assertEquals(0, resposta.pagina());
        assertEquals(50, resposta.tamanho());
        verify(repository).listar(null, 50, 0L);
    }

    @Test
    void semestreEmBrancoEquivaleATodosOsPeriodos() {
        when(repository.resumo(null)).thenReturn(new Resumo(0, null));

        service.obterRelatorio("   ", 0, 10);

        verify(repository).resumo(null);
        verify(repository, never()).listarSemestres();
    }

    @Test
    void deveRecusarSemestreComFormatoInvalido() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.obterRelatorio("2026-2", 0, 10));

        assertEquals(400, erro.getStatusCode().value());
        verifyNoInteractions(repository);
    }

    @Test
    void deveRecusarSemestreNaoCadastrado() {
        when(repository.listarSemestres()).thenReturn(List.of("2026.2"));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.obterRelatorio("2025.1", 0, 10));

        assertEquals(404, erro.getStatusCode().value());
        verify(repository, never()).resumo(any());
    }

    @Test
    void deveAceitarSemestreCadastrado() {
        when(repository.listarSemestres()).thenReturn(List.of("2026.2", "2026.1"));
        when(repository.resumo("2026.2")).thenReturn(new Resumo(0, null));

        RelatorioSatisfacaoResponse resposta = service.obterRelatorio("2026.2", 0, 10);

        assertEquals("2026.2", resposta.semestre());
        verify(repository).resumo("2026.2");
    }

    @Test
    void comentarioEmBrancoContaComoAusente() {
        when(repository.resumo(null)).thenReturn(new Resumo(2, 5.0));
        when(repository.listar(null, 10, 0L)).thenReturn(List.of(
                linha(5, "Aluno Um", "   "),
                linha(5, "Aluno Um", "  Muito bom  ")));

        RelatorioSatisfacaoResponse resposta = service.obterRelatorio(null, 0, 10);

        assertNull(resposta.avaliacoes().get(0).comentario());
        assertEquals("Muito bom", resposta.avaliacoes().get(1).comentario());
    }

    @Test
    void relatorioNaoExpoeDadosDoPaciente() {
        boolean expoePaciente = Arrays.stream(RelatorioSatisfacaoResponse.Avaliacao.class.getRecordComponents())
                .anyMatch(componente -> componente.getName().toLowerCase().contains("paciente"));

        assertFalse(expoePaciente);
    }
}