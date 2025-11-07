package de.bbajor.pvs.security.mfa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.KeyRepresentation;

/**
 * Service for Multi-Factor Authentication (MFA) using TOTP (Time-based One-Time Password).
 * 
 * <p>
 * This service provides functionality for:
 * <ul>
 * <li>Generating TOTP secrets</li>
 * <li>Creating QR codes for authenticator apps</li>
 * <li>Verifying TOTP codes</li>
 * </ul>
 * </p>
 */
@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
    private static final int SECRET_LENGTH = 32; // 256 bits
    private static final int QR_CODE_SIZE = 300;

    private final GoogleAuthenticator googleAuthenticator;
    private final String issuerName;

    public MfaService(@Value("${app.mfa.issuer:PVS}") String issuerName) {
        this.issuerName = issuerName;
        
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setTimeStepSizeInMillis(30000) // 30 seconds
                .setWindowSize(3) // Allow 3 time steps (90 seconds) for clock skew
                .setKeyRepresentation(KeyRepresentation.BASE32)
                .build();
        
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    /**
     * Generates a new TOTP secret for a user.
     * Uses GoogleAuthenticator to generate a proper Base32-encoded secret.
     * 
     * @return Base32-encoded secret string
     */
    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    /**
     * Generates a QR code image as Base64-encoded PNG for the given secret and username.
     * 
     * @param username the username to include in the QR code label
     * @param secret the TOTP secret (Base32-encoded)
     * @return Base64-encoded PNG image data
     * @throws MfaException if QR code generation fails
     */
    public String generateQrCode(String username, String secret) {
        return generateQrCode(username, secret, false);
    }

    /**
     * Generates a QR code image as Base64-encoded PNG for the given secret and username.
     * 
     * @param username the username to include in the QR code label
     * @param secret the TOTP secret (Base32-encoded)
     * @param isReset whether this is a reset (adds timestamp to label to help identify old entries)
     * @return Base64-encoded PNG image data
     * @throws MfaException if QR code generation fails
     */
    public String generateQrCode(String username, String secret, boolean isReset) {
        try {
            // Create TOTP URI according to Google Authenticator format
            String label = username;
            if (isReset) {
                // Add timestamp to label when resetting to help users identify and delete old entries
                long timestamp = System.currentTimeMillis();
                String dateStr = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(new java.util.Date(timestamp));
                label = username + " [" + dateStr + "]";
            }
            String totpUri = String.format(
                    "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                    URLEncoder.encode(issuerName, StandardCharsets.UTF_8),
                    URLEncoder.encode(label, StandardCharsets.UTF_8),
                    URLEncoder.encode(secret, StandardCharsets.UTF_8),
                    URLEncoder.encode(issuerName, StandardCharsets.UTF_8));

            log.debug("Generating QR code for user: {}", username);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(totpUri, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE);

            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return Base64.getEncoder().encodeToString(pngData);
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code for user: {}", username, e);
            throw new MfaException("Failed to generate QR code", e);
        }
    }

    /**
     * Verifies a TOTP code against the given secret.
     * 
     * @param secret the TOTP secret (Base32-encoded)
     * @param code the 6-digit code to verify
     * @return true if the code is valid, false otherwise
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || code.length() != 6) {
            log.debug("Invalid secret or code format");
            return false;
        }

        try {
            int codeInt = Integer.parseInt(code);
            boolean isValid = googleAuthenticator.authorize(secret, codeInt);
            
            log.debug("MFA code verification: {}", isValid ? "valid" : "invalid");
            return isValid;
        } catch (NumberFormatException e) {
            log.debug("Invalid code format: {}", code);
            return false;
        } catch (Exception e) {
            log.error("Error verifying MFA code", e);
            return false;
        }
    }
}
