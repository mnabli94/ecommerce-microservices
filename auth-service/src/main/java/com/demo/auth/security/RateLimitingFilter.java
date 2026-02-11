package com.demo.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static class RequestInfo {
        int count;
        Instant windowStart;

        RequestInfo() {
            this.count = 0;
            this.windowStart = Instant.now();
        }
    }

    private final ConcurrentHashMap<String, RequestInfo> requestCounts = new ConcurrentHashMap<>();

    private static final int LOGIN_MAX_REQUESTS = 5;
    private static final int GENERAL_MAX_REQUESTS = 10;
    private static final int WINDOW_MINUTES = 1;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIpAddress(request);
        String path = request.getRequestURI();
        String key = clientIp + ":" + (path.contains("/token") ? "login" : "general");

        RequestInfo info = requestCounts.computeIfAbsent(key, k -> new RequestInfo());

        if (info.windowStart.plus(WINDOW_MINUTES, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            info.count = 0;
            info.windowStart = Instant.now();
        }

        int maxRequests = key.endsWith("login") ? LOGIN_MAX_REQUESTS : GENERAL_MAX_REQUESTS;

        if (info.count >= maxRequests) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {
                        "error": "Too Many Requests",
                        "message": "Rate limit exceeded. Please try again later.",
                        "retryAfter": 60
                    }
                    """);
            response.setHeader("Retry-After", "60");
            return;
        }

        info.count++;
        filterChain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}