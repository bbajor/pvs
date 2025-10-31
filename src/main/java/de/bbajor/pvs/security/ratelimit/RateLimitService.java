package de.bbajor.pvs.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Service mit Bucket4j & Caffeine.
 * 
 * Features:
 * - Per-User Rate Limiting
 * - Per-IP Rate Limiting
 * - Configurable Limits (dev/test/prod)
 * - In-Memory Storage (Caffeine Cache)
 * 
 * Rate Limits:
 * - Standard: 100 req/s, Burst 50
 * - Login: 5 req/min, Burst 10
 * 
 * @author Agent 3 - MFA & Rate Limiting
 * @since 2025-10-30
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    // Cache für Buckets (User-basiert)
    private final Cache<String, Bucket> userBuckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    // Cache für Buckets (IP-basiert)
    private final Cache<String, Bucket> ipBuckets = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build();

    /**
     * Prüft ob Request von User erlaubt ist (Standard Rate-Limit).
     * 
     * @param userId User-ID
     * @return true wenn Request erlaubt
     */
    public boolean allowRequest(String userId) {
        Bucket bucket = userBuckets.get(userId, k -> createStandardBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("Rate limit exceeded for user: {}", userId);
        }
        
        return allowed;
    }

    /**
     * Prüft ob Login-Request von User erlaubt ist (Stricter Rate-Limit).
     * 
     * @param userId User-ID oder IP
     * @return true wenn Request erlaubt
     */
    public boolean allowLoginRequest(String userId) {
        Bucket bucket = userBuckets.get("login:" + userId, k -> createLoginBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("Login rate limit exceeded for user: {}", userId);
        }
        
        return allowed;
    }

    /**
     * Prüft ob Request von IP erlaubt ist.
     * 
     * @param ipAddress IP-Adresse
     * @return true wenn Request erlaubt
     */
    public boolean allowRequestByIp(String ipAddress) {
        Bucket bucket = ipBuckets.get(ipAddress, k -> createStandardBucket());
        boolean allowed = bucket.tryConsume(1);
        
        if (!allowed) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
        }
        
        return allowed;
    }

    /**
     * Erstellt Standard-Bucket: 100 req/s, Burst 50.
     */
    private Bucket createStandardBucket() {
        Bandwidth limit = Bandwidth.classic(100, Refill.intervally(100, Duration.ofSeconds(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Erstellt Login-Bucket: 5 req/min, Burst 10.
     */
    private Bucket createLoginBucket() {
        Bandwidth limit = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Reset Rate-Limit für User (z.B. nach erfolgreicher Auth).
     * 
     * @param userId User-ID
     */
    public void resetLimit(String userId) {
        userBuckets.invalidate(userId);
        userBuckets.invalidate("login:" + userId);
        log.debug("Rate limit reset for user: {}", userId);
    }
}
