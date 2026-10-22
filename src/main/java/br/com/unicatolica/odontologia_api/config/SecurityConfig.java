
package br.com.unicatolica.odontologia_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/solicitacoes-pacientes")
                                                .hasAuthority("ALUNO")
                                                .requestMatchers(
                                                                "/api/solicitacoes-pacientes/minhas")
                                                .hasAuthority("ALUNO")
                                                .requestMatchers(
                                                                "/api/solicitacoes-pacientes/pendentes",
                                                                "/api/solicitacoes-pacientes/*/aprovar",
                                                                "/api/solicitacoes-pacientes/*/recusar")
                                                .hasAuthority("SECRETARIA")
                                                .requestMatchers("/api/fila-espera/**")
                                                .hasAnyAuthority("SECRETARIA", "ROLE_SECRETARIA")

                                                .anyRequest().authenticated())
                                .httpBasic(Customizer.withDefaults());

                return http.build();
        }
}
