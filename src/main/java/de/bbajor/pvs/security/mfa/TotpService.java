package de.bbajor.pvs.security.mfa;

import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * TOTP (Time-based One-Time Password) Service für Multi-Factor Authentication.
 * 
 * Basiert auf RFC 6238 - TOTP: Time-Based One-Time Password Algorithm.
 * 
 * Features:
 * - Secret-Generierung (Base32)
 * - QR-Code-Generierung für Google Authenticator / Authy
 * - TOTP-Token-Validierung (30s Time-Window, 1 Period Drift)
 * - Backup-Codes-Generierung
 * 
 * @author Agent 3 - MFA & Rate Limiting
 * @since 2025-10-30
 */
@Service
public class TotpService {

    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private final DefaultCodeVerifier verifier;
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    public TotpService() {
        // DefaultCodeVerifier mit 1 Period Drift (erlaubt 30s vor/nach aktueller Zeit)
        this.verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        // Allow 1 discrepancy (30 seconds before/after)
        verifier.setAllowedTimePeriodDiscrepancy(1);
    }

    /**
     * Generiert ein neues TOTP Secret (Base32-encoded).
     * 
     * @return Base32-encoded Secret (z.B. "JBSWY3DPEHPK3PXP")
     */
    public String generateSecret() {
        DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
        return secretGenerator.generate();
    }

    /**
     * Generiert QR-Code für TOTP-Setup (PNG-Format, Base64-encoded Data-URI).
     * 
     * @param secret TOTP Secret (Base32)
     * @param username Username des Users
     * @param issuer App-Name (z.B. "PVS")
     * @return Base64-encoded Data-URI für QR-Code (data:image/png;base64,...)
     * @throws QrGenerationException bei Fehler
     */
    public String generateQrCodeDataUri(String secret, String username, String issuer) throws QrGenerationException {
        QrData data = new QrData.Builder()
                .label(username)
                .secret(secret)
                .issuer(issuer)
                .algorithm(HashingAlgorithm.SHA1) // Google Authenticator default
                .digits(6)
                .period(30)
                .build();
        
        // QR-Generator gibt byte[] zurück
        byte[] qrCodeBytes = qrGenerator.generate(data);
        
        // Konvertiere zu Base64 Data-URI
        String base64Image = Base64.getEncoder().encodeToString(qrCodeBytes);
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Validiert einen TOTP-Code gegen das Secret.
     * 
     * @param secret TOTP Secret (Base32)
     * @param code 6-stelliger TOTP-Code vom User
     * @return true wenn Code valide (innerhalb Time-Window)
     */
    public boolean verifyCode(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }

    /**
     * Generiert Backup-Codes (10 Codes, jeweils 8 Zeichen).
     * 
     * Format: "ABCD-1234"
     * 
     * @return Liste von 10 Backup-Codes
     */
    public List<String> generateBackupCodes() {
        List<String> backupCodes = new ArrayList<>();
        DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator();
        
        for (int i = 0; i < 10; i++) {
            // Generiere 8-Zeichen Code (4 + 4 mit Bindestrich)
            String secret = secretGenerator.generate();
            String code = secret.substring(0, 4) + "-" + secret.substring(4, 8);
            backupCodes.add(code);
        }
        
        return backupCodes;
    }
}
