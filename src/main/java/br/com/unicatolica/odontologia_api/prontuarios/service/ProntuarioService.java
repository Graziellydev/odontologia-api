package br.com.unicatolica.odontologia_api.prontuarios.service;

import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.StatusConsulta;
import br.com.unicatolica.odontologia_api.consultas.repository.ConsultaRepository;
import br.com.unicatolica.odontologia_api.notificacoes.service.NotificacaoConclusaoService;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.prontuarios.dto.ConsultaClinica;
import br.com.unicatolica.odontologia_api.prontuarios.dto.ProntuarioHistoricoResponse;
import br.com.unicatolica.odontologia_api.prontuarios.dto.RegistrarAdendoRequest;
import br.com.unicatolica.odontologia_api.prontuarios.dto.RegistrarProntuarioRequest;
import br.com.unicatolica.odontologia_api.prontuarios.dto.SalvarAdendoResponse;
import br.com.unicatolica.odontologia_api.prontuarios.dto.SalvarProntuarioResponse;
import br.com.unicatolica.odontologia_api.prontuarios.entity.Prontuario;
import br.com.unicatolica.odontologia_api.prontuarios.entity.ProntuarioAdendo;
import br.com.unicatolica.odontologia_api.prontuarios.repository.ProntuarioAdendoJpaRepository;
import br.com.unicatolica.odontologia_api.prontuarios.repository.ProntuarioJpaRepository;
import br.com.unicatolica.odontologia_api.prontuarios.repository.ProntuarioQueryRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import tools.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProntuarioService {
    private final ProntuarioQueryRepository queries;
    private final ProntuarioJpaRepository prontuarios;
    private final ProntuarioAdendoJpaRepository adendos;
    private final ConsultaRepository consultas;
    private final NotificacaoConclusaoService notificacaoConclusao;
    private final UsuarioRepository usuarios;
    private final AutorizacaoProntuarioService autorizacao;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final Clock prontuarioClock;

    @Transactional(readOnly = true)
    public ProntuarioHistoricoResponse buscarHistorico(long pacienteId, Long alunoId) {
        ProntuarioHistoricoResponse.PacienteResumo paciente = queries.buscarPaciente(pacienteId);
        if (paciente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado.");
        }
        ConsultaClinica consulta = alunoId == null
                ? null : autorizacao.exigirConsultaDoTurno(pacienteId, alunoId, true);
        ConsultaClinica consultaVisivel = consulta != null
                ? consulta : consultasAtivaParaAuditoria(pacienteId);
        return new ProntuarioHistoricoResponse(
                paciente,
                queries.buscarAnamneseMaisRecente(pacienteId),
                queries.listarOdontograma(pacienteId),
                consultaVisivel == null ? null : resumo(consultaVisivel),
                queries.listarEvolucoes(pacienteId));
    }

    private ConsultaClinica consultasAtivaParaAuditoria(long pacienteId) {
        return queries.buscarConsultaDoPaciente(pacienteId);
    }

    @Transactional
    public SalvarProntuarioResponse salvar(long alunoId, RegistrarProntuarioRequest request) {
        ConsultaClinica consulta = autorizacao.exigirConsulta(request.consultaId(), alunoId, false);
        if (prontuarios.findByConsultaId(consulta.id()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta consulta já possui prontuário.");
        }
        if (request.procedimentoId() == null
                || consulta.procedimentoId() != null && !consulta.procedimentoId().equals(request.procedimentoId())
                || !queries.procedimentoPermitido(consulta.turmaId(), request.procedimentoId(),
                request.dente(), request.classe())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Procedimento, dente ou classe não permitido para a turma desta consulta.");
        }

        Usuario aluno = usuarios.findById(alunoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado."));
        OffsetDateTime instante = OffsetDateTime.now(prontuarioClock);
        String evolucao = request.evolucaoClinica().trim();
        String assinatura = criarAssinatura(consulta.id(), request.procedimentoId(), request.dente(),
                request.classe(), evolucao, aluno, instante);

        Prontuario prontuario = new Prontuario();
        prontuario.setPacienteId(consulta.pacienteId());
        prontuario.setAlunoId(alunoId);
        prontuario.setConsultaId(consulta.id());
        prontuario.setTurmaId(consulta.turmaId());
        prontuario.setProcedimentoId(request.procedimentoId());
        prontuario.setDente(request.dente());
        prontuario.setClasse(request.classe());
        prontuario.setEvolucaoClinica(evolucao);
        prontuario.setAssinaturaDigital(assinatura);
        prontuario.setDataRegistro(instante);

        try {
            Prontuario salvo = prontuarios.saveAndFlush(prontuario);
            if (request.observacoesOdontograma() != null) {
                for (var observacao : request.observacoesOdontograma()) {
                    if (queries.atualizarObservacao(consulta.pacienteId(), observacao.dente(),
                            observacao.faceDente(), observacao.condicaoDente(), observacao.observacao()) == 0) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "A observação do odontograma não corresponde a um dente existente.");
                    }
                }
            }
            boolean concluir = Boolean.TRUE.equals(request.concluirAtendimento());
            if (concluir) concluirConsulta(alunoId, consulta.id());
            return new SalvarProntuarioResponse(salvo.getId(), consulta.id(), assinatura, instante, concluir);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível salvar o prontuário por conflito com os dados existentes.", e);
        }
    }

    @Transactional
    public SalvarAdendoResponse adicionarAdendo(long prontuarioId, long alunoId, RegistrarAdendoRequest request) {
        Prontuario prontuario = prontuarios.findById(prontuarioId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Prontuário não encontrado."));
        autorizacao.exigirConsultaDoProntuario(prontuario.getConsultaId(), alunoId,
                prontuario.getPacienteId(), true);
        OffsetDateTime instante = OffsetDateTime.now(prontuarioClock);
        ProntuarioAdendo adendo = new ProntuarioAdendo();
        adendo.setProntuarioId(prontuarioId);
        adendo.setAutorId(alunoId);
        adendo.setTexto(request.texto().trim());
        adendo.setCriadoEm(instante);
        ProntuarioAdendo salvo = adendos.saveAndFlush(adendo);
        return new SalvarAdendoResponse(salvo.getId(), prontuarioId, alunoId, salvo.getTexto(), instante);
    }

    private void concluirConsulta(long alunoId, long consultaId) {
        entityManager.createNativeQuery("""
                SELECT set_config('app.usuario_id', :id, true), set_config('app.origem', 'ALUNO', true)
                """).setParameter("id", String.valueOf(alunoId)).getSingleResult();
        Consulta consulta = consultas.findById(consultaId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta não encontrada."));
        consulta.setStatusConsulta(StatusConsulta.REALIZADO);
        consultas.saveAndFlush(consulta);
        notificacaoConclusao.registrarPesquisaSatisfacao(consulta.getId());
    }

    private String criarAssinatura(long consultaId, long procedimentoId, String dente,
                                   String classe, String evolucao, Usuario aluno,
                                   OffsetDateTime instante) {
        Map<String, Object> clinico = new LinkedHashMap<>();
        clinico.put("versao", "v1");
        clinico.put("consultaId", consultaId);
        clinico.put("procedimentoId", procedimentoId);
        clinico.put("dente", dente);
        clinico.put("classe", classe);
        clinico.put("evolucaoClinica", evolucao);
        clinico.put("alunoId", aluno.getId());
        clinico.put("matricula", aluno.getMatriculaOuRegistro());
        clinico.put("timestamp", instante.toString());
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(objectMapper.writeValueAsString(clinico).getBytes(StandardCharsets.UTF_8));
            return "v1;sha256=" + HexFormat.of().formatHex(hash)
                    + ";timestamp=" + instante
                    + ";alunoId=" + aluno.getId()
                    + ";matricula=" + (aluno.getMatriculaOuRegistro() == null ? "" : aluno.getMatriculaOuRegistro());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não está disponível nesta JVM.", e);
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalStateException("Não foi possível canonicalizar os dados clínicos.", e);
        }
    }

    private static ProntuarioHistoricoResponse.ConsultaResumo resumo(ConsultaClinica consulta) {
        return new ProntuarioHistoricoResponse.ConsultaResumo(consulta.id(), consulta.turmaId(),
                consulta.procedimentoId(), consulta.procedimento(), consulta.dente(), consulta.classe(),
                consulta.data(), consulta.turno(), consulta.status());
    }
}
