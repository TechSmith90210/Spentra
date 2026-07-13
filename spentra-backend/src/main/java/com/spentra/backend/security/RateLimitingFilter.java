package com.spentra.backend.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter implements Filter {

    // Map to store token buckets for each IP address
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    // Package-private getter for testing
    Map<String, TokenBucket> getBuckets() {
        return buckets;
    }

    // Clean up inactive rate limiting buckets to prevent memory leaks and DoS/OOM issues
    @Scheduled(fixedRate = 3600000) // Run every hour
    public void pruneBuckets() {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        buckets.entrySet().removeIf(entry -> entry.getValue().getLastRefillTime().isBefore(oneHourAgo));
    }

    // Rate limit configuration
    private static final long BUCKET_CAPACITY = 100; // Max burst requests
    private static final long REFILL_TOKENS = 10;   // Tokens added per refill period
    private static final long REFILL_DURATION_SECONDS = 10; // Refill period

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            String ipAddress = getClientIP(httpRequest);
            TokenBucket bucket = buckets.computeIfAbsent(ipAddress, k -> new TokenBucket(BUCKET_CAPACITY));

            if (!bucket.tryConsume()) {
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.setContentType("application/json");
                
                String jsonResponse = String.format(
                        "{\"message\": \"Too many requests. Please try again later.\", " +
                                "\"statusCode\": 429, " +
                                "\"timestamp\": \"%s\"}",
                        java.time.ZonedDateTime.now());
                
                httpResponse.getWriter().write(jsonResponse);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }

    // Inner class representing a Token Bucket for a single client
    static class TokenBucket {
        private final long capacity;
        private double tokens;
        private Instant lastRefillTime;

        public TokenBucket(long capacity) {
            this.capacity = capacity;
            this.tokens = capacity;
            this.lastRefillTime = Instant.now();
        }

        public Instant getLastRefillTime() {
            return lastRefillTime;
        }

        void setLastRefillTime(Instant lastRefillTime) {
            this.lastRefillTime = lastRefillTime;
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1) {
                tokens -= 1;
                return true;
            }
            return false;
        }

        private void refill() {
            Instant now = Instant.now();
            long timeElapsed = java.time.Duration.between(lastRefillTime, now).toSeconds();
            
            if (timeElapsed > 0) {
                double tokensToAdd = (double) timeElapsed / REFILL_DURATION_SECONDS * REFILL_TOKENS;
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefillTime = now;
            }
        }
    }
}
