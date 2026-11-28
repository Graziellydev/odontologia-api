package br.com.unicatolica.odontologia_api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.unicatolica.odontologia_api.dto.MetricasCoordenadorResponse;
import br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException;
import br.com.unicatolica.odontologia_api.exception.SemestreNaoEncontradoException;
import br.com.unicatolica.odontologia_api.repository.MetricasCoordenadorRepository;
import br.com.unicatolica.odontologia_api.repository.MetricasCoordenadorRepository.LinhaAtendimento;

@ExtendWith(MockitoExtension.class)
class MetricasCoordenadorServiceTest {

    @Mock
    private MetricasCoordenadorRepository repository;

    @InjectMocks
    private MetricasCoordenadorService service;

    @Test
    void deveSomarTotalEAgruparPorSemestreDisciplinaEEspecialidade() {
        when(repository.buscarTodos()).thenReturn(List.of(
                linha("2026.2", "Dentística", "Dentística", 2),
                linha("2026.2", "Endodontia", "Endodontia", 1),
                linha("2026.1", "Dentística", "Dentística", 1),
                linha("2026.1", "Dentística", null, 1)));

        MetricasCoordenadorResponse resposta = service.obterMetricas(null);

        assertEquals(5, resposta.totalAtendimentosConcluidos());
        assertEquals(List.of(
                new MetricasCoordenadorResponse.PorSemestre("2026.2", 3),
                new MetricasCoordenadorResponse.PorSemestre("2026.1", 2)), resposta.porSemestre());
        assertEquals(List.of(
                new MetricasCoordenadorResponse.PorDisciplina("Dentística", 4),
                new MetricasCoordenadorResponse.PorDisciplina("Endodontia", 1)), resposta.porDisciplina());
        assertEquals(List.of(
                new MetricasCoordenadorResponse.PorEspecialidade("Dentística", 3),
                new MetricasCoordenadorResponse.PorEspecialidade("Endodontia", 1),
                new MetricasCoordenadorResponse.PorEspecialidade("Não informada", 1)), resposta.porEspecialidade());
    }

    @Test
    void deveAplicarFiltroDoSemestreAConsultaEAResposta() {
        when(repository.listarSemestres()).thenReturn(List.of("2026.2", "2026.1"));
        when(repository.buscarPorSemestre("2026.1"))
                .thenReturn(List.of(linha("2026.1", "Dentística", "Dentística", 2)));

        MetricasCoordenadorResponse resposta = service.obterMetricas("2026.1");

        verify(repository).buscarPorSemestre("2026.1");
        assertEquals("2026.1", resposta.semestre());
        assertEquals(2, resposta.totalAtendimentosConcluidos());
        assertEquals(List.of(new MetricasCoordenadorResponse.PorSemestre("2026.1", 2)), resposta.porSemestre());
        assertEquals(List.of(new MetricasCoordenadorResponse.PorDisciplina("Dentística", 2)), resposta.porDisciplina());
        assertEquals(List.of(new MetricasCoordenadorResponse.PorEspecialidade("Dentística", 2)), resposta.porEspecialidade());
    }

    @Test
    void ausenciaDoFiltroConsultaTodosOsPeriodos() {
        when(repository.buscarTodos()).thenReturn(List.of(linha("2026.1", "Dentística", "Dentística", 4)));

        service.obterMetricas(null);

        verify(repository).buscarTodos();
    }

    @Test
    void deveRetornarSemestreComZeroEGruposVaziosQuandoNaoHaConcluidos() {
        when(repository.listarSemestres()).thenReturn(List.of("2026.2", "2026.1"));
        when(repository.buscarPorSemestre("2026.1")).thenReturn(List.of(
                linha("2026.1", "Dentística", "Dentística", 0),
                linha("2026.1", "Endodontia", null, 0)));

        MetricasCoordenadorResponse resposta = service.obterMetricas("2026.1");

        assertEquals(0, resposta.totalAtendimentosConcluidos());
        assertEquals(List.of(new MetricasCoordenadorResponse.PorSemestre("2026.1", 0)), resposta.porSemestre());
        assertEquals(List.of(), resposta.porDisciplina());
        assertEquals(List.of(), resposta.porEspecialidade());
    }

    @Test
    void todosOsPeriodosSemConcluidosRetornaTotalZeroEGruposVazios() {
        when(repository.buscarTodos()).thenReturn(List.of(linha("2026.1", "Dentística", null, 0)));

        MetricasCoordenadorResponse resposta = service.obterMetricas(null);

        assertEquals(0, resposta.totalAtendimentosConcluidos());
        assertEquals(List.of(), resposta.porSemestre());
        assertEquals(List.of(), resposta.porDisciplina());
        assertEquals(List.of(), resposta.porEspecialidade());
    }

    @Test
    void deveIgnorarGruposDeDisciplinaEEspecialidadeComTotalZero() {
        when(repository.buscarTodos()).thenReturn(List.of(
                linha("2026.2", "Dentística", "Dentística", 2),
                linha("2026.1", "Endodontia", null, 0)));

        MetricasCoordenadorResponse resposta = service.obterMetricas(null);

        assertEquals(1, resposta.porDisciplina().size());
        assertEquals(1, resposta.porEspecialidade().size());
        assertEquals(1, resposta.porSemestre().size());
    }

    @Test
    void somatorioDeCadaAgrupamentoDeveCorresponderAoTotalGeral() {
        when(repository.buscarTodos()).thenReturn(List.of(
                linha("2026.2", "Dentística", "Dentística", 2),
                linha("2026.1", "Endodontia", "Endodontia", 3)));

        MetricasCoordenadorResponse resposta = service.obterMetricas(null);

        assertEquals(resposta.totalAtendimentosConcluidos(),
                resposta.porSemestre().stream().mapToLong(MetricasCoordenadorResponse.PorSemestre::total).sum());
        assertEquals(resposta.totalAtendimentosConcluidos(),
                resposta.porDisciplina().stream().mapToLong(MetricasCoordenadorResponse.PorDisciplina::total).sum());
        assertEquals(resposta.totalAtendimentosConcluidos(),
                resposta.porEspecialidade().stream().mapToLong(MetricasCoordenadorResponse.PorEspecialidade::total).sum());
    }

    @Test
    void deveRejeitarFormatoDeSemestreInvalido() {
        assertThrows(SemestreInvalidoException.class, () -> service.obterMetricas("2026.3"));
    }

    @Test
    void deveDiferenciarSemestreValidoNaoCadastrado() {
        when(repository.listarSemestres()).thenReturn(List.of("2026.2"));

        assertThrows(SemestreNaoEncontradoException.class, () -> service.obterMetricas("2026.1"));
    }

    private LinhaAtendimento linha(String semestre, String disciplina, String especialidade, long total) {
        return new LinhaAtendimento(semestre, disciplina, especialidade, total);
    }
}