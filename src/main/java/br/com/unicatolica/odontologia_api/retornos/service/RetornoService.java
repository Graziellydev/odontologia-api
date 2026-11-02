package br.com.unicatolica.odontologia_api.retornos.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import br.com.unicatolica.odontologia_api.retornos.dto.AtendimentoDoTurnoDTO;
import br.com.unicatolica.odontologia_api.retornos.dto.RetornoResponse;
import br.com.unicatolica.odontologia_api.retornos.dto.SugestaoRetornoRequest;
import br.com.unicatolica.odontologia_api.retornos.entity.ConsultaRetorno;
import br.com.unicatolica.odontologia_api.retornos.repository.ConsultaRetornoRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetornoService {

    private static final Set<String> STATUS_ABERTOS = Set.of("AGENDADO", "CONFIRMADA_24H");
    private static final Set<String> URGENCIAS = Set.of("ALTA", "MEDIA", "BAIXA", "PNE");

    private final ConsultaRetornoRepository consultas;
    private final FilaEsperaRepository fila;
    private final EntityManager em;

    @Transactional(readOnly = true)
    public List<AtendimentoDoTurnoDTO> listarAtendimentosDeHoje(Long alunoId) {
        return consultas.listarDeHoje(alunoId).stream()
                .map(v -> new AtendimentoDoTurnoDTO(
                        v.getConsultaId(), v.getPaciente(), v.getProcedimento(),
                        v.getDente(), v.getClasse(), v.getData(), v.getTurno()))
                .toList();
    }

    @Transactional
    public RetornoResponse registrar(Long alunoId, SugestaoRetornoRequest dto) {

        ConsultaRetorno consulta = consultas.findById(dto.consultaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Atendimento não encontrado."));

        // Só o aluno responsável pode encerrar o próprio atendimento
        if (!alunoId.equals(consulta.getAlunoId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Este atendimento não pertence a você.");
        }

        // Evita sugerir duas vezes (e duplicar o paciente na fila)
        if (!STATUS_ABERTOS.contains(consulta.getStatusConsulta())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Este atendimento já foi encerrado.");
        }

        boolean necessitaRetorno = Boolean.TRUE.equals(dto.necessitaRetorno());
        String justificativa = dto.justificativa() == null ? "" : dto.justificativa().trim();

        // Cenário 3: justificativa obrigatória
        if (necessitaRetorno && justificativa.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe a justificativa técnica para sugerir o retorno.");
        }

        if (necessitaRetorno && consulta.getProcedimentoId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O atendimento não tem procedimento associado. Procure a secretaria.");
        }

        // Informa ao trigger do histórico quem está alterando o status (vale só nesta transação)
        em.createNativeQuery(
                "SELECT set_config('app.usuario_id', :id, true), set_config('app.origem', 'ALUNO', true)")
                .setParameter("id", String.valueOf(alunoId))
                .getSingleResult();

        // Nos dois cenários, o atendimento do turno é encerrado
        consulta.setStatusConsulta("REALIZADO");

        // Cenário 2: tratamento concluído, sem retorno
        if (!necessitaRetorno) {
            consulta.setRetornoSugerido(false);
            consultas.save(consulta);
            return new RetornoResponse(consulta.getId(), false, null,
                    "Tratamento concluído. O atendimento foi encerrado, sem retorno.");
        }

        // Cenário 1: a mesma demanda não pode estar duplicada na fila
        long jaAguardando = consultas.contarDemandaAguardando(
                consulta.getPacienteId(),
                consulta.getProcedimentoId(),
                consulta.getDente() == null ? "" : consulta.getDente(),
                consulta.getClasse() == null ? "" : consulta.getClasse());
        if (jaAguardando > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este paciente já está na fila de espera com o mesmo procedimento, dente e classe.");
        }

        // Procedimento, dente e classe são preservados
        FilaEspera entrada = new FilaEspera();
        entrada.setPacienteId(consulta.getPacienteId());
        entrada.setOrigem("RETORNO");
        entrada.setProcedimentoId(consulta.getProcedimentoId());
        entrada.setDente(consulta.getDente());
        entrada.setClasse(consulta.getClasse());
        entrada.setNivelUrgencia(urgenciaDoRetorno(consulta));
        entrada.setPrioridadeReagendamento(false);
        entrada.setStatusFila("AGUARDANDO");
        entrada.setObservacao("Retorno sugerido pelo aluno: " + justificativa);
        FilaEspera salva = fila.save(entrada);

        consulta.setRetornoSugerido(true);
        consulta.setJustificativaRetorno(justificativa);
        consultas.save(consulta);

        return new RetornoResponse(consulta.getId(), true, salva.getId(),
                "Retorno sugerido. O paciente entrou na fila e aguarda agendamento da secretaria.");
    }

    // Prioridade do retorno: a da fila que originou a consulta; senão, a da última triagem
    private String urgenciaDoRetorno(ConsultaRetorno consulta) {
        String urgencia = null;
        if (consulta.getFilaEsperaId() != null) {
            urgencia = fila.findById(consulta.getFilaEsperaId())
                    .map(FilaEspera::getNivelUrgencia)
                    .orElse(null);
        }
        if (urgencia == null) {
            urgencia = consultas.ultimaUrgenciaDaTriagem(consulta.getPacienteId()).orElse(null);
        }
        return normalizarUrgencia(urgencia);
    }

    private String normalizarUrgencia(String valor) {
        if (valor == null) {
            return "MEDIA";
        }
        String u = valor.trim().toUpperCase(Locale.ROOT).replace("É", "E");
        return URGENCIAS.contains(u) ? u : "MEDIA";
    }
}