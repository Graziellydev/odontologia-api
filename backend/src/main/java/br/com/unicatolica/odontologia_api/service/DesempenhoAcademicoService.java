package br.com.unicatolica.odontologia_api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.AlunoComparativo;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.LinhaHistorico;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Modalidade;
import br.com.unicatolica.odontologia_api.dto.DesempenhoAcademicoResponse.Resumo;
import br.com.unicatolica.odontologia_api.exception.DesempenhoAcademicoException;
import br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException;
import br.com.unicatolica.odontologia_api.exception.SemestreNaoEncontradoException;
import br.com.unicatolica.odontologia_api.repository.DesempenhoAcademicoRepository;
import br.com.unicatolica.odontologia_api.repository.DesempenhoAcademicoRepository.LinhaDesempenho;
import br.com.unicatolica.odontologia_api.repository.MetricasCoordenadorRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DesempenhoAcademicoService {

    private static final Pattern FORMATO_SEMESTRE = Pattern.compile("^[0-9]{4}\\.[12]$");
    private static final int TAMANHO_MAXIMO_IDENTIFICADOR = 50;

    private final DesempenhoAcademicoRepository repository;
    private final MetricasCoordenadorRepository metricasRepository;

    @Transactional(readOnly = true)
    public DesempenhoAcademicoResponse consultar(String modalidade, String identificador, String semestre) {
        Modalidade modo = interpretarModalidade(modalidade);
        String id = validarIdentificador(identificador);
        validarSemestre(semestre);

        List<LinhaDesempenho> linhas = repository.buscar(modo, id, semestre);
        if (linhas.isEmpty() || modo == Modalidade.ALUNO && linhas.getFirst().alunoId() == null) {
            throw naoEncontrado();
        }

        if (modo == Modalidade.ALUNO) {
            return respostaAluno(id, semestre, linhas);
        }
        return respostaTurma(id, semestre, linhas);
    }

    private Modalidade interpretarModalidade(String modalidade) {
        if (modalidade == null || modalidade.isBlank()) {
            throw new DesempenhoAcademicoException(
                    HttpStatus.BAD_REQUEST, "MODALIDADE_INVALIDA", "Selecione Aluno ou Turma.");
        }
        try {
            return Modalidade.valueOf(modalidade.trim());
        } catch (IllegalArgumentException exception) {
            throw new DesempenhoAcademicoException(
                    HttpStatus.BAD_REQUEST, "MODALIDADE_INVALIDA", "Selecione Aluno ou Turma.");
        }
    }

    private String validarIdentificador(String identificador) {
        if (identificador == null || identificador.isBlank()) {
            throw new DesempenhoAcademicoException(
                    HttpStatus.BAD_REQUEST, "IDENTIFICADOR_OBRIGATORIO", "Informe a matrícula ou o código da turma.");
        }
        String valor = identificador.trim();
        if (valor.length() > TAMANHO_MAXIMO_IDENTIFICADOR) {
            throw new DesempenhoAcademicoException(
                    HttpStatus.BAD_REQUEST, "IDENTIFICADOR_INVALIDO", "O identificador informado é inválido.");
        }
        return valor;
    }

    private void validarSemestre(String semestre) {
        if (semestre == null || !FORMATO_SEMESTRE.matcher(semestre).matches()) {
            throw new SemestreInvalidoException();
        }
        if (!metricasRepository.listarSemestres().contains(semestre)) {
            throw new SemestreNaoEncontradoException(semestre);
        }
    }

    private DesempenhoAcademicoResponse respostaAluno(
            String identificador, String semestre, List<LinhaDesempenho> linhas) {
        LinhaDesempenho aluno = linhas.getFirst();
        List<LinhaHistorico> historico = historico(linhas);
        return new DesempenhoAcademicoResponse(
                Modalidade.ALUNO,
                identificador,
                semestre,
                aluno.nomeAluno(),
                aluno.matriculaAluno(),
                null,
                resumo(historico),
                historico,
                List.of());
    }

    private DesempenhoAcademicoResponse respostaTurma(
            String identificador, String semestre, List<LinhaDesempenho> linhas) {
        List<LinhaHistorico> historico = historico(linhas);
        Map<Long, AlunoAcumulado> porAluno = new LinkedHashMap<>();

        for (LinhaDesempenho linha : linhas) {
            if (linha.alunoId() == null) {
                continue;
            }
            AlunoAcumulado aluno = porAluno.computeIfAbsent(linha.alunoId(),
                    ignored -> new AlunoAcumulado(linha.nomeAluno(), linha.matriculaAluno()));
            aluno.adicionar(linha);
        }

        List<AlunoComparativo> alunos = porAluno.values().stream()
                .map(AlunoAcumulado::paraResposta)
                .sorted(Comparator.comparing(AlunoComparativo::nomeAluno, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AlunoComparativo::matriculaAluno, Comparator.nullsLast(String::compareTo)))
                .toList();

        return new DesempenhoAcademicoResponse(
                Modalidade.TURMA,
                identificador,
                semestre,
                null,
                null,
                identificador,
                resumo(historico),
                historico,
                alunos);
    }

    private List<LinhaHistorico> historico(List<LinhaDesempenho> linhas) {
        Map<ChaveHistorico, Acumulado> porProcedimento = new LinkedHashMap<>();
        for (LinhaDesempenho linha : linhas) {
            if (linha.procedimentoId() == null || linha.quantidadeExigida() == null) {
                continue;
            }
            ChaveHistorico chave = new ChaveHistorico(
                    linha.disciplinaId(), linha.disciplina(),
                    linha.procedimentoId(), linha.procedimento());
            Acumulado atual = porProcedimento.computeIfAbsent(chave, ignored -> new Acumulado());
            atual.adicionar(
                    linha.quantidadeExigida(),
                    Math.min(linha.quantidadeConcluida(), linha.quantidadeExigida()));
        }

        return porProcedimento.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator
                        .comparing(ChaveHistorico::disciplina, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ChaveHistorico::procedimento, String.CASE_INSENSITIVE_ORDER)))
                .map(entry -> {
                    Acumulado valor = entry.getValue();
                    long pendentes = Math.max(0, valor.exigidos - valor.concluidos);
                    return new LinhaHistorico(
                            entry.getKey().disciplina(),
                            entry.getKey().procedimento(),
                            valor.exigidos,
                            valor.concluidos,
                            pendentes,
                            percentual(valor.exigidos, valor.concluidos));
                })
                .toList();
    }

    private Resumo resumo(List<LinhaHistorico> historico) {
        long exigidos = 0;
        long concluidos = 0;
        for (LinhaHistorico linha : historico) {
            exigidos += linha.exigidos();
            concluidos += linha.concluidos();
        }
        return new Resumo(exigidos, concluidos, Math.max(0, exigidos - concluidos),
                percentual(exigidos, concluidos));
    }

    private static int percentual(long exigidos, long concluidos) {
        if (exigidos == 0) {
            return 0;
        }
        return BigDecimal.valueOf(concluidos)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(exigidos), 0, RoundingMode.DOWN)
                .intValue();
    }

    private static DesempenhoAcademicoException naoEncontrado() {
        return new DesempenhoAcademicoException(
                HttpStatus.NOT_FOUND,
                "REGISTRO_NAO_ENCONTRADO",
                "Nenhum registro foi localizado.");
    }

    private record ChaveHistorico(
            Long disciplinaId,
            String disciplina,
            Long procedimentoId,
            String procedimento) {
    }

    private static class Acumulado {
        protected long exigidos;
        protected long concluidos;

        protected void adicionar(long quantidadeExigida, long quantidadeConcluida) {
            exigidos += quantidadeExigida;
            concluidos += quantidadeConcluida;
        }
    }

    private static final class AlunoAcumulado extends Acumulado {
        private final String nome;
        private final String matricula;

        private AlunoAcumulado(String nome, String matricula) {
            this.nome = nome;
            this.matricula = matricula;
        }

        private void adicionar(LinhaDesempenho linha) {
            if (linha.procedimentoId() != null && linha.quantidadeExigida() != null) {
                super.adicionar(linha.quantidadeExigida(),
                        Math.min(linha.quantidadeConcluida(), linha.quantidadeExigida()));
            }
        }

        private AlunoComparativo paraResposta() {
            long pendentes = Math.max(0, exigidos - concluidos);
            return new AlunoComparativo(
                    nome, matricula, exigidos, concluidos, pendentes, percentual(exigidos, concluidos));
        }
    }
}
