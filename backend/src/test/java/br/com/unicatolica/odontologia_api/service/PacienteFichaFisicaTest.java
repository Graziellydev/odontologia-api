package br.com.unicatolica.odontologia_api.service;

import br.com.unicatolica.odontologia_api.model.Paciente;
import br.com.unicatolica.odontologia_api.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PacienteFichaFisicaTest {
    @Test
    void camposCharExistentesNoSchemaSobrevivemAoCadastroELeituraDoService() {
        PacienteRepository repository = mock(PacienteRepository.class);
        PacienteService service = new PacienteService();
        ReflectionTestUtils.setField(service, "pacienteRepository", repository);
        Paciente cadastro = fichaCompleta();
        cadastro.setCpf("12345678901");
        when(repository.existsByCpf(cadastro.getCpf())).thenReturn(false);
        when(repository.save(cadastro)).thenReturn(cadastro);
        when(repository.findById(42L)).thenReturn(Optional.of(cadastro));

        Paciente salvo = service.salvar(cadastro);
        Paciente detalhe = service.buscarPorId(42L).orElseThrow();

        assertEquals("CE", salvo.getUf());
        assertEquals("CE", detalhe.getUf());
        assertEquals("F", salvo.getSexo());
        assertEquals("F", detalhe.getSexo());
    }

    @Test
    void atualizacaoPersisteOsCamposCharPresentesNoSchema() {
        PacienteRepository repository = mock(PacienteRepository.class);
        PacienteService service = new PacienteService();
        ReflectionTestUtils.setField(service, "pacienteRepository", repository);
        Paciente existente = new Paciente();
        Paciente atualizacao = fichaCompleta();
        when(repository.findById(42L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.atualizar(42L, atualizacao);

        assertEquals("CE", existente.getUf());
        assertEquals("F", existente.getSexo());
        verify(repository).save(existente);
    }

    private Paciente fichaCompleta() {
        Paciente paciente = new Paciente();
        paciente.setUf("CE");
        paciente.setSexo("F");
        return paciente;
    }
}
