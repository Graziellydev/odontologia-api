package br.com.unicatolica.odontologia_api.solicitacoes.service;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.NovaSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.SolicitacaoAdministrativaDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.SolicitacaoAdministrativaProjection;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.SolicitacaoPacienteRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

@Service
public class SolicitacaoPacienteService {

    private final SolicitacaoPacienteRepository repository;
    private final FilaEsperaRepository filaEsperaRepository;
    private final JdbcTemplate jdbc;

    public SolicitacaoPacienteService(
            SolicitacaoPacienteRepository repository,
            FilaEsperaRepository filaEsperaRepository,
            JdbcTemplate jdbc) {

        this.repository = repository;
        this.filaEsperaRepository = filaEsperaRepository;
        this.jdbc = jdbc;
    }

    @Transactional
    public SolicitacaoPaciente criarSolicitacao(
            Long alunoId,
            NovaSolicitacaoDTO dados) {

        if (alunoId == null || alunoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aluno inválido.");
        }

        // Verifica se o aluno está matriculado em uma turma ativa.
        Integer matriculas = jdbc.queryForObject("""
            SELECT COUNT(*)
            FROM matriculas_turma m
            JOIN turmas t ON t.id = m.turma_id AND t.ativa = TRUE
            JOIN disciplinas d ON d.id = t.disciplina_id AND d.ativo = TRUE
            WHERE m.aluno_id = ? AND m.turma_id = ?
            """, Integer.class, alunoId, dados.turmaId());

                if (matriculas == null || matriculas == 0) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Aluno não matriculado em turma ativa.");
                }

        // Verifica se o procedimento existe e está ativo.
                List<Boolean> exigeClasse = jdbc.query("""
            SELECT exige_classe
            FROM procedimentos
            WHERE id = ? AND ativo = TRUE
            """,
                        (rs, i) -> rs.getBoolean("exige_classe"),
                        dados.procedimentoId());
        
                if (exigeClasse.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Procedimento inexistente ou inativo.");
                }

        // Verifica se o procedimento exige uma classe clínica.
                if (exigeClasse.get(0)
                        && (dados.classe() == null || dados.classe().isBlank())) {

                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "A classe é obrigatória para esse procedimento.");
                }

        SolicitacaoPaciente solicitacao = new SolicitacaoPaciente();

        solicitacao.setAlunoId(alunoId);
        solicitacao.setTurmaId(dados.turmaId());
        solicitacao.setProcedimentoId(dados.procedimentoId());
        solicitacao.setDente(dados.dente());
        solicitacao.setClasse(dados.classe());
        solicitacao.setDataDesejada(dados.dataDesejada());
        solicitacao.setObservacaoSecretaria(dados.observacaoSecretaria());
        solicitacao.setStatusSolicitacao("PENDENTE_APROVACAO");

        return repository.save(solicitacao);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoPaciente> listarPendentesSecretaria() {
        return repository
                .findByStatusSolicitacaoOrderByDataSolicitacaoAsc(
                        "PENDENTE_APROVACAO");
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoPaciente> listarSolicitacoesDoAluno(
            Long alunoId) {

        if (alunoId == null || alunoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aluno inválido.");
        }

        return repository
                .findByAlunoIdOrderByDataSolicitacaoDesc(alunoId);
    }

    @Transactional(readOnly = true)
    public List<SolicitacaoAdministrativaDTO> listarSolicitacoesAdministrativas() {
        return repository.listarAdministrativas().stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public SolicitacaoPaciente aprovarSolicitacao(
            Long solicitacaoId,
            Long filaEsperaId,
            Long secretariaId) {

        SolicitacaoPaciente solicitacao = buscarSolicitacao(solicitacaoId);

        if (!estaPendente(solicitacao.getStatusSolicitacao())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A solicitação não está mais pendente.");
        }

        FilaEspera entrada = filaEsperaRepository
                .findById(filaEsperaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Entrada da fila não encontrada."));

        if (!"AGUARDANDO".equalsIgnoreCase(entrada.getStatusFila())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O paciente escolhido não está mais aguardando.");
        }

        if (!Objects.equals(
                solicitacao.getProcedimentoId(),
                entrada.getProcedimentoId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O procedimento do paciente não corresponde à solicitação.");
        }

        if (!Objects.equals(
                solicitacao.getDente(),
                entrada.getDente())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O dente do paciente não corresponde à solicitação.");
        }

        if (solicitacao.getClasse() != null
                && !Objects.equals(
                        solicitacao.getClasse(),
                        entrada.getClasse())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A classe clínica não corresponde à solicitação.");
        }

        if (secretariaId == null || secretariaId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Secretaria responsável inválida.");
        }

        OffsetDateTime agora = OffsetDateTime.now();

        entrada.setStatusFila("ALOCADO");
        entrada.setDataSaidaFila(agora);

        filaEsperaRepository.save(entrada);

        solicitacao.setStatusSolicitacao("APROVADO_E_ALOCADO");
        solicitacao.setFilaEsperaId(entrada.getId());
        solicitacao.setPacienteAlocadoId(entrada.getPacienteId());
        solicitacao.setSecretariaAnalisadorId(secretariaId);
        solicitacao.setDataAnalise(agora);

        return repository.save(solicitacao);
    }

    @Transactional
    public SolicitacaoPaciente recusarSolicitacao(
            Long solicitacaoId,
            Long secretariaId,
            String observacao) {

        SolicitacaoPaciente solicitacao = buscarSolicitacao(solicitacaoId);

        if (!estaPendente(solicitacao.getStatusSolicitacao())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A solicitação não está mais pendente.");
        }

        if (secretariaId == null || secretariaId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Secretaria responsável inválida.");
        }

        if (observacao == null || observacao.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe o motivo da recusa.");
        }

        solicitacao.setStatusSolicitacao("RECUSADO");
        solicitacao.setRespostaSecretaria(observacao.trim());
        solicitacao.setSecretariaAnalisadorId(secretariaId);
        solicitacao.setDataAnalise(OffsetDateTime.now());

        return repository.save(solicitacao);
    }

    private SolicitacaoPaciente buscarSolicitacao(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Solicitação não encontrada."));
    }

    private boolean estaPendente(String status) {
        return "PENDENTE_APROVACAO".equals(status)
                || "PENDENTE_FILA".equals(status);
    }

    private SolicitacaoAdministrativaDTO converterParaDTO(
            SolicitacaoAdministrativaProjection projection) {
        return new SolicitacaoAdministrativaDTO(
                projection.getId(),
                projection.getAlunoId(),
                projection.getAlunoNome(),
                projection.getAlunoIdentificacao(),
                projection.getTurmaId(),
                projection.getTurma(),
                projection.getDisciplina(),
                projection.getSemestreAtual(),
                projection.getProcedimentoId(),
                projection.getProcedimento(),
                projection.getDente(),
                projection.getClasse(),
                projection.getDataDesejada(),
                projection.getStatusSolicitacao(),
                converterDataSolicitacao(projection.getDataSolicitacao()),
                projection.getPacienteAlocadoId(),
                projection.getPacienteAlocadoNome(),
                projection.getObservacaoSecretaria(),
                projection.getRespostaSecretaria(),
                projection.getSecretariaAnalisadorId(),
                converterDataSolicitacao(projection.getDataAnalise()),
                projection.getFilaEsperaId());
    }

    private OffsetDateTime converterDataSolicitacao(Instant data) {
        return data == null ? null : data.atOffset(ZoneOffset.UTC);
    }
}
