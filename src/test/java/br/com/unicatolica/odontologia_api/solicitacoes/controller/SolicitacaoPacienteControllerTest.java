package br.com.unicatolica.odontologia_api.solicitacoes.controller;

import br.com.unicatolica.odontologia_api.solicitacoes.dto.AprovarSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.NovaSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.dto.RecusarSolicitacaoDTO;
import br.com.unicatolica.odontologia_api.solicitacoes.entity.SolicitacaoPaciente;
import br.com.unicatolica.odontologia_api.solicitacoes.repository.UsuarioIdentidadeRepository;
import br.com.unicatolica.odontologia_api.solicitacoes.service.SolicitacaoPacienteService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitacaoPacienteControllerTest {

    @Mock
    private SolicitacaoPacienteService service;

    @Mock
    private UsuarioIdentidadeRepository usuarioRepository;

    @Mock
    private Authentication autenticacao;

    @InjectMocks
    private SolicitacaoPacienteController controller;

    @Test
    void deveCriarSolicitacaoDoAluno() {
        when(autenticacao.isAuthenticated()).thenReturn(true);
        when(autenticacao.getName()).thenReturn("aluno@teste.com");
        when(usuarioRepository.buscarIdPorEmail("aluno@teste.com"))
                .thenReturn(Optional.of(2L));

        NovaSolicitacaoDTO dados = new NovaSolicitacaoDTO(
                3L, 5L, "11", "I", LocalDate.now().plusDays(2));

        SolicitacaoPaciente solicitacao = new SolicitacaoPaciente();
        solicitacao.setId(10L);
        solicitacao.setAlunoId(2L);
        solicitacao.setStatusSolicitacao("PENDENTE_APROVACAO");

        when(service.criarSolicitacao(2L, dados)).thenReturn(solicitacao);

        var resposta = controller.criarSolicitacao(dados, autenticacao);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals(10L, resposta.getBody().getId());

        verify(service).criarSolicitacao(2L, dados);
    }

    @Test
    void deveListarSolicitacoesDoAlunoAutenticado() {
        when(autenticacao.isAuthenticated()).thenReturn(true);
        when(autenticacao.getName()).thenReturn("aluno@teste.com");
        when(usuarioRepository.buscarIdPorEmail("aluno@teste.com"))
                .thenReturn(Optional.of(2L));

        when(service.listarSolicitacoesDoAluno(2L))
                .thenReturn(List.of(new SolicitacaoPaciente()));

        var resultado = controller.listarMinhasSolicitacoes(autenticacao);

        assertEquals(1, resultado.size());
        verify(service).listarSolicitacoesDoAluno(2L);
    }

    @Test
    void deveListarSolicitacoesPendentesDaSecretaria() {
        when(service.listarPendentesSecretaria())
                .thenReturn(List.of(new SolicitacaoPaciente()));

        var resultado = controller.listarPendentes();

        assertEquals(1, resultado.size());
        verify(service).listarPendentesSecretaria();
    }

    @Test
    void deveAprovarSolicitacaoComoSecretaria() {
        when(autenticacao.isAuthenticated()).thenReturn(true);
        when(autenticacao.getName()).thenReturn("secretaria@teste.com");
        when(usuarioRepository.buscarIdPorEmail("secretaria@teste.com"))
                .thenReturn(Optional.of(7L));

        AprovarSolicitacaoDTO dados = new AprovarSolicitacaoDTO(25L);

        SolicitacaoPaciente aprovada = new SolicitacaoPaciente();
        aprovada.setId(10L);
        aprovada.setStatusSolicitacao("APROVADO_E_ALOCADO");

        when(service.aprovarSolicitacao(10L, 25L, 7L))
                .thenReturn(aprovada);

        var resultado = controller.aprovarSolicitacao(
                10L, dados, autenticacao);

        assertEquals("APROVADO_E_ALOCADO",
                resultado.getStatusSolicitacao());

        verify(service).aprovarSolicitacao(10L, 25L, 7L);
    }

    @Test
    void deveRecusarSolicitacaoComoSecretaria() {
        when(autenticacao.isAuthenticated()).thenReturn(true);
        when(autenticacao.getName()).thenReturn("secretaria@teste.com");
        when(usuarioRepository.buscarIdPorEmail("secretaria@teste.com"))
                .thenReturn(Optional.of(7L));

        RecusarSolicitacaoDTO dados = new RecusarSolicitacaoDTO("Não há paciente compatível.");

        SolicitacaoPaciente recusada = new SolicitacaoPaciente();
        recusada.setId(10L);
        recusada.setStatusSolicitacao("RECUSADO");

        when(service.recusarSolicitacao(
                10L, 7L, "Não há paciente compatível.")).thenReturn(recusada);

        var resultado = controller.recusarSolicitacao(
                10L, dados, autenticacao);

        assertEquals("RECUSADO", resultado.getStatusSolicitacao());

        verify(service).recusarSolicitacao(
                10L, 7L, "Não há paciente compatível.");
    }
}