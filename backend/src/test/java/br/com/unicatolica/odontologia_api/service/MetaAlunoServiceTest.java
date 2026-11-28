package br.com.unicatolica.odontologia_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.dto.PainelMetasResponse;
import br.com.unicatolica.odontologia_api.entity.ProgressoMetaAluno;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.ProgressoMetaAlunoRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class MetaAlunoServiceTest {

    @Mock
    private ProgressoMetaAlunoRepository repository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private MetaAlunoService service;

    @BeforeEach
    void prepararAluno() {
        Usuario aluno = new Usuario();
        aluno.setNome("Mariana Costa");
        lenient().when(usuarioRepository.findById(1L)).thenReturn(Optional.of(aluno));
    }

    // Cria uma linha da view, já que a entity não tem setters
    private ProgressoMetaAluno linha(long disciplinaId, String disciplina,
                                     String procedimento, int exigida, long concluida) {
        ProgressoMetaAluno l = new ProgressoMetaAluno();
        ReflectionTestUtils.setField(l, "disciplinaId", disciplinaId);
        ReflectionTestUtils.setField(l, "disciplina", disciplina);
        ReflectionTestUtils.setField(l, "procedimento", procedimento);
        ReflectionTestUtils.setField(l, "quantidadeExigida", exigida);
        ReflectionTestUtils.setField(l, "quantidadeConcluida", concluida);
        return l;
    }

    @Test
    void deveCalcularTotaisQuandoHaSaldo() {
        when(repository.findByAlunoId(1L))
                .thenReturn(List.of(linha(1L, "Odontológica II", "Endodontia", 5, 2)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertEquals("Mariana Costa", painel.nomeAluno());
        assertEquals(5L, painel.totalExigido());
        assertEquals(2L, painel.totalConcluido());
        assertEquals(3L, painel.saldoPendente());
        assertEquals(40, painel.percentualConcluido());
        assertFalse(painel.metaCumprida());
        assertEquals(1, painel.procedimentos().size());
        assertEquals(3L, painel.procedimentos().get(0).saldoPendente());
    }

    @Test
    void deveIndicarMetaCumpridaQuandoSaldoEhZero() {
        when(repository.findByAlunoId(1L))
                .thenReturn(List.of(linha(1L, "Odontológica II", "Endodontia", 3, 3)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertTrue(painel.metaCumprida());
        assertEquals(100, painel.percentualConcluido());
        assertEquals(0L, painel.saldoPendente());
    }

    @Test
    void naoDevePassarDeCemPorCentoQuandoConcluiuMaisQueOExigido() {
        when(repository.findByAlunoId(1L))
                .thenReturn(List.of(linha(1L, "Odontológica I", "Raspagem periodontal", 2, 4)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertEquals(0L, painel.saldoPendente());
        assertEquals(2L, painel.totalConcluido());
        assertEquals(100, painel.percentualConcluido());
        assertTrue(painel.metaCumprida());
        // na linha do procedimento aparece o número real
        assertEquals(4L, painel.procedimentos().get(0).quantidadeConcluida());
    }

    @Test
    void deveAgruparTotaisPorDisciplina() {
        when(repository.findByAlunoId(1L)).thenReturn(List.of(
                linha(1L, "Odontológica II", "Endodontia", 5, 2),
                linha(1L, "Odontológica II", "Restauração", 4, 4),
                linha(2L, "Odontológica III", "Cirurgia", 3, 0)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertEquals(12L, painel.totalExigido());
        assertEquals(6L, painel.totalConcluido());
        assertEquals(6L, painel.saldoPendente());
        assertEquals(50, painel.percentualConcluido());
        assertEquals(3, painel.procedimentos().size());
        assertEquals(2, painel.progressoPorDisciplina().size());
        assertEquals(9L, painel.progressoPorDisciplina().get(0).totalExigido());
        assertEquals(6L, painel.progressoPorDisciplina().get(0).totalConcluido());
        assertEquals(3L, painel.progressoPorDisciplina().get(0).saldoPendente());
    }

    @Test
    void deveFiltrarPorSemestreQuandoInformado() {
        when(repository.findByAlunoIdAndSemestreLetivo(1L, "2026.1"))
                .thenReturn(List.of());

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, "2026.1");

        verify(repository).findByAlunoIdAndSemestreLetivo(1L, "2026.1");
        assertEquals("2026.1", painel.semestreLetivo());
        assertEquals(0, painel.percentualConcluido());
    }

    @Test
    void deveLancarErroQuandoAlunoNaoExiste() {
        // 99L não foi configurado no mock, então o findById devolve vazio
        assertThrows(ResponseStatusException.class,
                () -> service.obterPainelDeMetas(99L, null));
    }
}