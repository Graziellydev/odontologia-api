package br.com.unicatolica.odontologia_api.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(br.com.unicatolica.odontologia_api.prontuarios.security.ProntuarioProperties.class)
public class SecurityConfig {

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        private static final RequestMatcher COORDENADOR_REQUEST_MATCHER = request -> {
                String path = request.getServletPath();
                return path.startsWith("/api/coordenador/");
        };

        private static final AuthenticationEntryPoint METRICAS_AUTHENTICATION_ENTRY_POINT =
                        (request, response, exception) -> escreverErroSeguranca(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Autenticação necessária.\"}");

        private static final AccessDeniedHandler METRICAS_ACCESS_DENIED_HANDLER =
                        (request, response, exception) -> escreverErroSeguranca(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "{\"codigo\":\"SEM_PERMISSAO\",\"mensagem\":\"Perfil COORDENADOR necessário.\"}");

        private static final AuthenticationEntryPoint API_AUTHENTICATION_ENTRY_POINT =
                        (request, response, exception) -> escreverErroSeguranca(
                                        response,
                                        HttpStatus.UNAUTHORIZED,
                                        "{\"codigo\":\"NAO_AUTENTICADO\",\"mensagem\":\"Autentica\\u00e7\\u00e3o necess\\u00e1ria.\"}");

        private static final AccessDeniedHandler API_ACCESS_DENIED_HANDLER =
                        (request, response, exception) -> escreverErroSeguranca(
                                        response,
                                        HttpStatus.FORBIDDEN,
                                        "{\"codigo\":\"SEM_PERMISSAO\",\"mensagem\":\"Perfil sem permissao para esta operacao.\"}");

        private static void escreverErroSeguranca(
                        HttpServletResponse response,
                        HttpStatus status,
                        String json) throws IOException {
                response.setStatus(status.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.getWriter().write(json);
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .csrf(csrf -> csrf
                                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/auth/**").permitAll()
                                                .requestMatchers("/api/pacientes/**").hasAuthority("SECRETARIA")

                                                .requestMatchers(HttpMethod.GET, "/api/prontuarios/paciente/**")
                                                .hasAnyAuthority("ALUNO", "COORDENADOR")
                                                .requestMatchers(HttpMethod.POST, "/api/prontuarios")
                                                .hasAuthority("ALUNO")
                                                .requestMatchers(HttpMethod.POST, "/api/prontuarios/*/adendos")
                                                .hasAuthority("ALUNO")
                                                .requestMatchers("/api/prontuarios/**").denyAll()

                                                .requestMatchers(
                                                                "/api/coordenador/metricas",
                                                                "/api/coordenador/metricas/**",
                                                                "/api/coordenador/desempenho-historico",
                                                                "/api/coordenador/desempenho-historico/**")
                                                .hasAuthority("COORDENADOR")

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

                                                .requestMatchers("/api/consultas/**")
                                                .hasAuthority("SECRETARIA")

                                                .requestMatchers("/api/vagas/**", "/api/cadeiras/**")
                                                .hasAuthority("SECRETARIA")
                                                
                                                .anyRequest().authenticated())
                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(API_AUTHENTICATION_ENTRY_POINT)
                                                .accessDeniedHandler(API_ACCESS_DENIED_HANDLER)
                                                .defaultAuthenticationEntryPointFor(
                                                                METRICAS_AUTHENTICATION_ENTRY_POINT,
                                                                COORDENADOR_REQUEST_MATCHER)
                                                .defaultAccessDeniedHandlerFor(
                                                                METRICAS_ACCESS_DENIED_HANDLER,
                                                                COORDENADOR_REQUEST_MATCHER))
                                .httpBasic(httpBasic -> httpBasic.disable());

                return http.build();
        }
}
