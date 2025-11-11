package de.bbajor.pvs.security.email.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service für die Verwaltung von SMTP-Konfigurationen mit verschlüsselten Passwörtern.
 * 
 * <p>Der Verschlüsselungsschlüssel wird über die Environment-Variable {@code SMTP_ENCRYPTION_KEY}
 * bereitgestellt und darf nicht im Code hardcodiert sein.</p>
 * 
 * <p>Verwendet AES-Verschlüsselung für die sichere Speicherung von SMTP-Passwörtern.</p>
 */
@Service
public class SmtpConfigService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    @Value("${smtp.encryption.key:${SMTP_ENCRYPTION_KEY:}}")
    private String encryptionKey;

    private SecretKeySpec secretKey;

    @PostConstruct
    public void init() {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException(
                "SMTP_ENCRYPTION_KEY muss als Environment-Variable gesetzt sein. " +
                "Keine Secrets im Code erlaubt!"
            );
        }
        
        // Validierung: Schlüssel muss 16, 24 oder 32 Bytes lang sein (AES-128/192/256)
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "SMTP_ENCRYPTION_KEY muss 16, 24 oder 32 Bytes lang sein (AES-128/192/256). " +
                "Aktuelle Länge: " + keyBytes.length + " Bytes"
            );
        }
        
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /**
     * Verschlüsselt ein SMTP-Passwort für die sichere Speicherung.
     * 
     * @param password das zu verschlüsselnde Passwort
     * @return Base64-kodierter verschlüsselter String
     * @throws RuntimeException wenn die Verschlüsselung fehlschlägt
     */
    public String encryptPassword(String password) {
        if (password == null || password.isBlank()) {
            return "";
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Verschlüsseln des SMTP-Passworts", e);
        }
    }

    /**
     * Entschlüsselt ein verschlüsseltes SMTP-Passwort.
     * 
     * @param encryptedPassword Base64-kodierter verschlüsselter String
     * @return das entschlüsselte Passwort
     * @throws RuntimeException wenn die Entschlüsselung fehlschlägt
     */
    public String decryptPassword(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            return "";
        }
        
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encryptedPassword);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Entschlüsseln des SMTP-Passworts", e);
        }
    }

    /**
     * Prüft, ob der Verschlüsselungsschlüssel konfiguriert ist.
     * 
     * @return {@code true} wenn der Schlüssel gesetzt ist, sonst {@code false}
     */
    public boolean isEncryptionKeyConfigured() {
        return encryptionKey != null && !encryptionKey.isBlank();
    }
}
