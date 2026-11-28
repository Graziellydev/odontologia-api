package br.com.unicatolica.odontologia_api.prontuarios.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CidrMatcherTest {
    @Test
    void comparaFaixasIpv4EIpv6() {
        assertTrue(new CidrMatcher("192.168.10.0/24").matches("192.168.10.42"));
        assertFalse(new CidrMatcher("192.168.10.0/24").matches("192.168.11.42"));
        assertTrue(new CidrMatcher("::1/128").matches("::1"));
        assertTrue(new CidrMatcher("fc00::/7").matches("fd00::12"));
    }
}
