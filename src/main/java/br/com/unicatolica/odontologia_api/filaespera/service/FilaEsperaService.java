package br.com.unicatolica.odontologia_api.filaespera.service;

import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaAtualizadaDTO;
import java.time.OffsetDateTime;
import br.com.unicatolica.odontologia_api.filaespera.dto.NovaEntradaFilaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.EntradaFilaCriadaDTO;
import br.com.unicatolica.odontologia_api.filaespera.entity.FilaEspera;
import br.com.unicatolica.odontologia_api.filaespera.dto.TempoMedioEsperaDTO;
import br.com.unicatolica.odontologia_api.filaespera.dto.FilaEsperaItemDTO;
import br.com.unicatolica.odontologia_api.filaespera.repository.FilaEsperaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class FilaEsperaService {

    private final FilaEsperaRepository filaEsperaRepository;

    public FilaEsperaService(
            FilaEsperaRepository filaEsperaRepository) {
        this.filaEsperaRepository = filaEsperaRepository;
    }

    public List<FilaEsperaItemDTO> listarFila(
            Long procedimentoId,
            String especialidade,
            String dente,
            String classe,
            String nivelUrgencia,
            Integer diasEsperaMin,
            Integer diasEsperaMax) {

        if (diasEsperaMin != null && diasEsperaMin < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O tempo mínimo de espera não pode ser negativo.");
        }

        if (diasEsperaMax != null && diasEsperaMax < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O tempo máximo de espera não pode ser negativo.");
        }

        if (diasEsperaMin != null
                && diasEsperaMax != null
                && diasEsperaMin > diasEsperaMax) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O tempo mínimo não pode ser maior que o máximo.");
        }

        return filaEsperaRepository.pesquisarFila(
                procedimentoId,
                limpar(especialidade),
                limpar(dente),
                limpar(classe),
                limpar(nivelUrgencia),
                diasEsperaMin,
                diasEsperaMax);
    }

    private String limpar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    public List<TempoMedioEsperaDTO> calcularTempoMedioPorEspecialidade() {
        return filaEsperaRepository.calcularTempoMedioPorEspecialidade();
    }

    public EntradaFilaCriadaDTO adicionarEntrada(NovaEntradaFilaDTO dto) {

        FilaEspera entrada = new FilaEspera();

        entrada.setPacienteId(dto.pacienteId());
        entrada.setTriagemItemId(dto.triagemItemId());
        entrada.setTratamentoId(dto.tratamentoId());
        entrada.setOrigem("TRIAGEM");
        entrada.setProcedimentoId(dto.procedimentoId());
        entrada.setDente(dto.dente());
        entrada.setClasse(dto.classe());

        String urgencia = dto.nivelUrgencia()
                .trim()
                .toUpperCase(java.util.Locale.ROOT);

        if (urgencia.equals("MÉDIA")) {
            urgencia = "MEDIA";
        }

        entrada.setNivelUrgencia(urgencia);

        entrada.setPrioridadeReagendamento(
                Boolean.TRUE.equals(dto.prioridadeReagendamento()));

        entrada.setStatusFila("AGUARDANDO");
        entrada.setObservacao(dto.observacao());

        FilaEspera salva = filaEsperaRepository.save(entrada);

        return new EntradaFilaCriadaDTO(
                salva.getId(),
                salva.getPacienteId(),
                salva.getProcedimentoId(),
                salva.getNivelUrgencia(),
                salva.getStatusFila());
    }

    public FilaEsperaAtualizadaDTO alocarPaciente(Long id) {

        FilaEspera entrada = filaEsperaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Entrada da fila não encontrada."));

        if (!"AGUARDANDO".equalsIgnoreCase(entrada.getStatusFila())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este paciente não está aguardando na fila.");
        }

        entrada.setStatusFila("ALOCADO");
        entrada.setDataSaidaFila(OffsetDateTime.now());

        FilaEspera atualizada = filaEsperaRepository.save(entrada);

        return new FilaEsperaAtualizadaDTO(
                atualizada.getId(),
                atualizada.getStatusFila(),
                atualizada.getDataSaidaFila());
    }
}