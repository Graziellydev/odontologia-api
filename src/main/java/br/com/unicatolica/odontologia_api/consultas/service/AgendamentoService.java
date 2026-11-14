package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.AgendamentoResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.AlunoAgendamentoDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.DisponibilidadeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.FilaAgendamentoDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.NotificacaoConsultaDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.RemarcacaoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.NotificacaoWhatsapp;
import br.com.unicatolica.odontologia_api.consultas.entity.StatusConsulta;
import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import br.com.unicatolica.odontologia_api.consultas.repository.AgendaConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.ConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaGestaoRepository;
import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.model.Paciente;
import br.com.unicatolica.odontologia_api.model.Perfil;
import br.com.unicatolica.odontologia_api.model.Usuario;
import br.com.unicatolica.odontologia_api.repository.PacienteRepository;
import br.com.unicatolica.odontologia_api.repository.UsuarioRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgendamentoService {

    private final AgendaConsultaRepository agenda;
    private final ConsultaRepository consultas;
    private final VagaRepository vagas;
    private final VagaGestaoRepository gestaoVagas;
    private final FilaEsperaRepository filaRepository;
    private final PacienteRepository pacientes;
    private final UsuarioRepository usuarios;
    private final NotificacaoAgendamentoService notificacaoService;
    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<FilaAgendamentoDTO> listarFila() {
        return agenda.listarFilaDisponivel();
    }

    @Transactional(readOnly = true)
    public List<AlunoAgendamentoDTO> listarAlunos(long filaId, LocalDate data, AgendamentoRequestDTO.Turno turno) {
        validarData(data);
        exigirFilaElegivel(filaId);
        return agenda.listarAlunosElegiveis(filaId, data, turno.name());
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeDTO> listarDisponibilidade(
            LocalDate inicio, LocalDate fim, Long filaId, Long alunoId, Long consultaIgnorada) {
        validarPeriodo(inicio, fim);
        return agenda.listarDisponibilidade(inicio, fim, filaId, alunoId, consultaIgnorada);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoResponseDTO> listarAgendamentos(LocalDate data, Long alunoId, String status, String busca) {
        String filtroStatus = status == null || status.isBlank() ? null : status.trim().toUpperCase();
        if (filtroStatus != null) {
            try {
                StatusConsulta.valueOf(filtroStatus);
            } catch (IllegalArgumentException erro) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de consulta inválido.");
            }
        }
        String termo = busca == null || busca.isBlank() ? null : busca.trim();
        return agenda.listarAgendamentos(data, alunoId, filtroStatus, termo);
    }

    @Transactional(readOnly = true)
    public AgendamentoResponseDTO buscarAgendamento(long id) {
        AgendamentoResponseDTO consulta = agenda.buscarAgendamento(id);
        if (consulta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta não encontrada.");
        }
        return consulta;
    }

    @Transactional(readOnly = true)
    public List<NotificacaoConsultaDTO> listarNotificacoes(Long consultaId) {
        return agenda.listarNotificacoes(consultaId);
    }

    @Transactional
    public AgendamentoResponseDTO agendar(AgendamentoRequestDTO request, long secretariaId) {
        validarData(request.data());
        FilaEspera fila = buscarFilaParaAtualizacao(request.filaEsperaId());
        validarStatusFila(fila);
        Paciente paciente = pacientes.buscarPorIdParaAtualizacao(fila.getPacienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));
        if (!"ATIVO".equals(paciente.getSituacao())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O paciente não está ativo.");
        }
        Usuario aluno = buscarAlunoParaAtualizacao(request.alunoId());
        validarTurmaElegivel(fila, aluno, request.turmaId());

        if (agenda.existeConsultaAtivaNaFila(fila.getId(), null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta demanda já possui uma consulta ativa.");
        }
        validarSemConflito(paciente.getId(), aluno.getId(), request.data(), request.turno().name(), null);

        try {
            if (!gestaoVagas.cadeiraAtivaExiste(request.cadeiraId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A cadeira não existe ou está inativa.");
            }
            vagas.criarSeAusente(request.cadeiraId(), request.data(), request.turno().name());
            Vaga vaga = vagas.buscarPorCadeiraDataTurnoParaAtualizacao(
                            request.cadeiraId(), request.data(), request.turno().name())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                            "Não foi possível reservar a cadeira selecionada."));
            if (Boolean.TRUE.equals(vaga.getBloqueada()) || gestaoVagas.possuiConsultaAtiva(vaga.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A cadeira selecionada está bloqueada ou já possui uma consulta.");
            }
            Consulta consulta = novaConsulta(fila, aluno, request.turmaId(), vaga, secretariaId);
            prepararHistorico(secretariaId);
            consultas.saveAndFlush(consulta);
            fila.setStatusFila("ALOCADO");
            fila.setDataSaidaFila(OffsetDateTime.now());
            filaRepository.saveAndFlush(fila);
            NotificacaoWhatsapp notificacao = notificacaoService.registrar(
                    consulta, paciente, request.data(), request.turno().name(),
                    "COMPROVANTE_AGENDAMENTO", secretariaId);
            return agenda.buscarAgendamento(consulta.getId(), notificacao.getId());
        } catch (DataIntegrityViolationException erro) {
            throw conflitoConcorrencia(erro);
        }
    }

    @Transactional
    public AgendamentoResponseDTO cancelar(long consultaId, long secretariaId) {
        Consulta consulta = buscarConsultaParaAtualizacao(consultaId);
        validarConsultaAtiva(consulta);
        if (consulta.getFilaEsperaId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta consulta não está vinculada a uma entrada da fila e não pode retornar à fila automaticamente.");
        }

        FilaEspera fila = buscarFilaParaAtualizacao(consulta.getFilaEsperaId());
        Paciente paciente = pacientes.buscarPorIdParaAtualizacao(consulta.getPacienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));
        Vaga vaga = vagas.findById(consulta.getVagaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga da consulta não encontrada."));
        prepararHistorico(secretariaId);
        consulta.setStatusConsulta(StatusConsulta.CANCELADO_SECRETARIA);
        try {
            consultas.saveAndFlush(consulta);
            if (agenda.existeOutraEntradaAguardando(fila.getId())) {
                fila.setStatusFila("INATIVO");
                fila.setDataSaidaFila(OffsetDateTime.now());
            } else {
                fila.setStatusFila("AGUARDANDO");
                fila.setDataSaidaFila(null);
            }
            filaRepository.saveAndFlush(fila);
            NotificacaoWhatsapp notificacao = notificacaoService.registrar(
                    consulta, paciente, vaga.getData(), vaga.getTurno(), "AVISO_CANCELAMENTO", secretariaId);
            return agenda.buscarAgendamento(consulta.getId(), notificacao.getId());
        } catch (DataIntegrityViolationException erro) {
            throw conflitoConcorrencia(erro);
        }
    }

    @Transactional
    public AgendamentoResponseDTO remarcar(long consultaId, RemarcacaoRequestDTO request, long secretariaId) {
        validarData(request.data());
        Consulta origem = buscarConsultaParaAtualizacao(consultaId);
        validarConsultaAtiva(origem);
        if (origem.getFilaEsperaId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta consulta não está vinculada a uma demanda da fila.");
        }
        FilaEspera fila = buscarFilaParaAtualizacao(origem.getFilaEsperaId());
        Paciente paciente = pacientes.buscarPorIdParaAtualizacao(origem.getPacienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente não encontrado."));
        Usuario aluno = buscarAlunoParaAtualizacao(origem.getAlunoId());
        Vaga vagaAntiga = vagas.findById(origem.getVagaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga da consulta não encontrada."));

        if (vagaAntiga.getData().equals(request.data())
                && vagaAntiga.getTurno().equals(request.turno().name())
                && request.cadeiraId().equals(vagaAntiga.getCadeiraId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A cadeira, data e turno devem ser diferentes do agendamento atual.");
        }
        validarSemConflito(paciente.getId(), aluno.getId(), request.data(), request.turno().name(), origem.getId());
        if (!gestaoVagas.cadeiraAtivaExiste(request.cadeiraId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cadeira selecionada não existe ou está inativa.");
        }
        vagas.criarSeAusente(request.cadeiraId(), request.data(), request.turno().name());
        Vaga novaVaga = vagas.buscarPorCadeiraDataTurnoParaAtualizacao(
                        request.cadeiraId(), request.data(), request.turno().name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "Não foi possível reservar a cadeira selecionada."));
        if (Boolean.TRUE.equals(novaVaga.getBloqueada()) || gestaoVagas.possuiConsultaAtiva(novaVaga.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A cadeira selecionada está bloqueada ou já possui uma consulta.");
        }
        prepararHistorico(secretariaId);
        Consulta remarcada = copiarConsulta(origem, novaVaga, secretariaId);
        try {
            // As duas alterações e a notificação fazem parte da mesma transação.
            origem.setStatusConsulta(StatusConsulta.CANCELADO_SECRETARIA);
            consultas.saveAndFlush(origem);
            consultas.saveAndFlush(remarcada);
            NotificacaoWhatsapp notificacao = notificacaoService.registrar(
                    remarcada, paciente, request.data(), request.turno().name(),
                    "AVISO_REMARCACAO", secretariaId);
            return agenda.buscarAgendamento(remarcada.getId(), notificacao.getId());
        } catch (DataIntegrityViolationException erro) {
            throw conflitoConcorrencia(erro);
        }
    }

    private Consulta novaConsulta(FilaEspera fila, Usuario aluno, long turmaId, Vaga vaga, long secretariaId) {
        Consulta consulta = new Consulta();
        consulta.setPacienteId(fila.getPacienteId());
        consulta.setAlunoId(aluno.getId());
        consulta.setTurmaId(turmaId);
        consulta.setProcedimentoId(fila.getProcedimentoId());
        consulta.setDente(fila.getDente());
        consulta.setClasse(fila.getClasse());
        consulta.setVagaId(vaga.getId());
        consulta.setFilaEsperaId(fila.getId());
        consulta.setCriadoPorId(secretariaId);
        consulta.setStatusConsulta(StatusConsulta.AGENDADO);
        consulta.setProntuarioLiberado(false);
        consulta.setFaltaJustificada(null);
        consulta.setJustificativaFalta(null);
        consulta.setRetornoSugerido(false);
        consulta.setJustificativaRetorno(null);
        return consulta;
    }

    private Consulta copiarConsulta(Consulta origem, Vaga novaVaga, long secretariaId) {
        Consulta nova = new Consulta();
        nova.setPacienteId(origem.getPacienteId());
        nova.setAlunoId(origem.getAlunoId());
        nova.setTurmaId(origem.getTurmaId());
        nova.setProcedimentoId(origem.getProcedimentoId());
        nova.setDente(origem.getDente());
        nova.setClasse(origem.getClasse());
        nova.setVagaId(novaVaga.getId());
        nova.setFilaEsperaId(origem.getFilaEsperaId());
        nova.setConsultaOrigemId(origem.getId());
        nova.setCriadoPorId(secretariaId);
        nova.setStatusConsulta(StatusConsulta.AGENDADO);
        nova.setProntuarioLiberado(false);
        nova.setFaltaJustificada(null);
        nova.setJustificativaFalta(null);
        nova.setRetornoSugerido(false);
        nova.setJustificativaRetorno(null);
        return nova;
    }

    private void validarSemConflito(long pacienteId, long alunoId, LocalDate data,
                                    String turno, Long consultaIgnorada) {
        if (agenda.existeConflitoDePaciente(pacienteId, data, turno, consultaIgnorada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O paciente já possui uma consulta ativa nesse dia e turno.");
        }
        if (agenda.existeConflitoDeAluno(alunoId, data, turno, consultaIgnorada)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O aluno já possui uma consulta ativa nesse dia e turno.");
        }
    }

    private void validarTurmaElegivel(FilaEspera fila, Usuario aluno, long turmaId) {
        if (!agenda.alunoPodeAtender(fila.getId(), aluno.getId(), turmaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O aluno não está matriculado em uma turma ativa compatível com esse procedimento.");
        }
    }

    private FilaEspera buscarFilaParaAtualizacao(long id) {
        return filaRepository.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada da fila não encontrada."));
    }

    private Consulta buscarConsultaParaAtualizacao(long id) {
        return consultas.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Consulta não encontrada."));
    }

    private Usuario buscarAlunoParaAtualizacao(long id) {
        Usuario aluno = usuarios.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado."));
        if (aluno.getPerfil() != Perfil.ALUNO || !Boolean.TRUE.equals(aluno.getAtivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O usuário selecionado não é um aluno ativo.");
        }
        return aluno;
    }

    private void exigirFilaElegivel(long id) {
        FilaEspera fila = filaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Entrada da fila não encontrada."));
        validarStatusFila(fila);
    }

    private void validarStatusFila(FilaEspera fila) {
        if (!"AGUARDANDO".equals(fila.getStatusFila()) && !"ALOCADO".equals(fila.getStatusFila())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta demanda não está disponível para agendamento.");
        }
    }

    private void validarConsultaAtiva(Consulta consulta) {
        if (consulta.getStatusConsulta() != StatusConsulta.AGENDADO
                && consulta.getStatusConsulta() != StatusConsulta.CONFIRMADA_24H) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente consultas agendadas ou confirmadas podem ser canceladas ou remarcadas.");
        }
    }

    private void validarData(LocalDate data) {
        if (data == null || data.isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma data de hoje ou futura.");
        }
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isBefore(LocalDate.now()) || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um período futuro válido.");
        }
        if (fim.isAfter(inicio.plusDays(90))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consulte no máximo 90 dias por vez.");
        }
    }

    private void prepararHistorico(long secretariaId) {
        entityManager.createNativeQuery("SELECT set_config('app.usuario_id', :id, true)")
                .setParameter("id", String.valueOf(secretariaId)).getSingleResult();
        entityManager.createNativeQuery("SELECT set_config('app.origem', 'SECRETARIA', true)")
                .getSingleResult();
    }

    private ResponseStatusException conflitoConcorrencia(DataIntegrityViolationException erro) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "A vaga ou a fila foi alterada por outra operação. Atualize a disponibilidade e tente novamente.", erro);
    }
}
