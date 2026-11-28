package br.com.unicatolica.odontologia_api.notificacoes.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LinkWhatsappTest {

    @Test
    void incluiDdiUmaUnicaVezEcodificaTextoParaUrl() {
        assertEquals("https://wa.me/5511999999999?text=Ol%C3%A1%20Ana%20%26%20Jo%C3%A3o",
                LinkWhatsapp.criar("+55 (11) 99999-9999", "Olá Ana & João"));
    }

    @Test
    void rejeitaNumeroQueNaoTemFormatoBrasileiroCompleto() {
        assertNull(LinkWhatsapp.telefoneCanonico("99999"));
    }
}
