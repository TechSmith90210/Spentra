package com.spentra.backend.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RateLimitingFilterTest {

    @Test
    public void testRateLimiterPruning() {
        RateLimitingFilter filter = new RateLimitingFilter();
        Map<String, RateLimitingFilter.TokenBucket> buckets = filter.getBuckets();

        // Add an active bucket (last refilled now)
        RateLimitingFilter.TokenBucket activeBucket = new RateLimitingFilter.TokenBucket(100);
        activeBucket.setLastRefillTime(Instant.now());
        buckets.put("192.168.1.1", activeBucket);

        // Add an inactive bucket (last refilled 2 hours ago)
        RateLimitingFilter.TokenBucket inactiveBucket = new RateLimitingFilter.TokenBucket(100);
        inactiveBucket.setLastRefillTime(Instant.now().minusSeconds(7200));
        buckets.put("192.168.1.2", inactiveBucket);

        // Before pruning, both buckets exist
        assertEquals(2, buckets.size());

        // Perform pruning
        filter.pruneBuckets();

        // After pruning, only the active bucket remains
        assertEquals(1, buckets.size());
        assertTrue(buckets.containsKey("192.168.1.1"));
        assertFalse(buckets.containsKey("192.168.1.2"));
    }

    @Test
    public void rejectsNewClientsWhenBucketMapIsFull() throws Exception {
        RateLimitingFilter filter = new RateLimitingFilter();
        Map<String, RateLimitingFilter.TokenBucket> buckets = filter.getBuckets();
        for (int i = 0; i < 10_000; i++) {
            buckets.put("client-" + i, new RateLimitingFilter.TokenBucket(100));
        }

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("new-client");
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        filter.doFilter(request, response, chain);

        assertEquals(10_000, buckets.size());
        verify(response).setStatus(429);
        verify(chain, never()).doFilter(request, response);
    }
}
