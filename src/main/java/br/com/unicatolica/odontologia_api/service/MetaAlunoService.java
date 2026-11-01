package br.com.unicatolica.odontologia_api.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.dto.MetaProcedimentoResponse;
import br.com.unicatolica.odontologia_api.dto.PainelMetasResponse;
import br.com.unicatolica.odontologia_api.dto.ProgressoDisciplinaResponse;
import br.com.unicatolica.odontologia_api.entity.ProgressoMetaAluno;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.ProgressoMetaAlunoRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetaAlunoService {

    private final ProgressoMetaAlunoRepository repository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public PainelMetasResponse obterPainelDeMetas(Long alunoId, String semestreLetivo) {

        // Passo 1: o aluno precisa existir
        Usuario aluno = usuarioRepository.findById(alunoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aluno não encontrado"));

        // Passo 2: buscar as linhas da view
        List<ProgressoMetaAluno> linhas;
        if (semestreLetivo == null) {
            linhas = repository.findByAlunoId(alunoId);
        } else {
            linhas = repository.findByAlunoIdAndSemestreLetivo(alunoId, semestreLetivo);
        }

        // TODO: alunos do 5 semestre (Semiologia) fazem apenas triagem. Se a coordenacao
        // definir meta de triagens, ela entra como o procedimento "Triagem" em
        // disciplina_procedimentos, sem mudar este codigo. Se NAO houver meta, a lista
        // chega vazia e o painel responderia "meta cumprida", o que e enganoso.
        // Tratar esse caso quando a secretaria responder.

        // Passo 3: percorrer as linhas, somando os totais
        List<MetaProcedimentoResponse> procedimentos = new ArrayList<>();
        Map<Long, ProgressoDisciplinaResponse> porDisciplina = new LinkedHashMap<>();
        long totalExigido = 0;
        long totalConcluido = 0;
        long totalSaldo = 0;

        for (ProgressoMetaAluno linha : linhas) {
            long exigida = linha.getQuantidadeExigida();
            long concluida = linha.getQuantidadeConcluida();
            long saldo = Math.max(0L, exigida - concluida);
            // nos totais, o concluido nunca passa da meta do procedimento
            long contabilizado = Math.min(concluida, exigida);

            procedimentos.add(new MetaProcedimentoResponse(
                    linha.getProcedimento(),
                    linha.getDisciplina(),
                    linha.getQuantidadeExigida(),
                    linha.getQuantidadeConcluida(),
                    saldo));

            totalExigido += exigida;
            totalConcluido += contabilizado;
            totalSaldo += saldo;

            // agrupa por disciplina, somando com o que ja existe
            ProgressoDisciplinaResponse atual = porDisciplina.get(linha.getDisciplinaId());
            long exigidoDisc = exigida + (atual == null ? 0 : atual.totalExigido());
            long concluidoDisc = contabilizado + (atual == null ? 0 : atual.totalConcluido());
            long saldoDisc = saldo + (atual == null ? 0 : atual.saldoPendente());
            porDisciplina.put(linha.getDisciplinaId(), new ProgressoDisciplinaResponse(
                    linha.getDisciplinaId(), linha.getDisciplina(),
                    exigidoDisc, concluidoDisc, saldoDisc));
        }

        // Passo 4: percentual e meta cumprida
        int percentual = totalExigido == 0 ? 0 : (int) (totalConcluido * 100 / totalExigido);
        boolean metaCumprida = totalSaldo == 0;

        return new PainelMetasResponse(
                alunoId,
                aluno.getNome(),
                semestreLetivo,
                totalExigido,
                totalConcluido,
                totalSaldo,
                percentual,
                metaCumprida,
                new ArrayList<>(porDisciplina.values()),
                procedimentos);
    }
}