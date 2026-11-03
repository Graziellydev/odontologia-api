package br.com.unicatolica.odontologia_api.solicitacoes.controller;

import br.com.unicatolica.odontologia_api.solicitacoes.dto.AprovarSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.RecusarSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.UsuarioIdentidadeRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.service.SolicitacaoPacienteService;
import jakarta.validation.Validation;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitacaoPacientePermissoesTest {
    private static AnnotationConfigApplicationContext contexto;
    private SolicitacaoPacienteController controller;
    private SolicitacaoPacienteService service;
    private UsuarioIdentidadeRepository identidades;

    @Configuration
    @EnableMethodSecurity
    static class Configuracao {
        @Bean SolicitacaoPacienteService service() { return mock(SolicitacaoPacienteService.class); }
        @Bean UsuarioIdentidadeRepository identidades() { return mock(UsuarioIdentidadeRepository.class); }
        @Bean SolicitacaoPacienteController controller(SolicitacaoPacienteService service, UsuarioIdentidadeRepository identidades) {
            return new SolicitacaoPacienteController(service, identidades);
        }
    }
    @BeforeAll static void iniciar() { contexto = new AnnotationConfigApplicationContext(Configuracao.class); }
    @AfterAll static void encerrar() { contexto.close(); }
    @BeforeEach void preparar() {
        SecurityContextHolder.clearContext();
        controller = contexto.getBean(SolicitacaoPacienteController.class);
        service = contexto.getBean(SolicitacaoPacienteService.class);
        identidades = contexto.getBean(UsuarioIdentidadeRepository.class);
        reset(service, identidades);
    }
    @AfterEach void limpar() { SecurityContextHolder.clearContext(); }
    private void autenticar(String autoridade) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
            "secretaria@teste.local", "irrelevante", List.of(new SimpleGrantedAuthority(autoridade))));
    }

    @Test void somenteSecretariaPodeDecidirEConsultarAdministrativas() {
        for (String perfil : List.of("ALUNO", "COORDENADOR", "ROLE_SECRETARIA")) {
            autenticar(perfil);
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThrows(AccessDeniedException.class, () -> controller.recusarSolicitacao(1L, new RecusarSolicitacaoDTO("Motivo"), auth));
            assertThrows(AccessDeniedException.class, () -> controller.aprovarSolicitacao(1L, new AprovarSolicitacaoDTO(25L), auth));
            assertThrows(AccessDeniedException.class, () -> controller.listarAdministrativas());
        }
        verifyNoInteractions(service, identidades);
    }

    @Test void semAutenticacaoNaoDecide() {
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.recusarSolicitacao(1L, new RecusarSolicitacaoDTO("Motivo"), null));
        assertThrows(AuthenticationCredentialsNotFoundException.class,
                () -> controller.aprovarSolicitacao(1L, new AprovarSolicitacaoDTO(25L), null));
        verifyNoInteractions(service, identidades);
    }

    @Test void secretariaAutenticadaTemAutoriaResolvidaNoServidor() {
        autenticar("SECRETARIA");
        when(identidades.buscarIdPorEmail("secretaria@teste.local")).thenReturn(Optional.of(7L));
        when(service.recusarSolicitacao(1L, 7L, "Motivo")).thenReturn(new SolicitacaoPaciente());
        when(service.aprovarSolicitacao(2L, 25L, 7L)).thenReturn(new SolicitacaoPaciente());
        var auth = SecurityContextHolder.getContext().getAuthentication();
        controller.recusarSolicitacao(1L, new RecusarSolicitacaoDTO("Motivo"), auth);
        controller.aprovarSolicitacao(2L, new AprovarSolicitacaoDTO(25L), auth);
        verify(service).recusarSolicitacao(1L, 7L, "Motivo");
        verify(service).aprovarSolicitacao(2L, 25L, 7L);
    }

    @Test void contratosRejeitamJustificativaAusenteOuIdInvalido() {
        try (var fabrica = Validation.buildDefaultValidatorFactory()) {
            var validator = fabrica.getValidator();
            for (String motivo : new String[] { null, "", "   ", "x".repeat(5001) })
                assertFalse(validator.validate(new RecusarSolicitacaoDTO(motivo)).isEmpty());
            assertTrue(validator.validate(new RecusarSolicitacaoDTO("Motivo valido")).isEmpty());
            for (Long id : new Long[] { null, 0L, -1L })
                assertFalse(validator.validate(new AprovarSolicitacaoDTO(id)).isEmpty());
            assertTrue(validator.validate(new AprovarSolicitacaoDTO(25L)).isEmpty());
        }
    }

    @Test void conflitoOtimistaVira409SemDetalhesSensiveis() {
        var resposta = new SolicitacaoConflitoHandler().conflito(new OptimisticLockingFailureException("senha=SEGREDO"));
        assertEquals(409, resposta.getStatusCode().value());
        assertFalse(resposta.getBody().toString().contains("SEGREDO"));
    }
}
