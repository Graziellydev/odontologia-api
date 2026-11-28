package br.com.unicatolica.odontologia_api.notificacoes.controller;

import br.com.unicatolica.odontologia_api.notificacoes.repository.NotificacaoConsultaQueryRepository;
import br.com.unicatolica.odontologia_api.notificacoes.service.Lembrete24hService;
import br.com.unicatolica.odontologia_api.security.SessaoAluno;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoControllerTest {
    @Mock private NotificacaoConsultaQueryRepository notificacoes;
    @Mock private Lembrete24hService lembretes;
    @Mock private SessaoAluno sessao;
    @InjectMocks private NotificacaoController controller;

    @Test
    void listaExigeSecretariaEAplicaFiltros() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(notificacoes.listar(null, null, "LEMBRETE_24H", "PENDENTE")).thenReturn(List.of());
        assertEquals(0, controller.listar(null, null, "LEMBRETE_24H", "PENDENTE", request).size());
        verify(sessao).exigirSecretaria(request);
        verify(notificacoes).listar(null, null, "LEMBRETE_24H", "PENDENTE");
    }

    @Test
    void rejeitaPeriodoInvertidoAntesDeConsultarDados() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                controller.listar(LocalDate.of(2026, 11, 24), LocalDate.of(2026, 11, 23),
                        null, null, request));
        verifyNoInteractions(notificacoes);
    }

    @Test
    void processamentoManualExigeSecretaria() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(lembretes.processar(any())).thenReturn(2);
        assertEquals(2, controller.processar(request).criadas());
        verify(sessao).exigirSecretaria(request);
    }
}
