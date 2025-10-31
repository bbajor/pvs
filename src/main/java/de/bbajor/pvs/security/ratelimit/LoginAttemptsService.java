package de.bbajor.pvs.security.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Login Attempts Tracking Service.
 * 
 * Features:
 * - Failed-Login-Counter pro User
 * - Account-Lockout nach X Versuchen
 * - Lockout-Duration (15 Minuten)
 * - Auto-Unlock nach Timeout
 * 
 * Limits:
 * - Max Failed Attempts: 5
 * - Lockout Duration: 15 Minuten
 * 
 * @author Agent 3 - MFA & Rate Limiting
 * @since 2025-10-30
 */
@Service
public class LoginAttemptsService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptsService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    // Cache für Failed-Attempts (Username -> Count)
    private final Cache<String, AtomicInteger> attemptsCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(LOCKOUT_DURATION.toMinutes(), TimeUnit.MINUTES)
            .build();

    // Cache für Lockout-Timestamp (Username -> LocalDateTime)
    private final Cache<String, LocalDateTime> lockoutCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(LOCKOUT_DURATION.toMinutes(), TimeUnit.MINUTES)
            .build();

    /**
     * Registriert erfolgreichen Login (reset counter).
     * 
     * @param username Username
     */
    public void loginSucceeded(String username) {
        attemptsCache.invalidate(username);
        lockoutCache.invalidate(username);
        log.debug("Login succeeded for user: {} - attempts reset", username);
    }

    /**
     * Registriert fehlgeschlagenen Login (increment counter).
     * 
     * @param username Username
     */
    public void loginFailed(String username) {
        AtomicInteger attempts = attemptsCache.get(username, k -> new AtomicInteger(0));
        int failedAttempts = attempts.incrementAndGet();
        
        log.warn("Login failed for user: {} - attempts: {}/{}", username, failedAttempts, MAX_FAILED_ATTEMPTS);
        
        if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
            lockAccount(username);
        }
    }

    /**
     * Prüft ob Account gesperrt ist.
     * 
     * @param username Username
     * @return true wenn Account gesperrt
     */
    public boolean isLocked(String username) {
        LocalDateTime lockoutTime = lockoutCache.getIfPresent(username);
        
        if (lockoutTime == null) {
            return false;
        }
        
        // Prüfe ob Lockout abgelaufen
        if (LocalDateTime.now().isAfter(lockoutTime.plus(LOCKOUT_DURATION))) {
            unlock(username);
            return false;
        }
        
        return true;
    }

    /**
     * Gibt verbleibende Failed-Attempts zurück.
     * 
     * @param username Username
     * @return Anzahl Failed-Attempts
     */
    public int getFailedAttempts(String username) {
        AtomicInteger attempts = attemptsCache.getIfPresent(username);
        return attempts != null ? attempts.get() : 0;
    }

    /**
     * Gibt verbleibende Attempts bis Lockout zurück.
     * 
     * @param username Username
     * @return Verbleibende Attempts
     */
    public int getRemainingAttempts(String username) {
        return MAX_FAILED_ATTEMPTS - getFailedAttempts(username);
    }

    /**
     * Sperrt Account.
     * 
     * @param username Username
     */
    private void lockAccount(String username) {
        lockoutCache.put(username, LocalDateTime.now());
        log.warn("Account locked for user: {} - {} minutes", username, LOCKOUT_DURATION.toMinutes());
    }

    /**
     * Entsperrt Account (Admin-Funktion).
     * 
     * @param username Username
     */
    public void unlock(String username) {
        attemptsCache.invalidate(username);
        lockoutCache.invalidate(username);
        log.info("Account unlocked for user: {}", username);
    }

    /**
     * Gibt Lockout-Timestamp zurück.
     * 
     * @param username Username
     * @return Lockout-Zeit oder null
     */
    public LocalDateTime getLockoutTime(String username) {
        return lockoutCache.getIfPresent(username);
    }
}
