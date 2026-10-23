package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.model.Paciente;
import br.com.unicatolica.odontologia_api.repository.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

// @Service: Indica que esta classe contem a logica de negocio da aplicacao
@Service
public class PacienteService {

    // Injecao de dependencia do Repository para comunicacao com o PostgreSQL
    @Autowired
    private PacienteRepository pacienteRepository;

    // Regra do RF007: Cadastrar um novo paciente pela Secretaria
    public Paciente salvar(Paciente paciente) {
        // Validador de seguranca: Nao permite cadastrar dois pacientes com o mesmo CPF
        if (paciente.getCpf() != null && pacienteRepository.existsByCpf(paciente.getCpf())) {
            throw new IllegalArgumentException("Ja existe um paciente cadastrado com o CPF: " + paciente.getCpf());
        }

        // Define a situacao padrao como ATIVO caso nao tenha sido informada
        if (paciente.getSituacao() == null || paciente.getSituacao().isBlank()) {
            paciente.setSituacao("ATIVO");
        }

        // Preenche com valores seguros os campos que pertencem a funcionalidades futuras
        if (paciente.getTermoCompromissoAssinado() == null) {
            paciente.setTermoCompromissoAssinado(false);
        }
        if (paciente.getMoraProximo() == null) {
            paciente.setMoraProximo(false);
        }
        if (paciente.getAceitaChamadaUltimaHora() == null) {
            paciente.setAceitaChamadaUltimaHora(false);
        }
        if (paciente.getFaltasJustificadas() == null) {
            paciente.setFaltasJustificadas(0);
        }
        if (paciente.getFaltasNaoJustificadas() == null) {
            paciente.setFaltasNaoJustificadas(0);
        }

        // Se o termo foi marcado como assinado, preenche a data atual do termo automaticamente
        if (Boolean.TRUE.equals(paciente.getTermoCompromissoAssinado()) && paciente.getDataTermoCompromisso() == null) {
            paciente.setDataTermoCompromisso(OffsetDateTime.now());
        }

        // Salva o paciente no banco de dados
        return pacienteRepository.save(paciente);
    }

    // Retorna a lista de todos os pacientes cadastrados
    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    // Busca um paciente especifico pelo seu ID
    public Optional<Paciente> buscarPorId(Long id) {
        return pacienteRepository.findById(id);
    }

    // Busca um paciente especifico pelo CPF
    public Optional<Paciente> buscarPorCpf(String cpf) {
        return pacienteRepository.findByCpf(cpf);
    }

    // Atualiza os dados cadastrais de um paciente existente
    public Paciente atualizar(Long id, Paciente pacienteAtualizado) {
        return pacienteRepository.findById(id).map(paciente -> {
            paciente.setNomeCompleto(pacienteAtualizado.getNomeCompleto());
            paciente.setTelefone(pacienteAtualizado.getTelefone());
            paciente.setTelefoneAlternativo(pacienteAtualizado.getTelefoneAlternativo());
            paciente.setEmail(pacienteAtualizado.getEmail());
            paciente.setCep(pacienteAtualizado.getCep());
            paciente.setLogradouro(pacienteAtualizado.getLogradouro());
            paciente.setNumeroEndereco(pacienteAtualizado.getNumeroEndereco());
            paciente.setComplemento(pacienteAtualizado.getComplemento());
            paciente.setBairro(pacienteAtualizado.getBairro());
            paciente.setCidade(pacienteAtualizado.getCidade());
            // (Linha do UF removida pois o campo foi descontinuado)
            paciente.setMoraProximo(pacienteAtualizado.getMoraProximo());
            paciente.setAceitaChamadaUltimaHora(pacienteAtualizado.getAceitaChamadaUltimaHora());
            paciente.setDisponibilidadeHorario(pacienteAtualizado.getDisponibilidadeHorario());
            paciente.setObservacao(pacienteAtualizado.getObservacao());

            return pacienteRepository.save(paciente);
        }).orElseThrow(() -> new IllegalArgumentException("Paciente nao encontrado com o ID: " + id));
    }

    // Realiza o desligamento do paciente (Exclusao Logica - altera situacao para DESLIGADO)
    public Paciente desligarPaciente(Long id, String motivo, Long desligadoPorId) {
        return pacienteRepository.findById(id).map(paciente -> {
            paciente.setSituacao("DESLIGADO");
            paciente.setMotivoDesligamento(motivo);
            paciente.setDesligadoPorId(desligadoPorId);
            paciente.setDataDesligamento(OffsetDateTime.now());
            return pacienteRepository.save(paciente);
        }).orElseThrow(() -> new IllegalArgumentException("Paciente nao encontrado com o ID: " + id));
    }
}