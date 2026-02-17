package com.infotienda.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final Map<String, RateLimitRule> RULES = Map.of(
            "/api/auth/login", new RateLimitRule(10, Duration.ofMinutes(1)),
            "/api/auth/register", new RateLimitRule(5, Duration.ofMinutes(5)),
            "/api/auth/refresh", new RateLimitRule(30, Duration.ofMinutes(1))
    );

    private final Map<String, ConcurrentLinkedDeque<Long>> requestsByClientAndPath = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        RateLimitRule rule = RULES.get(path);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = path + ":" + resolveClientIp(request);
        long now = System.currentTimeMillis();
        ConcurrentLinkedDeque<Long> timestamps = requestsByClientAndPath.computeIfAbsent(key, ignored -> new ConcurrentLinkedDeque<>());

        int retryAfter = 0;
        boolean limited;
        synchronized (timestamps) {
            trimExpired(timestamps, now, rule.windowMs());
            limited = timestamps.size() >= rule.maxAttempts();
            if (limited) {
                Long oldest = timestamps.peekFirst();
                if (oldest != null) {
                    retryAfter = Math.max(1, (int) ((rule.windowMs() - (now - oldest)) / 1000));
                }
            } else {
                timestamps.addLast(now);
            }
        }

        if (limited) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            if (retryAfter > 0) {
                response.setHeader("Retry-After", String.valueOf(retryAfter));
            }
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void trimExpired(ConcurrentLinkedDeque<Long> timestamps, long now, long windowMs) {
        while (true) {
            Long first = timestamps.peekFirst();
            if (first == null || (now - first) <= windowMs) {
                break;
            }
            timestamps.pollFirst();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded == null || forwarded.isBlank()) {
            return request.getRemoteAddr();
        }
        return Optional.of(forwarded.split(","))
                .filter(parts -> parts.length > 0)
                .map(parts -> parts[0].trim())
                .filter(ip -> !ip.isBlank())
                .orElse(request.getRemoteAddr());
    }

    private record RateLimitRule(int maxAttempts, long windowMs) {
        private RateLimitRule(int maxAttempts, Duration window) {
            this(maxAttempts, window.toMillis());
        }
    }
}
