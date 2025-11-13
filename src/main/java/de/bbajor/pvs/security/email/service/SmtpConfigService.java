package de.bbajor.pvs.security.email.service;

import de.bbajor.pvs.security.email.model.SmtpConfig;
import de.bbajor.pvs.security.email.model.SmtpSecurityMethod;
import de.bbajor.pvs.security.email.repository.SmtpConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service for managing SMTP configuration.
 * Uses encryption for storing passwords.
 */
@Service
public class SmtpConfigService {

    private static final Logger log = LoggerFactory.getLogger(SmtpConfigService.class);
    
    // In production, this should come from environment variable or secure key management
    // The key must be exactly 16 bytes (128-bit) for AES-128 or 32 bytes (256-bit) for AES-256
    private static final String ENCRYPTION_KEY = getEncryptionKey();
    
    private static final String ENCRYPTION_SALT = "smtp-config-salt"; // Should be unique per installation

    private final SmtpConfigRepository smtpConfigRepository;
    private final TextEncryptor encryptor;

    public SmtpConfigService(SmtpConfigRepository smtpConfigRepository) {
        this.smtpConfigRepository = smtpConfigRepository;
        
        // Validate encryption key before initializing encryptor
        if (ENCRYPTION_KEY == null || ENCRYPTION_KEY.isBlank()) {
            String activeProfile = System.getProperty("spring.profiles.active", 
                    System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", ""));
            boolean isProduction = activeProfile.contains("prod") || 
                    System.getenv("SPRING_PROFILES_ACTIVE") != null && 
                    System.getenv("SPRING_PROFILES_ACTIVE").contains("prod");
            
            if (isProduction) {
                throw new IllegalStateException(
                    "SMTP_ENCRYPTION_KEY muss als Environment-Variable gesetzt sein. " +
                    "Keine Secrets im Code erlaubt!"
                );
            }
            // In dev/test, log warning but allow empty key (encryption will fail gracefully)
            log.warn("SMTP_ENCRYPTION_KEY ist nicht gesetzt. SMTP-Verschlüsselung wird nicht funktionieren.");
        }
        
        // Initialize encryptor with key and salt
        // Encryptors.stronger() uses AES-256 and expects hex-encoded strings
        // Encryptors.standard() uses AES-128 (16-byte key), stronger() uses AES-256 (32-byte key)
        HexFormat hex = HexFormat.of();
        String hexSalt = hex.formatHex(ENCRYPTION_SALT.getBytes(StandardCharsets.UTF_8));
        
        // Prepare keys for both encryption methods (backward compatibility)
        byte[] keyBytes = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
        
        // For AES-256 (new method): use first 32 bytes
        byte[] key256 = new byte[32];
        if (keyBytes.length >= 32) {
            System.arraycopy(keyBytes, 0, key256, 0, 32);
        } else {
            System.arraycopy(keyBytes, 0, key256, 0, keyBytes.length);
        }
        String hexKey256 = hex.formatHex(key256);
        BytesEncryptor encryptor256 = Encryptors.stronger(hexKey256, hexSalt);
        
        // For AES-128 (old method): use first 16 bytes for backward compatibility
        byte[] key128 = new byte[16];
        if (keyBytes.length >= 16) {
            System.arraycopy(keyBytes, 0, key128, 0, 16);
        } else {
            System.arraycopy(keyBytes, 0, key128, 0, keyBytes.length);
        }
        String hexKey128 = hex.formatHex(key128);
        BytesEncryptor encryptor128 = Encryptors.standard(hexKey128, hexSalt);
        
        // Use AES-256 for new encryptions, but support both for decryption (backward compatibility)
        this.encryptor = new TextEncryptor() {
            @Override
            public String encrypt(String text) {
                // Always use AES-256 for new encryptions
                byte[] encrypted = encryptor256.encrypt(text.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(encrypted);
            }

            @Override
            public String decrypt(String encryptedText) {
                if (encryptedText == null || encryptedText.isEmpty()) {
                    return "";
                }
                
                try {
                    byte[] encrypted = Base64.getDecoder().decode(encryptedText);
                    // Try AES-256 first (new method)
                    try {
                        byte[] decrypted = encryptor256.decrypt(encrypted);
                        return new String(decrypted, StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        // If that fails, try AES-128 (old method) for backward compatibility
                        try {
                            log.debug("AES-256 decryption failed, trying AES-128 for backward compatibility");
                            byte[] decrypted = encryptor128.decrypt(encrypted);
                            return new String(decrypted, StandardCharsets.UTF_8);
                        } catch (Exception e2) {
                            // If both fail, the data might be encrypted with a different key
                            // This can happen if the encryption key changed or data is corrupted
                            log.warn("Failed to decrypt with both AES-256 and AES-128. " +
                                    "Data may be encrypted with a different key or corrupted. " +
                                    "Please re-enter the credentials.", e2);
                            // Return empty string instead of throwing - allows graceful handling
                            return "";
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Base64 decoding failed - data is not valid Base64
                    log.warn("Failed to decode Base64 encrypted data", e);
                    return "";
                }
            }
        };
    }

    /**
     * Gets the encryption key from environment variable.
     * The key must be exactly 32 bytes for AES-256 (used by Encryptors.stronger()).
     * 
     * @throws IllegalStateException if SMTP_ENCRYPTION_KEY is not set (required for cloud deployment)
     */
    private static String getEncryptionKey() {
        String envKey = System.getenv("SMTP_ENCRYPTION_KEY");
        if (envKey == null || envKey.isEmpty()) {
            // In cloud/production, the encryption key MUST be provided via environment variable
            // This prevents using a default key that could be compromised
            String activeProfile = System.getProperty("spring.profiles.active", 
                    System.getenv("SPRING_PROFILES_ACTIVE"));
            if (activeProfile != null && (activeProfile.contains("cloud") || 
                    activeProfile.contains("prod") || activeProfile.contains("production"))) {
                throw new IllegalStateException(
                    "SMTP_ENCRYPTION_KEY environment variable is required for cloud/production deployment. " +
                    "Please set a secure 32-byte encryption key via environment variable."
                );
            }
            // Only allow default key in dev/test environments
            log.warn("SMTP_ENCRYPTION_KEY not set. Using default key (ONLY for dev/test). " +
                    "Set SMTP_ENCRYPTION_KEY environment variable for production!");
            return "default-smtp-encryption-key-32!!"; // Exactly 32 characters
        }
        
        // Ensure it's exactly 32 bytes
        byte[] keyBytes = envKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length == 32) {
            return envKey;
        } else if (keyBytes.length < 32) {
            // Pad with zeros if too short
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return new String(padded, StandardCharsets.UTF_8);
        } else {
            // Truncate if too long
            return new String(keyBytes, 0, 32, StandardCharsets.UTF_8);
        }
    }

    /**
     * Gets the current SMTP configuration, or creates a default one if none exists.
     * If no DB configuration exists, loads default values from environment variables.
     */
    @Transactional(readOnly = true)
    public SmtpConfig getSmtpConfig() {
        return smtpConfigRepository.findFirstByOrderByIdAsc()
                .orElseGet(() -> {
                    log.info("No SMTP configuration found in database, creating default from environment variables");
                    SmtpConfig config = new SmtpConfig();
                    config.setEnabled(false);
                    
                    // Load default values from environment variables (only if not in DB)
                    String envHost = System.getenv("SMTP_HOST");
                    if (envHost != null && !envHost.isEmpty()) {
                        config.setHost(envHost);
                    }
                    
                    String envPort = System.getenv("SMTP_PORT");
                    if (envPort != null && !envPort.isEmpty()) {
                        try {
                            config.setPort(Integer.parseInt(envPort));
                        } catch (NumberFormatException e) {
                            log.debug("Invalid SMTP_PORT in environment: {}", envPort);
                        }
                    }
                    
                    String envUsername = System.getenv("SMTP_USERNAME");
                    if (envUsername != null && !envUsername.isEmpty()) {
                        config.setUsername(envUsername);
                    }
                    
                    // Note: Password is NEVER read from ENV for security reasons
                    
                    String envFromAddress = System.getenv("SMTP_FROM_ADDRESS");
                    if (envFromAddress != null && !envFromAddress.isEmpty()) {
                        config.setFromAddress(envFromAddress);
                    }
                    
                    String envSecurityMethod = System.getenv("SMTP_SECURITY_METHOD");
                    if (envSecurityMethod != null && !envSecurityMethod.isEmpty()) {
                        try {
                            config.setSecurityMethod(SmtpSecurityMethod.valueOf(envSecurityMethod.toUpperCase()));
                        } catch (IllegalArgumentException e) {
                            log.debug("Invalid SMTP_SECURITY_METHOD in environment: {}", envSecurityMethod);
                        }
                    }
                    
                    String envEnabled = System.getenv("SMTP_ENABLED");
                    if (envEnabled != null && !envEnabled.isEmpty()) {
                        config.setEnabled(Boolean.parseBoolean(envEnabled));
                    }
                    
                    return config;
                });
    }

    /**
     * Saves or updates the SMTP configuration.
     * The password is encrypted before storage.
     */
    @Transactional
    public SmtpConfig saveSmtpConfig(SmtpConfig config) {
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            // Encrypt password before saving
            String encryptedPassword = encryptor.encrypt(config.getPassword());
            config.setPassword(encryptedPassword);
        } else if (config.getId() != null) {
            // If password is empty and config exists, keep the old encrypted password
            SmtpConfig existing = smtpConfigRepository.findById(config.getId()).orElse(null);
            if (existing != null && existing.getPassword() != null) {
                config.setPassword(existing.getPassword());
            }
        }

        // Encrypt private key if provided
        if (config.getOpenpgpPrivateKey() != null && !config.getOpenpgpPrivateKey().isEmpty()) {
            String encryptedPrivateKey = encryptor.encrypt(config.getOpenpgpPrivateKey());
            config.setOpenpgpPrivateKey(encryptedPrivateKey);
        } else if (config.getId() != null) {
            // If private key is empty and config exists, keep the old encrypted key
            SmtpConfig existing = smtpConfigRepository.findById(config.getId()).orElse(null);
            if (existing != null && existing.getOpenpgpPrivateKey() != null) {
                config.setOpenpgpPrivateKey(existing.getOpenpgpPrivateKey());
            }
        }

        // Encrypt private key passphrase if provided
        if (config.getOpenpgpPrivateKeyPassphrase() != null 
                && !config.getOpenpgpPrivateKeyPassphrase().isEmpty()) {
            String encryptedPassphrase = encryptor.encrypt(config.getOpenpgpPrivateKeyPassphrase());
            config.setOpenpgpPrivateKeyPassphrase(encryptedPassphrase);
        } else if (config.getId() != null) {
            // If passphrase is empty and config exists, keep the old encrypted passphrase
            SmtpConfig existing = smtpConfigRepository.findById(config.getId()).orElse(null);
            if (existing != null && existing.getOpenpgpPrivateKeyPassphrase() != null) {
                config.setOpenpgpPrivateKeyPassphrase(existing.getOpenpgpPrivateKeyPassphrase());
            }
        }

        SmtpConfig saved = smtpConfigRepository.save(config);
        log.info("SMTP configuration saved (ID: {})", saved.getId());
        return saved;
    }

    /**
     * Gets the decrypted password for the SMTP configuration.
     * Returns empty string if no password is set.
     */
    @Transactional(readOnly = true)
    public String getDecryptedPassword() {
        SmtpConfig config = getSmtpConfig();
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            try {
                return encryptor.decrypt(config.getPassword());
            } catch (Exception e) {
                log.error("Failed to decrypt SMTP password", e);
                return "";
            }
        }
        return "";
    }

    /**
     * Gets the decrypted private key for the SMTP configuration.
     * Returns empty string if no private key is set.
     */
    @Transactional(readOnly = true)
    public String getDecryptedPrivateKey() {
        SmtpConfig config = getSmtpConfig();
        if (config.getOpenpgpPrivateKey() != null && !config.getOpenpgpPrivateKey().isEmpty()) {
            try {
                return encryptor.decrypt(config.getOpenpgpPrivateKey());
            } catch (Exception e) {
                log.error("Failed to decrypt OpenPGP private key", e);
                return "";
            }
        }
        return "";
    }

    /**
     * Gets the decrypted passphrase for the OpenPGP private key.
     * Returns empty string if no passphrase is set.
     */
    @Transactional(readOnly = true)
    public String getDecryptedPrivateKeyPassphrase() {
        SmtpConfig config = getSmtpConfig();
        if (config.getOpenpgpPrivateKeyPassphrase() != null 
                && !config.getOpenpgpPrivateKeyPassphrase().isEmpty()) {
            try {
                return encryptor.decrypt(config.getOpenpgpPrivateKeyPassphrase());
            } catch (Exception e) {
                log.error("Failed to decrypt OpenPGP private key passphrase", e);
                return "";
            }
        }
        return "";
    }

    /**
     * Tests the SMTP configuration by attempting to connect to the server.
     * This performs a real connection test, not just validation.
     */
    public boolean testConnection(SmtpConfig config) {
        // Basic validation first
        if (config.getHost() == null || config.getHost().isEmpty()) {
            return false;
        }
        if (config.getPort() == null || config.getPort() < 1 || config.getPort() > 65535) {
            return false;
        }
        
        // Try to establish a real connection
        try {
            org.springframework.mail.javamail.JavaMailSenderImpl testSender = 
                    new org.springframework.mail.javamail.JavaMailSenderImpl();
            
            testSender.setHost(config.getHost());
            testSender.setPort(config.getPort() != null ? config.getPort() : 587);
            
            if (config.getUsername() != null && !config.getUsername().isEmpty()) {
                testSender.setUsername(config.getUsername());
            }
            
            // Get decrypted password for testing
            String password = null;
            if (config.getPassword() != null && !config.getPassword().isEmpty()) {
                try {
                    password = encryptor.decrypt(config.getPassword());
                } catch (Exception e) {
                    log.debug("Could not decrypt password for connection test", e);
                    // If password is provided in config but can't be decrypted, try using it directly
                    // (might be a new password that hasn't been encrypted yet)
                    password = config.getPassword();
                }
            }
            
            if (password != null && !password.isEmpty()) {
                testSender.setPassword(password);
            }
            
            // Configure security method
            SmtpSecurityMethod securityMethod = config.getSecurityMethod();
            if (securityMethod == null) {
                securityMethod = (config.getUseTls() != null && config.getUseTls()) 
                        ? SmtpSecurityMethod.STARTTLS 
                        : SmtpSecurityMethod.NONE;
            }
            
            java.util.Properties props = testSender.getJavaMailProperties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            props.put("mail.smtp.writetimeout", "5000");
            
            switch (securityMethod) {
                case SSL_TLS:
                    props.put("mail.smtp.ssl.enable", "true");
                    props.put("mail.smtp.ssl.trust", config.getHost());
                    props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
                    props.put("mail.smtp.socketFactory.port", String.valueOf(config.getPort()));
                    props.put("mail.smtp.socketFactory.fallback", "false");
                    break;
                case STARTTLS:
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.starttls.required", "true");
                    break;
                case NONE:
                default:
                    // No encryption
                    break;
            }
            
            // Test connection by creating a session
            jakarta.mail.Session session = testSender.getSession();
            jakarta.mail.Transport transport = session.getTransport("smtp");
            
            try {
                // Attempt to connect
                int port = config.getPort() != null ? config.getPort() : 587;
                if (config.getUsername() != null && !config.getUsername().isEmpty() 
                        && password != null && !password.isEmpty()) {
                    transport.connect(config.getHost(), port, 
                            config.getUsername(), password);
                } else {
                    // Connect without authentication
                    transport.connect(config.getHost(), port, null, null);
                }
                
                // Connection successful
                transport.close();
                log.info("SMTP connection test successful for {}:{}", config.getHost(), config.getPort());
                return true;
            } catch (jakarta.mail.MessagingException e) {
                log.warn("SMTP connection test failed for {}:{} - {}", 
                        config.getHost(), config.getPort(), e.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("Error during SMTP connection test", e);
            return false;
        }
    }
}

