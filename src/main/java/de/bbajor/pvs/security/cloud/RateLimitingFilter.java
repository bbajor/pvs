package de.bbajor.pvs.security.cloud;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple rate limiting filter for login endpoints.
 * Limits requests per IP address to prevent brute-force attacks.
 * 
 * In production, consider using Redis-based rate limiting for distributed systems.
 */
@Component
@Order(1)
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    // Rate limit: 5 requests per minute per IP
    private static final int MAX_REQUESTS_PER_MINUTE = 5;
    private static final long TIME_WINDOW_MS = 60_000; // 1 minute

    // In-memory store (use Redis for distributed systems)
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Only apply rate limiting to login endpoints
        String path = request.getRequestURI();
        if (path.contains("/login") || path.contains("/api/auth")) {
            String clientIp = getClientIp(request);
            RequestCounter counter = requestCounts.computeIfAbsent(clientIp, k -> new RequestCounter());
            
            long now = System.currentTimeMillis();
            if (now - counter.lastReset > TIME_WINDOW_MS) {
                // Reset counter if time window expired
                counter.count.set(0);
                counter.lastReset = now;
            }
            
            int currentCount = counter.count.incrementAndGet();
            if (currentCount > MAX_REQUESTS_PER_MINUTE) {
                log.warn("Rate limit exceeded for IP: {} ({} requests in window)", clientIp, currentCount);
                response.setStatus(429); // HTTP 429 Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Get client IP address, considering X-Forwarded-For header from reverse proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Request counter for rate limiting.
     */
    private static class RequestCounter {
        final AtomicInteger count = new AtomicInteger(0);
        long lastReset = System.currentTimeMillis();
    }
}


