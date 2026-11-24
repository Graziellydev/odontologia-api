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

import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Modalidade;
import br.com.unicatolica.odontologia_api.exception.DesempenhoAcademicoException;
import br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException;
import br.com.unicatolica.odontologia_api.repository.DesempenhoAcademicoRepository;
import br.com.unicatolica.odontologia_api.repository.DesempenhoAcademicoRepository.LinhaDesempenho;
import br.com.unicatolica.odontologia_api.repository.MetricasCoordenadorRepository;

@ExtendWith(MockitoExtension.class)
class DesempenhoAcademicoServiceTest {

    @Mock
    private DesempenhoAcademicoRepository repository;

    @Mock
    private MetricasCoordenadorRepository metricasRepository;

    @InjectMocks
    private DesempenhoAcademicoService service;

    @Test
    void consolidaAlunoLimitandoConclusoesECalculandoPendencias() {
        when(metricasRepository.listarSemestres()).thenReturn(List.of("2026.1"));
        when(repository.buscar(Modalidade.ALUNO, "A001", "2026.1")).thenReturn(List.of(
                linha(1L, "Ana", "A001", 10L, "A", 11L, "Dentística", 21L, "Restauração", 2, 5),
                linha(1L, "Ana", "A001", 10L, "A", 12L, "Endodontia", 22L, "Endodontia", 3, 1)));

        DesempenhoAcademicoResponse resposta = service.consultar("ALUNO", " A001 ", "2026.1");

        assertEquals(Modalidade.ALUNO, resposta.modalidade());
        assertEquals(5, resposta.resumo().exigidos());
        assertEquals(3, resposta.resumo().concluidos());
        assertEquals(2, resposta.resumo().pendentes());
        assertEquals(60, resposta.resumo().progressoPercentual());
        assertEquals(2, resposta.historico().size());
        assertEquals(0, resposta.alunos().size());
        assertEquals(2, resposta.historico().getFirst().concluidos());
        verify(repository).buscar(Modalidade.ALUNO, "A001", "2026.1");
    }

    @Test
    void agrupaTurmasDoMesmoCodigoEPeriodoComUmaLinhaPorAluno() {
        when(metricasRepository.listarSemestres()).thenReturn(List.of("2026.1"));
        when(repository.buscar(Modalidade.TURMA, "A", "2026.1")).thenReturn(List.of(
                linha(1L, "Ana", "A001", 10L, "A", 11L, "Dentística", 21L, "Restauração", 2, 1),
                linha(1L, "Ana", "A001", 11L, "A", 12L, "Endodontia", 22L, "Endodontia", 3, 4),
                linha(2L, "Bia", "A002", 11L, "A", 12L, "Endodontia", 22L, "Endodontia", 3, 0)));

        DesempenhoAcademicoResponse resposta = service.consultar("TURMA", "A", "2026.1");

        assertEquals(8, resposta.resumo().exigidos());
        assertEquals(4, resposta.resumo().concluidos());
        assertEquals(4, resposta.resumo().pendentes());
        assertEquals(2, resposta.alunos().size());
        assertEquals("Ana", resposta.alunos().getFirst().nomeAluno());
        assertEquals(5, resposta.alunos().getFirst().exigidos());
        assertEquals(4, resposta.alunos().getFirst().concluidos());
        assertEquals(6, resposta.historico().get(1).exigidos());
    }

    @Test
    void consultaSemMetasRetornaIndicadoresZerados() {
        when(metricasRepository.listarSemestres()).thenReturn(List.of("2026.1"));
        when(repository.buscar(Modalidade.ALUNO, "A001", "2026.1")).thenReturn(List.of(
                new LinhaDesempenho(1L, "Ana", "A001", 10L, "A", "2026.1",
                        null, null, null, null, null, 0)));

        DesempenhoAcademicoResponse resposta = service.consultar("ALUNO", "A001", "2026.1");

        assertEquals(0, resposta.resumo().exigidos());
        assertEquals(0, resposta.resumo().progressoPercentual());
        assertEquals(List.of(), resposta.historico());
    }

    @Test
    void identificadorNaoLocalizadoRetornaMensagemGenerica() {
        when(metricasRepository.listarSemestres()).thenReturn(List.of("2026.1"));
        when(repository.buscar(Modalidade.ALUNO, "A999", "2026.1")).thenReturn(List.of());

        DesempenhoAcademicoException erro = assertThrows(DesempenhoAcademicoException.class,
                () -> service.consultar("ALUNO", "A999", "2026.1"));

        assertEquals(404, erro.getStatus().value());
        assertEquals("Nenhum registro foi localizado.", erro.getMessage());
    }

    @Test
    void validaModalidadeIdentificadorESemestreAntesDaConsulta() {
        DesempenhoAcademicoException modalidade = assertThrows(DesempenhoAcademicoException.class,
                () -> service.consultar("OUTRO", "A001", "2026.1"));
        assertEquals("MODALIDADE_INVALIDA", modalidade.getCodigo());

        DesempenhoAcademicoException identificador = assertThrows(DesempenhoAcademicoException.class,
                () -> service.consultar("ALUNO", " ", "2026.1"));
        assertEquals("IDENTIFICADOR_OBRIGATORIO", identificador.getCodigo());

        assertThrows(SemestreInvalidoException.class,
                () -> service.consultar("ALUNO", "A001", "2026.3"));
    }

    private LinhaDesempenho linha(
            Long alunoId,
            String nome,
            String matricula,
            Long turmaId,
            String codigoTurma,
            Long disciplinaId,
            String disciplina,
            Long procedimentoId,
            String procedimento,
            Integer exigidos,
            long concluidos) {
        return new LinhaDesempenho(alunoId, nome, matricula, turmaId, codigoTurma, "2026.1",
                disciplinaId, disciplina, procedimentoId, procedimento, exigidos, concluidos);
    }
}
