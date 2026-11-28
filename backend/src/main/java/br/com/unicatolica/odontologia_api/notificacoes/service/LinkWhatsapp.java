package br.com.unicatolica.odontologia_api.notificacoes.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public final class LinkWhatsapp {

    private LinkWhatsapp() {
    }

    public static String telefoneCanonico(String telefone) {
        if (telefone == null) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            return digitos;
        }
        if (digitos.length() == 10 || digitos.length() == 11) {
            return "55" + digitos;
        }
        return null;
    }

    public static String criar(String telefone, String mensagem) {
        String canonico = telefoneCanonico(telefone);
        if (canonico == null || mensagem == null) {
            return null;
        }
        String texto = URLEncoder.encode(mensagem, StandardCharsets.UTF_8).replace("+", "%20");
        return "https://wa.me/" + canonico + "?text=" + texto;
    }
}
