package br.com.unicatolica.odontologia_api.prontuarios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ProntuarioClockConfig {
    @Bean
    Clock prontuarioClock() {
        return Clock.system(ZoneId.of("America/Sao_Paulo"));
    }
}
