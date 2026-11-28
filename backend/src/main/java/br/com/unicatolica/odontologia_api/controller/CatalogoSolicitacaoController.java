package br.com.unicatolica.odontologia_api.controller;

import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalogo-solicitacoes")
@PreAuthorize("hasAuthority('ALUNO')")
public class CatalogoSolicitacaoController {
    private final JdbcTemplate jdbc;
    private final SessaoAluno sessao;

    public CatalogoSolicitacaoController(JdbcTemplate jdbc, SessaoAluno sessao) {
        this.jdbc = jdbc;
        this.sessao = sessao;
    }

    @GetMapping("/turmas")
    public List<Map<String, Object>> turmas(@RequestParam(required = false) Long alunoId, HttpServletRequest request) {
        Long logado = sessao.exigirAluno(request);
        if (alunoId != null && !logado.equals(alunoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "A matrícula consultada não pertence ao aluno");
        }
        return jdbc.queryForList("""
            SELECT t.id, d.nome AS disciplina, t.codigo_turma AS "codigoTurma",
                   t.semestre_letivo AS semestre
            FROM matriculas_turma m
            JOIN turmas t ON t.id = m.turma_id
            JOIN disciplinas d ON d.id = t.disciplina_id
            WHERE m.aluno_id = ? AND t.ativa = TRUE AND d.ativo = TRUE
            ORDER BY d.nome, t.codigo_turma
            """, logado);
    }

    @GetMapping("/procedimentos")
    public List<Map<String, Object>> procedimentos(@RequestParam Long turmaId, HttpServletRequest request) {
        Long alunoId = sessao.exigirAluno(request);
        Integer matriculado = jdbc.queryForObject("""
            SELECT COUNT(*) FROM matriculas_turma m
            JOIN turmas t ON t.id = m.turma_id AND t.ativa = TRUE
            JOIN disciplinas d ON d.id = t.disciplina_id AND d.ativo = TRUE
            WHERE m.aluno_id = ? AND m.turma_id = ?
            """, Integer.class, alunoId, turmaId);
        if (matriculado == null || matriculado == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não está matriculado nesta turma");
        }
        return jdbc.queryForList("""
            SELECT DISTINCT p.id, p.nome, p.especialidade,
                   p.exige_dente AS "exigeDente", p.exige_classe AS "exigeClasse"
            FROM turmas t
            JOIN disciplina_procedimentos dp ON dp.disciplina_id = t.disciplina_id
            JOIN procedimentos p ON p.id = dp.procedimento_id
            WHERE t.id = ? AND dp.ativo = TRUE AND p.ativo = TRUE
            ORDER BY p.nome
            """, turmaId);
    }
}
