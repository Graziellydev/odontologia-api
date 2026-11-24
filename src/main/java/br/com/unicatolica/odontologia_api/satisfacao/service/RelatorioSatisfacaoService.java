package br.com.unicatolica.odontologia_api.satisfacao.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse;
import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse.Avaliacao;
import br.com.unicatolica.odontologia_api.satisfacao.dto.RelatorioSatisfacaoResponse.PorNota;
import br.com.unicatolica.odontologia_api.satisfacao.repository.RelatorioSatisfacaoRepository;
import br.com.unicatolica.odontologia_api.satisfacao.repository.RelatorioSatisfacaoRepository.Resumo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioSatisfacaoService {

    private static final Pattern FORMATO_SEMESTRE = Pattern.compile("^[0-9]{4}\\.[12]$");
    private static final int TAMANHO_MAXIMO = 50;

    private final RelatorioSatisfacaoRepository repository;

    @Transactional(readOnly = true)
    public RelatorioSatisfacaoResponse obterRelatorio(String semestre, int pagina, int tamanho) {
        String filtro = (semestre == null || semestre.isBlank()) ? null : semestre.trim();

        if (filtro != null) {
            if (!FORMATO_SEMESTRE.matcher(filtro).matches()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Semestre letivo inválido.");
            }
            if (!repository.listarSemestres().contains(filtro)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "O semestre letivo informado não está cadastrado.");
            }
        }

        int paginaSegura = Math.max(pagina, 0);
        int tamanhoSeguro = Math.min(Math.max(tamanho, 1), TAMANHO_MAXIMO);

        Resumo resumo = repository.resumo(filtro);
        Map<Integer, Long> contagem = repository.contagemPorNota(filtro);

        List<PorNota> distribuicao = new ArrayList<>();
        for (int nota = 5; nota >= 1; nota--) {
            distribuicao.add(new PorNota(nota, contagem.getOrDefault(nota, 0L)));
        }

        long deslocamento = (long) paginaSegura * tamanhoSeguro;
        List<Avaliacao> avaliacoes = repository.listar(filtro, tamanhoSeguro, deslocamento).stream()
                .map(linha -> new Avaliacao(
                        linha.nota(),
                        linha.dataResposta(),
                        linha.aluno(),
                        comentarioOuNulo(linha.comentario())))
                .toList();

        int totalPaginas = (int) Math.ceil((double) resumo.total() / tamanhoSeguro);

        return new RelatorioSatisfacaoResponse(
                filtro,
                resumo.media(),
                resumo.total(),
                List.copyOf(distribuicao),
                avaliacoes,
                paginaSegura,
                tamanhoSeguro,
                totalPaginas);
    }

    private static String comentarioOuNulo(String comentario) {
        return (comentario == null || comentario.isBlank()) ? null : comentario.trim();
    }
}