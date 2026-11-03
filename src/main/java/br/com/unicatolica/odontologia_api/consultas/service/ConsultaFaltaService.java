package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.dto.AusenciaResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.FaltaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.ReagendamentoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.entity.Consulta;
import br.com.unicatolica.odontologia_api.consultas.entity.StatusConsulta;
import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import br.com.unicatolica.odontologia_api.consultas.repository.ConsultaRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConsultaFaltaService {

    private final ConsultaRepository consultas;
    private final VagaRepository vagas;
    private final EntityManager entityManager;

    @Transactional
    public void registrarFalta(Long consultaId, FaltaRequestDTO request) {
        Consulta consulta = consultas.buscarPorIdParaAtualizacao(consultaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Consulta não encontrada."));

        if (consulta.getStatusConsulta() != StatusConsulta.AGENDADO
                && consulta.getStatusConsulta() != StatusConsulta.CONFIRMADA_24H) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Só é possível registrar falta em consulta agendada ou confirmada.");
        }

        String justificativa = request.justificativa() == null ? null : request.justificativa().trim();
        if (Boolean.TRUE.equals(request.justificada())
                && (justificativa == null || justificativa.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a justificativa textual para marcar a falta como justificada.");
        }

        definirOrigemDoHistorico();
        consulta.setStatusConsulta(request.quemFaltou() == FaltaRequestDTO.QuemFaltou.PACIENTE
                ? StatusConsulta.FALTOU_PACIENTE
                : StatusConsulta.FALTOU_ALUNO);
        consulta.setFaltaJustificada(request.justificada());
        consulta.setJustificativaFalta(justificativa);
        consultas.save(consulta);
    }

    @Transactional
    public Long reagendar(Long origemId, ReagendamentoRequestDTO request) {
        Consulta origem = consultas.buscarPorIdParaAtualizacao(origemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Consulta original não encontrada."));

        if (origem.getStatusConsulta() != StatusConsulta.FALTOU_PACIENTE
                && origem.getStatusConsulta() != StatusConsulta.FALTOU_ALUNO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Só é possível reagendar uma consulta registrada com status de falta.");
        }

        Vaga novaVaga = vagas.buscarPorIdParaAtualizacao(request.novaVagaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nova vaga não encontrada."));
        if (Boolean.TRUE.equals(novaVaga.getBloqueada())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A nova vaga está bloqueada.");
        }
        if (consultas.existeConsultaAtivaNaVaga(novaVaga.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A nova vaga já está ocupada por outra consulta.");
        }

        definirOrigemDoHistorico();
        Consulta reagendada = new Consulta();
        reagendada.setPacienteId(origem.getPacienteId());
        reagendada.setAlunoId(origem.getAlunoId());
        reagendada.setTurmaId(origem.getTurmaId());
        reagendada.setProcedimentoId(origem.getProcedimentoId());
        reagendada.setDente(origem.getDente());
        reagendada.setClasse(origem.getClasse());
        reagendada.setVagaId(novaVaga.getId());
        reagendada.setFilaEsperaId(origem.getFilaEsperaId());
        reagendada.setConsultaOrigemId(origem.getId());
        reagendada.setCriadoPorId(origem.getCriadoPorId());
        reagendada.setStatusConsulta(StatusConsulta.AGENDADO);
        reagendada.setProntuarioLiberado(false);
        reagendada.setFaltaJustificada(null);
        reagendada.setJustificativaFalta(null);
        reagendada.setRetornoSugerido(false);
        reagendada.setJustificativaRetorno(null);

        // O DTO aceita a preferência de notificação definida no contrato. O disparo
        // fica a cargo da integração de WhatsApp, que não faz parte destes requisitos.
        return consultas.saveAndFlush(reagendada).getId();
    }

    @Transactional(readOnly = true)
    public List<AusenciaResponseDTO> listarFaltas(Integer mes, FaltaRequestDTO.QuemFaltou quemFaltou) {
        return consultas.listarAusencias(mes, quemFaltou == null ? null : quemFaltou.name())
                .stream()
                .map(item -> new AusenciaResponseDTO(
                        item.getConsultaId(),
                        item.getData(),
                        item.getAusencia(),
                        item.getVinculoPaciente(),
                        item.getVinculoAluno(),
                        item.getSituacao(),
                        item.getProximoPasso()))
                .toList();
    }

    private void definirOrigemDoHistorico() {
        entityManager.createNativeQuery("SELECT set_config('app.origem', 'SECRETARIA', true)")
                .getSingleResult();
    }
}
