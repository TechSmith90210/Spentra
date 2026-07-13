package com.spentra.backend.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
