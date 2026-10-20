package br.com.unicatolica.odontologia_api.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.unicatolica.odontologia_api.dto.MetaProcedimentoResponse;
import br.com.unicatolica.odontologia_api.dto.PainelMetasResponse;
import br.com.unicatolica.odontologia_api.entity.ProgressoMetaAluno;
import br.com.unicatolica.odontologia_api.repository.ProgressoMetaAlunoRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class MetaAlunoService {
    private final ProgressoMetaAlunoRepository repository;

    @Transactional(readOnly = true)
    public PainelMetasResponse obterPainelDeMetas(Long alunoId, String semestreLetivo) {
        // Aqui eu busquei as linhas da view
        List<ProgressoMetaAluno> linhas;

        if(semestreLetivo == null) {
            linhas = repository.findByAlunoId(alunoId);
        } else {
            linhas = repository.findByAlunoIdAndSemestreLetivo(alunoId, semestreLetivo);
        }

        // Aqui eu percorro as linhas e guardo apenas as que possuem pendencias
        List<MetaProcedimentoResponse> pendencias = new ArrayList<>();

        for(ProgressoMetaAluno linha : linhas) {
            long saldo = Math.max(0L, linha.getQuantidadeExigida() - linha.getQuantidadeConcluida());
            
            if(saldo > 0) {
                pendencias.add(new MetaProcedimentoResponse(
                    linha.getProcedimento(),
                    linha.getDisciplina(),
                    linha.getQuantidadeExigida(),
                    linha.getQuantidadeConcluida(),
                    saldo));
            }
        }

        // Se nao tem pendencia, entao a meta foi cumprida
        boolean metaCumprida = pendencias.isEmpty();

        // Monta e devolve o painel
        return new PainelMetasResponse(alunoId, metaCumprida, pendencias);
    }
}