package com.gnimah.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Test isolé (pas de contexte Spring) : le rate-limiter est un singleton à état partagé par tout le
 * contexte applicatif, donc l'exercer via de vrais appels HTTP dans une suite d'intégration risquerait
 * de faire échouer d'autres tests qui se connectent légitimement. Ici on instancie directement le filtre.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    private final RateLimitFilter filter = new RateLimitFilter();

    private HttpServletRequest loginRequestFrom(String ip) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn(ip);
        return request;
    }

    @Test
    void allowsRequestsUnderTheThreshold() throws Exception {
        HttpServletRequest request = loginRequestFrom("10.0.0.1");

        for (int i = 0; i < 9; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(9)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }

    @Test
    void blocksRequestsAboveTheThreshold_forThatIpOnly() throws Exception {
        HttpServletRequest attacker = loginRequestFrom("10.0.0.2");
        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(attacker, response, filterChain);
        }
        filter.doFilterInternal(attacker, response, filterChain);

        verify(filterChain, times(10)).doFilter(attacker, response);
        verify(response).setStatus(429);
        assertThat(body.toString()).contains("Trop de tentatives");

        // Une autre IP ne doit pas être affectée par les tentatives de la première.
        HttpServletRequest other = loginRequestFrom("10.0.0.3");
        filter.doFilterInternal(other, response, filterChain);
        verify(filterChain).doFilter(other, response);
    }

    @Test
    void doesNotRateLimit_nonAuthEndpoints() throws Exception {
        // GET est exclu dès la vérification de méthode — l'URI n'est même pas consultée.
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");

        for (int i = 0; i < 15; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(15)).doFilter(request, response);
        verify(response, never()).setStatus(429);
    }
}
