package br.com.unicatolica.odontologia_api.prontuarios.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class ProntuarioNetworkFilter extends OncePerRequestFilter {
    public static final String BLOCK_MESSAGE = "Acesso restrito a dispositivos autorizados na rede local";

    private final List<CidrMatcher> allowed;
    private final List<CidrMatcher> trustedProxies;
    private final ObjectMapper objectMapper;

    public ProntuarioNetworkFilter(ProntuarioProperties properties, ObjectMapper objectMapper) {
        this.allowed = properties.getAllowedCidrs().stream().map(CidrMatcher::new).toList();
        this.trustedProxies = properties.getTrustedProxyCidrs().stream().map(CidrMatcher::new).toList();
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) path = request.getRequestURI();
        return !(path.equals("/api/prontuarios") || path.startsWith("/api/prontuarios/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = resolveClientIp(request);
        if (allowed.stream().noneMatch(cidr -> cidr.matches(ip))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            objectMapper.writeValue(response.getWriter(), Map.of("message", BLOCK_MESSAGE));
            return;
        }
        chain.doFilter(request, response);
    }

    String resolveClientIp(HttpServletRequest request) {
        String remote = request.getRemoteAddr();
        List<String> xff = trustedProxies.stream().anyMatch(cidr -> cidr.matches(remote))
                ? parseForwardedFor(request.getHeader("X-Forwarded-For"))
                : List.of();
        String hop = remote;
        for (int i = xff.size() - 1; i >= 0; i--) {
            String currentHop = hop;
            if (trustedProxies.stream().noneMatch(cidr -> cidr.matches(currentHop))) break;
            hop = xff.get(i);
        }
        return hop;
    }

    private static List<String> parseForwardedFor(String header) {
        if (header == null || header.isBlank()) return List.of();
        List<String> values = java.util.Arrays.stream(header.split(","))
                .map(String::trim)
                .map(value -> value.startsWith("[") && value.contains("]")
                        ? value.substring(1, value.indexOf(']')) : value)
                .toList();
        return values.stream().allMatch(CidrMatcher::isNumericAddress) ? values : List.of();
    }
}
