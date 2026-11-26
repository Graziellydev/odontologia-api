package br.com.unicatolica.odontologia_api.relatorios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import br.com.unicatolica.odontologia_api.relatorios.repository.RelatorioCoordenacaoRepository;
import br.com.unicatolica.odontologia_api.relatorios.service.RelatorioCoordenacaoService;

class RelatorioCoordenacaoServiceTest {
    @Test void agrupaFaixasPercentuaisEOrigemRaraEmOutros() {
        var repo=mock(RelatorioCoordenacaoRepository.class);
        var svc=new RelatorioCoordenacaoService(repo);
        var e=new java.util.ArrayList<RelatorioCoordenacaoRepository.EventoPaciente>();
        // 3 patients in the 18-29 age range and common neighborhood; 2 rare neighborhood rows suppressed.
        for(long i=1;i<=3;i++) e.add(new RelatorioCoordenacaoRepository.EventoPaciente(i,LocalDate.of(2000,1,1),"Centro","Quixadá",LocalDate.of(2026,1,1)));
        e.add(new RelatorioCoordenacaoRepository.EventoPaciente(4,LocalDate.of(1990,1,1),"Bairro raro","Quixadá",LocalDate.of(2026,1,1)));
        e.add(new RelatorioCoordenacaoRepository.EventoPaciente(5,LocalDate.of(1990,1,1),"Bairro raro","Quixadá",LocalDate.of(2026,1,1)));
        when(repo.buscarEventos(null,null,null,null,"TODOS")).thenReturn(e);
        var r=svc.demografico(null,null,null,null,null);
        assertEquals(5,r.baseAnalise().totalPacientesUnicos());
        assertEquals(3,r.distribuicaoFaixaEtaria().get(1).quantidade());
        assertEquals(60.0,r.distribuicaoFaixaEtaria().get(1).percentual());
        assertEquals("Outros bairros / municípios",r.distribuicaoTerritorial().getLast().categoria());
        assertEquals(2,r.distribuicaoTerritorial().getLast().quantidade());
    }
    @Test void rejeitaIntervaloInvalido() {
        var svc=new RelatorioCoordenacaoService(mock(RelatorioCoordenacaoRepository.class));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->svc.demografico("2026.3",null,null,null,null));
    }
}
