package br.com.unicatolica.odontologia_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.unicatolica.odontologia_api.dto.PainelMetasResponse;
import br.com.unicatolica.odontologia_api.entity.ProgressoMetaAluno;
import br.com.unicatolica.odontologia_api.repository.ProgressoMetaAlunoRepository;

@ExtendWith(MockitoExtension.class)
class MetaAlunoServiceTest {

    @Mock
    private ProgressoMetaAlunoRepository repository;

    @InjectMocks
    private MetaAlunoService service;

    // Cria uma linha da view, já que a entity não tem setters
    private ProgressoMetaAluno linha(String procedimento, int exigida, long concluida) {
        ProgressoMetaAluno l = new ProgressoMetaAluno();
        ReflectionTestUtils.setField(l, "procedimento", procedimento);
        ReflectionTestUtils.setField(l, "disciplina", "Clínica I");
        ReflectionTestUtils.setField(l, "quantidadeExigida", exigida);
        ReflectionTestUtils.setField(l, "quantidadeConcluida", concluida);
        return l;
    }

    @Test
    void deveExibirPendenciasQuandoHaSaldo() {
        when(repository.findByAlunoId(1L))
                .thenReturn(List.of(linha("Endodontia", 5, 2)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertFalse(painel.metaCumprida());
        assertEquals(1, painel.pendencias().size());
        assertEquals(3L, painel.pendencias().get(0).saldoPendente());
    }

    @Test
    void deveIndicarMetaCumpridaQuandoSaldoEhZero() {
        when(repository.findByAlunoId(1L))
                .thenReturn(List.of(linha("Endodontia", 3, 3)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertTrue(painel.metaCumprida());
        assertTrue(painel.pendencias().isEmpty());
    }

    @Test
    void naoDeveGerarSaldoNegativoQuandoConcluiuMaisQueOExigido() {
        when(repository.findByAlunoId(1L))
                .thenReturn(List.of(linha("Periodontia", 2, 4)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertTrue(painel.metaCumprida());
        assertTrue(painel.pendencias().isEmpty());
    }

    @Test
    void deveListarSoOsProcedimentosComPendencia() {
        when(repository.findByAlunoId(1L)).thenReturn(List.of(
                linha("Endodontia", 5, 2),
                linha("Periodontia", 2, 2)));

        PainelMetasResponse painel = service.obterPainelDeMetas(1L, null);

        assertFalse(painel.metaCumprida());
        assertEquals(1, painel.pendencias().size());
        assertEquals("Endodontia", painel.pendencias().get(0).procedimento());
    }

    @Test
    void deveFiltrarPorSemestreQuandoInformado() {
        when(repository.findByAlunoIdAndSemestreLetivo(1L, "2026.1"))
                .thenReturn(List.of());

        service.obterPainelDeMetas(1L, "2026.1");

        verify(repository).findByAlunoIdAndSemestreLetivo(1L, "2026.1");
    }
}