package de.bbajor.pvs.security.pin.service;

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
 * Service for handling PIN reset via recovery email.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PinResetService {

    private final UserAccountRepository userAccountRepository;
    private static final int TOKEN_LENGTH = 32;
    private static final int TOKEN_VALIDITY_HOURS = 24;

    /**
     * Generates a secure reset token for PIN recovery.
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
        userAccount.setPinResetToken(token);
        userAccount.setPinResetTokenExpiry(LocalDateTime.now().plusHours(TOKEN_VALIDITY_HOURS));
        userAccountRepository.save(userAccount);
        log.info("PIN reset token created for user: {}", userAccount.getUsername());
        return token;
    }

    /**
     * Validates and uses a reset token to reset PIN for a user.
     * Returns the user account if token is valid, null otherwise.
     */
    @Transactional
    public Optional<UserAccount> validateAndUseResetToken(String token) {
        Optional<UserAccount> userOpt = userAccountRepository.findAll().stream()
                .filter(u -> token.equals(u.getPinResetToken()))
                .filter(u -> u.getPinResetTokenExpiry() != null)
                .filter(u -> u.getPinResetTokenExpiry().isAfter(LocalDateTime.now()))
                .findFirst();

        if (userOpt.isPresent()) {
            UserAccount user = userOpt.get();
            // Clear reset token (PIN will be set in the reset view)
            user.setPinResetToken(null);
            user.setPinResetTokenExpiry(null);
            userAccountRepository.save(user);
            log.info("PIN reset token validated for user: {}", user.getUsername());
            return Optional.of(user);
        }

        log.warn("Invalid or expired PIN reset token used");
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

