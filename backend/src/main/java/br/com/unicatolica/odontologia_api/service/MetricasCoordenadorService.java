package br.com.unicatolica.odontologia_api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.unicatolica.odontologia_api.dto.MetricasCoordenadorResponse;
import br.com.unicatolica.odontologia_api.dto.SemestresLetivosResponse;
import br.com.unicatolica.odontologia_api.exception.SemestreInvalidoException;
import br.com.unicatolica.odontologia_api.exception.SemestreNaoEncontradoException;
import br.com.unicatolica.odontologia_api.repository.MetricasCoordenadorRepository;
import br.com.unicatolica.odontologia_api.repository.MetricasCoordenadorRepository.LinhaAtendimento;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetricasCoordenadorService {

    private static final Pattern FORMATO_SEMESTRE = Pattern.compile("^[0-9]{4}\\.[12]$");
    private static final String ESPECIALIDADE_NAO_INFORMADA = "Não informada";

    private final MetricasCoordenadorRepository repository;

    @Transactional(readOnly = true)
    public SemestresLetivosResponse listarSemestres() {
        return new SemestresLetivosResponse(List.copyOf(repository.listarSemestres()));
    }

    @Transactional(readOnly = true)
    public MetricasCoordenadorResponse obterMetricas(String semestre) {
        List<LinhaAtendimento> linhas;
        if (semestre == null) {
            linhas = repository.buscarTodos();
        } else {
            if (!FORMATO_SEMESTRE.matcher(semestre).matches()) {
                throw new SemestreInvalidoException();
            }
            if (!repository.listarSemestres().contains(semestre)) {
                throw new SemestreNaoEncontradoException(semestre);
            }
            linhas = repository.buscarPorSemestre(semestre);
        }

        Map<String, Long> porSemestre = new TreeMap<>((a, b) -> b.compareTo(a));
        Map<String, Long> porDisciplina = new TreeMap<>();
        Map<String, Long> porEspecialidade = new TreeMap<>();
        long total = 0;

        for (LinhaAtendimento linha : linhas) {
            long concluidos = linha.totalConcluidos();
            if (concluidos <= 0) {
                continue;
            }

            total += concluidos;
            porSemestre.merge(linha.semestre(), concluidos, Long::sum);
            porDisciplina.merge(linha.disciplina(), concluidos, Long::sum);

            String especialidade = linha.especialidade() == null
                    ? ESPECIALIDADE_NAO_INFORMADA
                    : linha.especialidade();
            porEspecialidade.merge(especialidade, concluidos, Long::sum);
        }

        if (semestre != null && total == 0) {
            porSemestre.put(semestre, 0L);
        }

        return new MetricasCoordenadorResponse(
                semestre,
                total,
                mapearSemestres(porSemestre),
                mapearDisciplinas(porDisciplina),
                mapearEspecialidades(porEspecialidade));
    }

    private List<MetricasCoordenadorResponse.PorSemestre> mapearSemestres(Map<String, Long> totais) {
        List<MetricasCoordenadorResponse.PorSemestre> resultado = new ArrayList<>();
        totais.forEach((nome, total) -> resultado.add(new MetricasCoordenadorResponse.PorSemestre(nome, total)));
        return List.copyOf(resultado);
    }

    private List<MetricasCoordenadorResponse.PorDisciplina> mapearDisciplinas(Map<String, Long> totais) {
        List<MetricasCoordenadorResponse.PorDisciplina> resultado = new ArrayList<>();
        totais.forEach((nome, total) -> resultado.add(new MetricasCoordenadorResponse.PorDisciplina(nome, total)));
        return List.copyOf(resultado);
    }

    private List<MetricasCoordenadorResponse.PorEspecialidade> mapearEspecialidades(Map<String, Long> totais) {
        List<MetricasCoordenadorResponse.PorEspecialidade> resultado = new ArrayList<>();
        totais.forEach((nome, total) -> resultado.add(new MetricasCoordenadorResponse.PorEspecialidade(nome, total)));
        return List.copyOf(resultado);
    }
}