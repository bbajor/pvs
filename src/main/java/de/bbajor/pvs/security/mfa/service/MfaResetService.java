package de.bbajor.pvs.security.mfa.service;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for handling MFA reset via recovery email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MfaResetService {

    private final UserAccountRepository userAccountRepository;
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_VALIDITY_HOURS = 24;

    /**
     * Generates a secure reset token for MFA recovery.
     */
    public String generateResetToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[TOKEN_LENGTH];
        random.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Creates a reset token for the user and stores it with expiry time.
     */
    @Transactional
    public String createResetToken(UserAccount userAccount) {
        String token = generateResetToken();
        userAccount.setMfaResetToken(token);
        userAccount.setMfaResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userAccountRepository.save(userAccount);
        log.info("MFA reset token created for user: {}", userAccount.getUsername());
        return token;
    }

    /**
     * Validates and uses a reset token to reset MFA for a user.
     * Returns the user account if token is valid, null otherwise.
     */
    @Transactional
    public Optional<UserAccount> validateAndUseResetToken(String token) {
        Optional<UserAccount> userOpt = userAccountRepository.findAll().stream()
                .filter(u -> token.equals(u.getMfaResetToken()))
                .filter(u -> u.getMfaResetTokenExpiry() != null)
                .filter(u -> u.getMfaResetTokenExpiry().isAfter(LocalDateTime.now()))
                .findFirst();

        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            // Reset MFA
            user.setMfaEnabled(false);
            user.setMfaSecret(null);
            user.setMfaResetToken(null);
            user.setMfaResetTokenExpiry(null);
            userAccountRepository.save(user);
            log.info("MFA reset token used successfully for user: {}", user.getUsername());
            return Optional.of(user);
        }

        log.warn("Invalid or expired MFA reset token used");
        return Optional.empty();
    }

    /**
     * Checks if a user has a verified recovery email.
     */
    public boolean hasVerifiedRecoveryEmail(UserAccount userAccount) {
        return userAccount.getRecoveryEmail() != null 
                && !userAccount.getRecoveryEmail().isEmpty()
                && userAccount.isRecoveryEmailVerified();
    }
}

