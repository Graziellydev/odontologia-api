package br.com.unicatolica.odontologia_api.repository;

import br.com.unicatolica.odontologia_api.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// @Repository: Indica ao Spring Boot que esta interface e responsavel pela comunicacao direta com o banco de dados
@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    // Busca um paciente no banco de dados pelo CPF
    Optional<Paciente> findByCpf(String cpf);

    // Busca um paciente pelo numero do prontuario
    Optional<Paciente> findByNumeroProntuario(String numeroProntuario);

    // Verifica se ja existe um paciente cadastrado com o CPF informado (retorna true ou false)
    boolean existsByCpf(String cpf);

    // Verifica se ja existe um paciente cadastrado com o numero de prontuario informado
    boolean existsByNumeroProntuario(String numeroProntuario);
}
