package br.com.unicatolica.odontologia_api.prontuarios.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ProntuarioNetworkFilterTest {
    @Test
    void bloqueiaIpForaDaRedeComMensagemExata() throws Exception {
        ProntuarioProperties properties = new ProntuarioProperties();
        properties.setAllowedCidrs(List.of("127.0.0.1/32", "::1/128"));
        ProntuarioNetworkFilter filter = new ProntuarioNetworkFilter(properties, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/prontuarios/paciente/5");
        request.setServletPath("/api/prontuarios/paciente/5");
        request.setRemoteAddr("203.0.113.25");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, mock(jakarta.servlet.FilterChain.class));

        assertEquals(403, response.getStatus());
        assertEquals(ProntuarioNetworkFilter.BLOCK_MESSAGE,
                new ObjectMapper().readTree(response.getContentAsString()).get("message").asString());
    }

    @Test
    void cabeçalhoForjadoDeClienteDiretoEIgnorado() {
        ProntuarioProperties properties = new ProntuarioProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8"));
        ProntuarioNetworkFilter filter = new ProntuarioNetworkFilter(properties, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.8");
        request.addHeader("X-Forwarded-For", "127.0.0.1");

        assertEquals("203.0.113.8", filter.resolveClientIp(request));
    }

    @Test
    void consideraCadeiaDeProxyEIgnoraValorXffMalformado() {
        ProntuarioProperties properties = new ProntuarioProperties();
        properties.setTrustedProxyCidrs(List.of("10.0.0.0/8"));
        ProntuarioNetworkFilter filter = new ProntuarioNetworkFilter(properties, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.2");
        request.addHeader("X-Forwarded-For", "198.51.100.4, 10.1.2.3");
        assertEquals("198.51.100.4", filter.resolveClientIp(request));
        MockHttpServletRequest malformed = new MockHttpServletRequest();
        malformed.setRemoteAddr("10.0.0.2");
        malformed.addHeader("X-Forwarded-For", "127.0.0.1, invalid");
        assertEquals("10.0.0.2", filter.resolveClientIp(malformed));
    }
}
