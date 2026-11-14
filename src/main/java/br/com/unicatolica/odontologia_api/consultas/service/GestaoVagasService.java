package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.dto.BloqueioVagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.BloqueioTurnoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.CadeiraResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.TurnoGradeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaGestaoRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GestaoVagasService {

    private final VagaRepository vagas;
    private final VagaGestaoRepository gestao;

    @Transactional(readOnly = true)
    public List<CadeiraResponseDTO> listarCadeiras() {
        return gestao.listarCadeirasAtivas();
    }

    @Transactional
    public GerarTurnosResponseDTO gerarTurnos(GerarTurnosRequestDTO request) {
        validarPeriodo(request.inicio(), request.fim());
        return gestao.gerarTurnos(request.inicio(), request.fim());
    }

    @Transactional(readOnly = true)
    public List<TurnoGradeDTO> listarGradeSemanal(LocalDate inicio, LocalDate fim,
                                                   String disciplina, String situacao) {
        validarPeriodo(inicio, fim);
        if (inicio.getDayOfWeek() != DayOfWeek.MONDAY || ChronoUnit.DAYS.between(inicio, fim) != 6) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A grade deve cobrir sete dias, de segunda-feira a domingo.");
        }
        String estado = situacao == null || situacao.isBlank() ? null : situacao.trim().toUpperCase();
        if (estado != null && !List.of("LIVRE", "OCUPADA", "BLOQUEADA").contains(estado)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro de disponibilidade invÃ¡lido.");
        }
        String filtroDisciplina = disciplina == null || disciplina.isBlank() ? null : disciplina.trim();
        return gestao.listarGradeSemanal(inicio, fim, filtroDisciplina, estado);
    }

    @Transactional
    public VagaResponseDTO bloquearTurno(BloqueioTurnoRequestDTO request) {
        String motivo = request.motivo().trim();
        if (motivo.isEmpty() || motivo.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um motivo de bloqueio com até 200 caracteres.");
        }
        if (!gestao.cadeiraAtivaExiste(request.cadeiraId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cadeira não existe ou está inativa.");
        }
        Long vagaId = gestao.materializarBloqueio(
                request.cadeiraId(), request.data(), request.turno().name(), motivo);
        Vaga vaga = buscarParaAtualizacao(vagaId);
        if (gestao.possuiConsultaVinculada(vagaId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível bloquear um turno vinculado a uma consulta.");
        }
        return buscar(vaga.getId());
    }

    @Transactional
    public VagaResponseDTO criar(VagaRequestDTO request) {
        if (!gestao.cadeiraAtivaExiste(request.cadeiraId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A cadeira não existe ou está inativa.");
        }
        Vaga vaga = new Vaga();
        vaga.setCadeiraId(request.cadeiraId());
        vaga.setData(request.data());
        vaga.setTurno(request.turno().name());
        vaga.setBloqueada(false);
        try {
            Vaga salva = vagas.saveAndFlush(vaga);
            return buscar(salva.getId());
        } catch (DataIntegrityViolationException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma vaga para esta cadeira, data e turno.", erro);
        }
    }

    @Transactional(readOnly = true)
    public List<VagaResponseDTO> listar(
            LocalDate inicio, LocalDate fim, Long cadeiraId, VagaRequestDTO.Turno turno, Boolean bloqueada) {
        if (inicio != null && fim != null && fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final deve ser igual ou posterior à data inicial.");
        }
        return gestao.listar(inicio, fim, cadeiraId, turno == null ? null : turno.name(), bloqueada);
    }

    @Transactional(readOnly = true)
    public VagaResponseDTO buscar(Long id) {
        VagaResponseDTO resposta = gestao.buscar(id);
        if (resposta == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada.");
        }
        return resposta;
    }

    @Transactional
    public VagaResponseDTO bloquear(Long id, BloqueioVagaRequestDTO request) {
        String motivo = request == null || request.motivo() == null ? null : request.motivo().trim();
        if (motivo == null || motivo.isEmpty() || motivo.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um motivo de bloqueio com até 200 caracteres.");
        }
        Vaga vaga = buscarParaAtualizacao(id);
        if (gestao.possuiConsultaVinculada(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível bloquear uma vaga vinculada a uma consulta.");
        }
        vaga.setBloqueada(true);
        vaga.setMotivoBloqueio(motivo);
        vagas.saveAndFlush(vaga);
        return buscar(id);
    }

    @Transactional
    public VagaResponseDTO desbloquear(Long id) {
        Vaga vaga = buscarParaAtualizacao(id);
        vaga.setBloqueada(false);
        vaga.setMotivoBloqueio(null);
        vagas.saveAndFlush(vaga);
        return buscar(id);
    }

    @Transactional
    public void remover(Long id) {
        Vaga vaga = buscarParaAtualizacao(id);
        if (gestao.possuiConsultaVinculada(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A vaga possui consulta vinculada e deve ser preservada no histórico.");
        }
        try {
            vagas.delete(vaga);
            vagas.flush();
        } catch (DataIntegrityViolationException erro) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A vaga foi vinculada a uma consulta e não pode ser removida.", erro);
        }
    }

    private Vaga buscarParaAtualizacao(Long id) {
        return vagas.buscarPorIdParaAtualizacao(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vaga não encontrada."));
    }

    private void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe um período válido com data final igual ou posterior à inicial.");
        }
    }
}
