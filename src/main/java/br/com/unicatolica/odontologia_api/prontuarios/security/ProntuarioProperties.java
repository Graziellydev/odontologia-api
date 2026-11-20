package br.com.unicatolica.odontologia_api.prontuarios.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.prontuario")
public class ProntuarioProperties {
    private List<String> allowedCidrs = new ArrayList<>(List.of("127.0.0.1/32", "::1/128"));
    private List<String> trustedProxyCidrs = new ArrayList<>(List.of("127.0.0.1/32", "::1/128"));
    private boolean validarTurnoEstrito = true;

    public List<String> getAllowedCidrs() { return allowedCidrs; }
    public void setAllowedCidrs(List<String> allowedCidrs) { this.allowedCidrs = allowedCidrs; }
    public List<String> getTrustedProxyCidrs() { return trustedProxyCidrs; }
    public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) { this.trustedProxyCidrs = trustedProxyCidrs; }
    public boolean isValidarTurnoEstrito() { return validarTurnoEstrito; }
    public void setValidarTurnoEstrito(boolean validarTurnoEstrito) { this.validarTurnoEstrito = validarTurnoEstrito; }
}
