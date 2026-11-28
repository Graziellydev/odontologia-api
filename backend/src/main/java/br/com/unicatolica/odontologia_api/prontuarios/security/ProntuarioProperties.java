package br.com.unicatolica.odontologia_api.prontuarios.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;

@ConfigurationProperties(prefix = "app.prontuario")
public class ProntuarioProperties {
    private List<String> allowedCidrs = new ArrayList<>(List.of("127.0.0.1/32", "::1/128"));
    private List<String> trustedProxyCidrs = new ArrayList<>(List.of("127.0.0.1/32", "::1/128"));
    private boolean validarTurnoEstrito = true;
    private Turno turnoManha = new Turno(LocalTime.of(7, 30), LocalTime.NOON);
    private Turno turnoTarde = new Turno(LocalTime.of(13, 30), LocalTime.of(18, 0));

    public static class Turno {
        private LocalTime inicio;
        private LocalTime fim;
        public Turno() { }
        public Turno(LocalTime inicio, LocalTime fim) { this.inicio = inicio; this.fim = fim; }
        public LocalTime getInicio() { return inicio; }
        public void setInicio(LocalTime inicio) { this.inicio = inicio; }
        public LocalTime getFim() { return fim; }
        public void setFim(LocalTime fim) { this.fim = fim; }
        public boolean contem(LocalTime hora) {
            if (inicio == null || fim == null || !inicio.isBefore(fim)) {
                throw new IllegalStateException("Os limites de turno devem possuir inicio anterior ao fim.");
            }
            return !hora.isBefore(inicio) && hora.isBefore(fim);
        }
    }

    public List<String> getAllowedCidrs() { return allowedCidrs; }
    public void setAllowedCidrs(List<String> allowedCidrs) { this.allowedCidrs = allowedCidrs; }
    public List<String> getTrustedProxyCidrs() { return trustedProxyCidrs; }
    public void setTrustedProxyCidrs(List<String> trustedProxyCidrs) { this.trustedProxyCidrs = trustedProxyCidrs; }
    public boolean isValidarTurnoEstrito() { return validarTurnoEstrito; }
    public void setValidarTurnoEstrito(boolean validarTurnoEstrito) { this.validarTurnoEstrito = validarTurnoEstrito; }
    public Turno getTurnoManha() { return turnoManha; }
    public void setTurnoManha(Turno turnoManha) { this.turnoManha = turnoManha; }
    public Turno getTurnoTarde() { return turnoTarde; }
    public void setTurnoTarde(Turno turnoTarde) { this.turnoTarde = turnoTarde; }
}
