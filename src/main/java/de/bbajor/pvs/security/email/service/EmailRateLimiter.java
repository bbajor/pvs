package de.bbajor.pvs.security.email.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiter for email sending to prevent abuse.
 * Limits emails per recipient and globally.
 * 
 * In production, consider using Redis-based rate limiting for distributed systems.
 */
@Component
@Slf4j
public class EmailRateLimiter {

    // Rate limits
    private static final int MAX_EMAILS_PER_RECIPIENT_PER_HOUR = 10;
    private static final int MAX_EMAILS_GLOBAL_PER_HOUR = 100;
    private static final long TIME_WINDOW_MS = 3_600_000; // 1 hour

    // Per-recipient tracking
    private final Map<String, EmailCounter> recipientCounts = new ConcurrentHashMap<>();
    
    // Global tracking
    private final EmailCounter globalCounter = new EmailCounter();

    /**
     * Checks if an email can be sent to the given recipient.
     * 
     * @param recipientEmail the recipient email address
     * @return true if email can be sent, false if rate limit exceeded
     */
    public boolean canSendEmail(String recipientEmail) {
        long now = System.currentTimeMillis();
        
        // Check global rate limit
        if (now - globalCounter.lastReset > TIME_WINDOW_MS) {
            globalCounter.count.set(0);
            globalCounter.lastReset = now;
        }
        
        int globalCount = globalCounter.count.incrementAndGet();
        if (globalCount > MAX_EMAILS_GLOBAL_PER_HOUR) {
            log.warn("Global email rate limit exceeded: {} emails in last hour", globalCount);
            return false;
        }
        
        // Check per-recipient rate limit
        EmailCounter recipientCounter = recipientCounts.computeIfAbsent(recipientEmail, k -> new EmailCounter());
        
        if (now - recipientCounter.lastReset > TIME_WINDOW_MS) {
            recipientCounter.count.set(0);
            recipientCounter.lastReset = now;
        }
        
        int recipientCount = recipientCounter.count.incrementAndGet();
        if (recipientCount > MAX_EMAILS_PER_RECIPIENT_PER_HOUR) {
            log.warn("Email rate limit exceeded for recipient {}: {} emails in last hour", 
                    recipientEmail, recipientCount);
            return false;
        }
        
        return true;
    }

    /**
     * Resets rate limit counters (for testing or manual reset).
     */
    public void reset() {
        recipientCounts.clear();
        globalCounter.count.set(0);
        globalCounter.lastReset = System.currentTimeMillis();
    }

    /**
     * Email counter for rate limiting.
     */
    private static class EmailCounter {
        final AtomicInteger count = new AtomicInteger(0);
        volatile long lastReset = System.currentTimeMillis();
    }
}
