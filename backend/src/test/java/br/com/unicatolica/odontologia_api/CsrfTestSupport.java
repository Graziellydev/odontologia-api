package br.com.unicatolica.odontologia_api;

import jakarta.servlet.http.Cookie;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

public final class CsrfTestSupport {
    private CsrfTestSupport() { }

    public static RequestPostProcessor csrf() {
        return request -> {
            request.setCookies(new Cookie("XSRF-TOKEN", "security-test-token"));
            request.addHeader("X-XSRF-TOKEN", "security-test-token");
            return request;
        };
    }
}
