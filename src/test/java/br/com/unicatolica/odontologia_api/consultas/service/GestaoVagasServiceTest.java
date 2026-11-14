package br.com.unicatolica.odontologia_api.consultas.service;

import br.com.unicatolica.odontologia_api.consultas.dto.BloqueioVagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.GerarTurnosResponseDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.TurnoGradeDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.BloqueioTurnoRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.dto.VagaRequestDTO;
import br.com.unicatolica.odontologia_api.consultas.entity.Vaga;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaGestaoRepository;
import br.com.unicatolica.odontologia_api.consultas.repository.VagaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestaoVagasServiceTest {

    @Mock VagaRepository vagas;
    @Mock VagaGestaoRepository gestao;
    @InjectMocks GestaoVagasService service;

    @Test void rejeitaCadeiraInexistenteOuInativa() {
        when(gestao.cadeiraAtivaExiste(4L)).thenReturn(false);
        var request = new VagaRequestDTO(4L, LocalDate.now().plusDays(1), VagaRequestDTO.Turno.MANHA);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.criar(request));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(vagas, never()).saveAndFlush(any());
    }

    @Test void listarEncaminhaTodosOsFiltrosEDevolveDadosDaCadeira() {
        LocalDate inicio = LocalDate.parse("2026-12-01");
        LocalDate fim = LocalDate.parse("2026-12-31");
        VagaResponseDTO esperada = new VagaResponseDTO(9L, 4L, "04", inicio, "TARDE", true, "Manutenção");
        when(gestao.listar(inicio, fim, 4L, "TARDE", true)).thenReturn(List.of(esperada));

        var resposta = service.listar(inicio, fim, 4L, VagaRequestDTO.Turno.TARDE, true);

        assertEquals(List.of(esperada), resposta);
    }

    @Test void filtrosSemCorrespondenciaRetornamListaVazia() {
        when(gestao.listar(any(), any(), any(), any(), any())).thenReturn(java.util.List.of());

        assertEquals(java.util.List.of(), service.listar(null, null, null, null, null));
    }

    @Test void bloqueioValidoArmazenaMotivoEConsulta() {
        Vaga vaga = new Vaga();
        vaga.setId(7L);
        when(vagas.buscarPorIdParaAtualizacao(7L)).thenReturn(Optional.of(vaga));
        when(gestao.possuiConsultaVinculada(7L)).thenReturn(false);
        when(vagas.saveAndFlush(any(Vaga.class))).thenAnswer(call -> call.getArgument(0));
        VagaResponseDTO resposta = new VagaResponseDTO(7L, 2L, "02", LocalDate.now(), "MANHA", true, "Manutenção");
        when(gestao.buscar(7L)).thenReturn(resposta);

        assertEquals(resposta, service.bloquear(7L, new BloqueioVagaRequestDTO(" Manutenção ")));

        assertEquals(true, vaga.getBloqueada());
        assertEquals("Manutenção", vaga.getMotivoBloqueio());
    }

    @Test void desbloqueioLimpaFlagEMotivo() {
        Vaga vaga = new Vaga();
        vaga.setId(7L);
        vaga.setBloqueada(true);
        vaga.setMotivoBloqueio("Manutenção");
        when(vagas.buscarPorIdParaAtualizacao(7L)).thenReturn(Optional.of(vaga));
        when(vagas.saveAndFlush(any(Vaga.class))).thenAnswer(call -> call.getArgument(0));
        VagaResponseDTO resposta = new VagaResponseDTO(7L, 2L, "02", LocalDate.now(), "MANHA", false, null);
        when(gestao.buscar(7L)).thenReturn(resposta);

        assertEquals(resposta, service.desbloquear(7L));

        assertEquals(false, vaga.getBloqueada());
        assertEquals(null, vaga.getMotivoBloqueio());
    }

    @Test void chaveUnicaViraConflitoHttp409() {
        when(gestao.cadeiraAtivaExiste(4L)).thenReturn(true);
        when(vagas.saveAndFlush(any(Vaga.class))).thenThrow(new DataIntegrityViolationException("uk_vaga"));
        var request = new VagaRequestDTO(4L, LocalDate.now().plusDays(1), VagaRequestDTO.Turno.MANHA);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.criar(request));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test void filtrosInvertidosSaoRejeitados() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.listar(LocalDate.parse("2026-12-02"), LocalDate.parse("2026-12-01"),
                        null, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test void motivoInvalidoNaoAlteraVaga() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.bloquear(7L, new BloqueioVagaRequestDTO(" ")));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
        verify(vagas, never()).buscarPorIdParaAtualizacao(any());
    }

    @Test void consultaCanceladaAindaImpedeRemocao() {
        Vaga vaga = new Vaga();
        vaga.setId(7L);
        when(vagas.buscarPorIdParaAtualizacao(7L)).thenReturn(Optional.of(vaga));
        when(gestao.possuiConsultaVinculada(7L)).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.remover(7L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(vagas, never()).delete(any(Vaga.class));
    }

    @Test void removeVagaSemConsultaVinculada() {
        Vaga vaga = new Vaga();
        vaga.setId(8L);
        when(vagas.buscarPorIdParaAtualizacao(8L)).thenReturn(Optional.of(vaga));
        when(gestao.possuiConsultaVinculada(8L)).thenReturn(false);

        service.remover(8L);

        verify(vagas).delete(vaga);
        verify(vagas).flush();
    }

    @Test void bloqueioRecusaVagaComConsultaAtivaOuHistorica() {
        Vaga vaga = new Vaga();
        vaga.setId(8L);
        when(vagas.buscarPorIdParaAtualizacao(8L)).thenReturn(Optional.of(vaga));
        when(gestao.possuiConsultaVinculada(8L)).thenReturn(true);

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.bloquear(8L, new BloqueioVagaRequestDTO("Manutenção")));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
        verify(vagas, never()).saveAndFlush(any());
    }

    @Test void conflitoDeChaveEstrangeiraDuranteRemocaoVira409() {
        Vaga vaga = new Vaga();
        vaga.setId(8L);
        when(vagas.buscarPorIdParaAtualizacao(8L)).thenReturn(Optional.of(vaga));
        when(gestao.possuiConsultaVinculada(8L)).thenReturn(false);
        org.mockito.Mockito.doThrow(new DataIntegrityViolationException("fk_consulta_vaga"))
                .when(vagas).flush();

        ResponseStatusException erro = assertThrows(ResponseStatusException.class, () -> service.remover(8L));

        assertEquals(HttpStatus.CONFLICT, erro.getStatusCode());
    }

    @Test void geraPeriodoValidoEDevolveContagensDoRepositorio() {
        var inicio = LocalDate.parse("2026-11-09");
        var fim = LocalDate.parse("2026-11-15");
        var esperado = new GerarTurnosResponseDTO(24, 4);
        when(gestao.gerarTurnos(inicio, fim)).thenReturn(esperado);

        assertEquals(esperado, service.gerarTurnos(new GerarTurnosRequestDTO(inicio, fim)));
    }

    @Test void rejeitaGeracaoComPeriodoInvertido() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.gerarTurnos(new GerarTurnosRequestDTO(
                        LocalDate.parse("2026-11-15"), LocalDate.parse("2026-11-09"))));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test void gradeAceitaSemanaSegundaADomingo() {
        var inicio = LocalDate.parse("2026-11-09");
        var fim = LocalDate.parse("2026-11-15");
        var linha = new TurnoGradeDTO(inicio, "MANHA", 1L, "01", null,
                false, null, "LIVRE", null, null, null, null, null, null);
        when(gestao.listarGradeSemanal(inicio, fim, null, null)).thenReturn(List.of(linha));

        assertEquals(List.of(linha), service.listarGradeSemanal(inicio, fim, null, null));
    }

    @Test void gradeRejeitaPeriodoQueNaoSejaSemanaCompleta() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.listarGradeSemanal(LocalDate.parse("2026-11-10"), LocalDate.parse("2026-11-16"), null, null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test void bloqueioDeTurnoCriaVagaQuandoElaAindaNaoExiste() {
        var request = new BloqueioTurnoRequestDTO(2L, LocalDate.parse("2026-11-14"),
                VagaRequestDTO.Turno.TARDE, "Manutenção");
        Vaga vaga = new Vaga();
        vaga.setId(21L);
        VagaResponseDTO resposta = new VagaResponseDTO(21L, 2L, "02", request.data(), "TARDE", true, "Manutenção");
        when(gestao.cadeiraAtivaExiste(2L)).thenReturn(true);
        when(gestao.materializarBloqueio(2L, request.data(), "TARDE", "Manutenção")).thenReturn(21L);
        when(vagas.buscarPorIdParaAtualizacao(21L)).thenReturn(Optional.of(vaga));
        when(gestao.possuiConsultaVinculada(21L)).thenReturn(false);
        when(gestao.buscar(21L)).thenReturn(resposta);

        assertEquals(resposta, service.bloquearTurno(request));
    }
}
