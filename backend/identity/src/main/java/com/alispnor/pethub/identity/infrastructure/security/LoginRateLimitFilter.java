package com.alispnor.pethub.identity.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limit em POST /api/v1/auth/login: 5 tentativas por minuto por IP.
 * Retorna 429 com header Retry-After quando excedido.
 *
 * <p>Implementação in-memory com {@link ConcurrentHashMap}. Para múltiplas
 * instâncias do backend, migrar para Bucket4j com backend Redis (Fase 9+).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final int MAX_TENTATIVAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        if (!isLoginRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        var ip = clientIp(request);
        var bucket = buckets.computeIfAbsent(ip, this::newBucket);
        var probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            var retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000L;
            log.info("Rate limit excedido em /auth/login para IP={}, retry em {}s", ip, retryAfterSeconds);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "type", "https://pethub.com/errors/rate-limit-exceeded",
                    "title", "Muitas requisições",
                    "status", 429,
                    "detail", "Muitas tentativas de login. Tente novamente em " + retryAfterSeconds + "s.",
                    "instance", LOGIN_PATH,
                    "retryAfterSeconds", retryAfterSeconds
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && LOGIN_PATH.equals(request.getRequestURI());
    }

    private Bucket newBucket(String ip) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(MAX_TENTATIVAS).refillIntervally(MAX_TENTATIVAS, JANELA).build())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        var forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
