package de.bbajor.pvs.security.email.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.institution.model.EmailEncryptionMethod;
import de.bbajor.pvs.institution.model.InstitutionEmailContact;
import de.bbajor.pvs.institution.repository.InstitutionEmailContactRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

/**
 * Service for sending encrypted emails using OpenPGP.
 * Extends EmailService functionality with encryption support.
 */
@Service
public class EncryptedEmailService {

    private static final Logger log = LoggerFactory.getLogger(EncryptedEmailService.class);

    private final JavaMailSender mailSender;
    private final OpenPgpService openPgpService;
    private final SmimeService smimeService;
    private final InstitutionEmailContactRepository emailContactRepository;
    private final OpenPgpKeyServerService keyServerService;
    private final SmtpConfigService smtpConfigService;

    public EncryptedEmailService(
            JavaMailSender mailSender,
            OpenPgpService openPgpService,
            SmimeService smimeService,
            InstitutionEmailContactRepository emailContactRepository,
            OpenPgpKeyServerService keyServerService,
            SmtpConfigService smtpConfigService) {
        this.mailSender = mailSender;
        this.openPgpService = openPgpService;
        this.smimeService = smimeService;
        this.emailContactRepository = emailContactRepository;
        this.keyServerService = keyServerService;
        this.smtpConfigService = smtpConfigService;
    }

    /**
     * Sends an email, encrypting it with OpenPGP if a public key is available for the recipient.
     * 
     * @param toEmail the recipient email address
     * @param subject the email subject
     * @param plainText the plain text message
     * @param fromAddress the sender email address
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendEmail(String toEmail, String subject, String plainText, String fromAddress) {
        return sendEmail(toEmail, subject, plainText, fromAddress, null);
    }

    /**
     * Sends an email, encrypting it with OpenPGP if a public key is available for the recipient.
     * Allows providing a temporary OpenPGP key that is not yet saved in the database.
     * 
     * @param toEmail the recipient email address
     * @param subject the email subject
     * @param plainText the plain text message
     * @param fromAddress the sender email address
     * @param temporaryPgpKey optional temporary OpenPGP public key (e.g., from UI before saving)
     * @return true if email was sent successfully, false otherwise
     */
    public boolean sendEmail(String toEmail, String subject, String plainText, String fromAddress, String temporaryPgpKey) {
        try {
            // Check contact configuration for this email
            InstitutionEmailContact contact = emailContactRepository.findByEmail(toEmail)
                    .orElse(null);
            
            // Determine encryption method
            // First check contact-specific configuration, then fall back to global default
            EmailEncryptionMethod encryptionMethod = null;
            if (contact != null && contact.getActive() != null && contact.getActive()) {
                encryptionMethod = contact.getEncryptionMethod();
                if (encryptionMethod == null) {
                    // Migration: default to OPENPGP if key exists, otherwise use global default
                    encryptionMethod = (contact.getOpenpgpPublicKey() != null && !contact.getOpenpgpPublicKey().isEmpty())
                            ? EmailEncryptionMethod.OPENPGP
                            : null; // Will use global default below
                }
            }
            
            // If no contact-specific method, use global default from SMTP config
            if (encryptionMethod == null) {
                var smtpConfig = smtpConfigService.getSmtpConfig();
                encryptionMethod = smtpConfig.getDefaultEncryptionMethod();
                if (encryptionMethod == null) {
                    encryptionMethod = EmailEncryptionMethod.NONE; // Final fallback
                }
                log.debug("Using global default encryption method: {} for {}", encryptionMethod, toEmail);
            }
            
            // If temporary PGP key is provided, use OPENPGP
            String pgpKeyToUse = null;
            if (temporaryPgpKey != null && !temporaryPgpKey.trim().isEmpty()) {
                encryptionMethod = EmailEncryptionMethod.OPENPGP;
                pgpKeyToUse = temporaryPgpKey.trim();
                log.debug("Using temporary OpenPGP key for {}", toEmail);
            } else if (encryptionMethod == EmailEncryptionMethod.OPENPGP) {
                if (contact != null && contact.getOpenpgpPublicKey() != null) {
                    pgpKeyToUse = contact.getOpenpgpPublicKey();
                    log.debug("Using OpenPGP key from database for {}", toEmail);
                } else {
                    // Try to fetch key from keys.openpgp.org
                    log.debug("No OpenPGP key in database for {}, trying keys.openpgp.org", toEmail);
                    Optional<String> keyFromServer = keyServerService.lookupKey(toEmail);
                    if (keyFromServer.isPresent()) {
                        pgpKeyToUse = keyFromServer.get();
                        log.info("Retrieved OpenPGP key from keys.openpgp.org for {}", toEmail);
                    } else {
                        log.debug("No OpenPGP key found on keys.openpgp.org for {}, falling back to unencrypted", toEmail);
                        encryptionMethod = EmailEncryptionMethod.NONE;
                    }
                }
            }

            MimeMessage message = mailSender.createMimeMessage();
            
            // Warn if from address differs from SMTP username (may cause issues with some SMTP servers)
            // This is checked dynamically to allow runtime configuration changes
            var smtpConfig = smtpConfigService.getSmtpConfig();
            String smtpUsername = smtpConfig.getUsername();
            if (smtpUsername != null && !smtpUsername.isEmpty() 
                    && !smtpUsername.equalsIgnoreCase(fromAddress)) {
                log.warn("SMTP username ({}) differs from sender address ({}). " +
                        "Some SMTP servers may reject this. Consider using the same address or configuring " +
                        "the SMTP server to allow sending from different addresses.",
                        smtpUsername, fromAddress);
            }
            
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject, "UTF-8");

            // Handle encryption based on method
            if (encryptionMethod == EmailEncryptionMethod.OPENPGP && pgpKeyToUse != null && !pgpKeyToUse.isEmpty()) {
                // Encrypt the message with OpenPGP using PGP/MIME format (RFC 3156)
                try {
                    // Extract and log key ID for debugging
                    String extractedKeyId = null;
                    try {
                        extractedKeyId = openPgpService.extractKeyId(pgpKeyToUse);
                        log.info("Encrypting email to {} using OpenPGP key ID: {}", toEmail, extractedKeyId);
                    } catch (Exception e) {
                        log.debug("Could not extract key ID", e);
                    }
                    
                    // Check if we have a private key for signing
                    // Note: Private keys CANNOT be retrieved from keys.openpgp.org for security reasons.
                    // keys.openpgp.org only provides public keys. The private key must be stored locally
                    // (encrypted in the database) to create digital signatures.
                    String privateKey = smtpConfig.getOpenpgpPrivateKey();
                    String privateKeyPassphrase = null;
                    boolean isSigned = false;
                    String decryptedPrivateKey = null; // Declare outside if-block for Autocrypt header
                    
                    if (privateKey != null && !privateKey.isEmpty()) {
                        // Decrypt the private key passphrase if available
                        if (smtpConfig.getOpenpgpPrivateKeyPassphrase() != null 
                                && !smtpConfig.getOpenpgpPrivateKeyPassphrase().isEmpty()) {
                            try {
                                privateKeyPassphrase = smtpConfigService.getDecryptedPrivateKeyPassphrase();
                            } catch (Exception e) {
                                log.warn("Failed to decrypt private key passphrase, trying without passphrase", e);
                            }
                        }
                        
                        // Decrypt the private key itself
                        try {
                            decryptedPrivateKey = smtpConfigService.getDecryptedPrivateKey();
                            if (decryptedPrivateKey == null || decryptedPrivateKey.isEmpty()) {
                                log.warn("Private key decryption returned empty result for signing");
                            } else {
                                log.debug("Successfully decrypted private key for signing (length: {} chars)", decryptedPrivateKey.length());
                            }
                        } catch (Exception e) {
                            log.error("Failed to decrypt private key for signing", e);
                        }
                        
                        if (decryptedPrivateKey != null && !decryptedPrivateKey.isEmpty()) {
                            // Optional: Validate that the private key matches a public key from keys.openpgp.org
                            // This is just a validation step - the signature still requires the private key
                            try {
                                Optional<String> senderPublicKey = keyServerService.lookupKey(fromAddress);
                                if (senderPublicKey.isPresent()) {
                                    // Could validate key ID or fingerprint match here
                                    log.debug("Found public key for sender {} on keys.openpgp.org - validating match", fromAddress);
                                }
                            } catch (Exception e) {
                                log.debug("Could not validate sender public key from keys.openpgp.org", e);
                            }
                            
                              // Encrypt and sign the message with pgpainless
                              try {
                                  log.debug("Attempting to sign and encrypt message for {} using pgpainless", toEmail);
                                  String encryptedAndSigned = openPgpService.encryptAndSignMessage(
                                          plainText, pgpKeyToUse, decryptedPrivateKey, privateKeyPassphrase);

                                  if (encryptedAndSigned == null || encryptedAndSigned.isEmpty()) {
                                      log.error("encryptAndSignMessage returned empty result");
                                      throw new RuntimeException("Signing failed: empty result");
                                  }

                                  log.info("Email encrypted and signed for {} using pgpainless (encrypted message length: {} chars)",
                                          toEmail, encryptedAndSigned.length());
                                  isSigned = true;
                                  // Use the encrypted and signed message
                                  pgpKeyToUse = null; // Mark that we already have encrypted+signed message
                                  plainText = encryptedAndSigned; // This will be used as the encrypted content
                              } catch (Exception e) {
                                // Log as debug/warn instead of error - signing is optional
                                // Common reasons: no signing key in ring, key not suitable for signing
                                if (e.getMessage() != null && e.getMessage().contains("No signing key")) {
                                    log.debug("No signing key found in private key ring, sending encrypted only. " +
                                            "This is normal if the private key doesn't contain a signing-capable key.");
                                } else {
                                    log.warn("Failed to sign email, sending encrypted only. Error: {}", e.getMessage());
                                }
                                // Fallback to encryption only
                                isSigned = false;
                            }
                        } else {
                            log.warn("Private key is null or empty, cannot sign email");
                        }
                    } else {
                        // No private key configured - try to get public key from keys.openpgp.org for info
                        // (This doesn't help with signing, but could be used for validation)
                        try {
                            Optional<String> senderPublicKey = keyServerService.lookupKey(fromAddress);
                            if (senderPublicKey.isPresent()) {
                                log.debug("Public key found for sender {} on keys.openpgp.org, but no private key configured for signing", fromAddress);
                            }
                        } catch (Exception e) {
                            // Ignore - this is just informational
                        }
                    }
                    
                    // If not signed, just encrypt
                    String encryptedMessage;
                    if (pgpKeyToUse != null && !pgpKeyToUse.isEmpty()) {
                        encryptedMessage = openPgpService.encryptMessage(plainText, pgpKeyToUse);
                    } else {
                        // Already encrypted and signed
                        encryptedMessage = plainText;
                    }
                    
                    // Generate boundary in Thunderbird-compatible format
                    // Thunderbird uses: 14 dashes + alphanumeric string (e.g., "------------HuMw0yk9TdZYDq060bbphnvF")
                    // This format is critical for Thunderbird/Enigmail to recognize the encrypted message
                    String boundary = generateThunderbirdCompatibleBoundary();
                    
                    // Create PGP/MIME multipart structure (RFC 3156)
                    MimeMultipart multipart = new MimeMultipart("encrypted");
                    multipart.setPreamble("This is an OpenPGP/MIME encrypted message (RFC 4880 and 3156)");
                    
                    // First part: version information
                    MimeBodyPart versionPart = new MimeBodyPart();
                    versionPart.setText("Version: 1");
                    versionPart.setHeader("Content-Type", "application/pgp-encrypted");
                    versionPart.setHeader("Content-Description", "PGP/MIME version identification");
                    versionPart.setHeader("Content-Transfer-Encoding", "7bit");
                    multipart.addBodyPart(versionPart);
                    
                    // Second part: encrypted data (ASCII-armored)
                    // Match Gmail format exactly: no blank lines, proper formatting
                    // The encrypted message is already normalized in OpenPgpService
                    // (no trailing newline, no blank lines after BEGIN/before END)
                    MimeBodyPart encryptedPart = new MimeBodyPart();
                    encryptedPart.setText(encryptedMessage, StandardCharsets.UTF_8.name());
                    encryptedPart.setHeader("Content-Type", "application/octet-stream; name=\"encrypted.asc\"");
                    encryptedPart.setHeader("Content-Description", "OpenPGP encrypted message");
                    encryptedPart.setHeader("Content-Disposition", "inline; filename=\"encrypted.asc\"");
                    encryptedPart.setHeader("Content-Transfer-Encoding", "7bit");
                    multipart.addBodyPart(encryptedPart);
                    
                    // Set content first to let MimeMultipart generate its boundary
                    message.setContent(multipart);
                    message.saveChanges();
                    
                    // Extract the auto-generated boundary and replace with Thunderbird-compatible format
                    String rawMessage = getRawMessageContent(message);
                    if (rawMessage != null) {
                        String autoBoundary = extractAutoGeneratedBoundary(rawMessage);
                        if (autoBoundary != null && !autoBoundary.equals(boundary)) {
                            // Replace boundary in Content-Type header
                            String contentType = message.getContentType();
                            if (contentType != null && contentType.contains(autoBoundary)) {
                                contentType = contentType.replace(autoBoundary, boundary);
                                message.setHeader("Content-Type", contentType);
                            }
                            
                            // Replace boundary markers in body (--boundary becomes --newBoundary)
                            // Need to replace all occurrences: --boundary, --boundary--, etc.
                            final String modifiedRawMessage = rawMessage.replace("--" + autoBoundary, "--" + boundary);
                            final String finalBoundary = boundary;
                            
                            // Reconstruct the message by parsing the modified raw content
                            // We'll use a DataHandler to set the content from the modified string
                            message.setDataHandler(new jakarta.activation.DataHandler(
                                    new jakarta.activation.DataSource() {
                                        @Override
                                        public java.io.InputStream getInputStream() {
                                            return new java.io.ByteArrayInputStream(
                                                    modifiedRawMessage.getBytes(StandardCharsets.UTF_8));
                                        }
                                        
                                        @Override
                                        public java.io.OutputStream getOutputStream() {
                                            throw new UnsupportedOperationException();
                                        }
                                        
                                        @Override
                                        public String getContentType() {
                                            return "multipart/encrypted; protocol=\"application/pgp-encrypted\"; boundary=\"" + finalBoundary + "\"";
                                        }
                                        
                                        @Override
                                        public String getName() {
                                            return "message";
                                        }
                                    }));
                            message.saveChanges();
                        }
                    }
                    
                    // Ensure Content-Type has the correct boundary format
                    message.setHeader("Content-Type", 
                            "multipart/encrypted; protocol=\"application/pgp-encrypted\"; boundary=\"" + 
                            boundary + "\"");
                    message.saveChanges();
                    
                    log.debug("Set Content-Type with Thunderbird-compatible boundary: {}", boundary);
                    
                    // Add Autocrypt header if message is signed (helps Thunderbird recognize the signature)
                    // Note: Some Thunderbird versions may have issues with Autocrypt header
                    // Set header AFTER saveChanges() to ensure it's properly added to the final message
                    // Can be disabled via system property: -Demail.autocrypt.enabled=false
                    boolean autocryptEnabled = !"false".equalsIgnoreCase(
                            System.getProperty("email.autocrypt.enabled", "true"));
                    if (isSigned && decryptedPrivateKey != null && !decryptedPrivateKey.isEmpty() && autocryptEnabled) {
                        try {
                            // Use stored public key if available, otherwise extract from private key
                            String publicKeyArmored = null;
                            String storedPublicKey = smtpConfig.getOpenpgpPublicKey();
                            if (storedPublicKey != null && !storedPublicKey.trim().isEmpty()) {
                                // Use stored public key (better performance and Thunderbird compatibility)
                                publicKeyArmored = storedPublicKey;
                                log.debug("Using stored public key for Autocrypt header");
                            } else {
                                // Extract public key from private key at runtime
                                publicKeyArmored = openPgpService.extractPublicKeyFromPrivateKey(decryptedPrivateKey);
                                log.debug("Extracted public key from private key for Autocrypt header");
                            }
                            
                            // Convert to base64 (remove armor headers/footers and whitespace)
                            String publicKeyContent = publicKeyArmored
                                    .replace("-----BEGIN PGP PUBLIC KEY BLOCK-----", "")
                                    .replace("-----END PGP PUBLIC KEY BLOCK-----", "")
                                    .replaceAll("\\s+", ""); // Remove all whitespace
                            
                            // Create Autocrypt header (RFC 8315)
                            // Format: addr=email; keydata=base64-key
                            // Note: Thunderbird 144+ should support this, but some versions may have issues
                            String autocryptHeader = String.format("addr=%s; keydata=%s", fromAddress, publicKeyContent);
                            message.setHeader("Autocrypt", autocryptHeader);
                            // Save again after adding Autocrypt header
                            message.saveChanges();
                            log.info("Added Autocrypt header for signed email to {} (key length: {} chars). " +
                                    "If signature is not recognized, try removing this header.", 
                                    toEmail, publicKeyContent.length());
                        } catch (Exception e) {
                            log.warn("Failed to add Autocrypt header (email will still be signed): {}", e.getMessage(), e);
                        }
                    }
                    
                    // Log final key ID for debugging
                    String finalKeyId = (contact != null && contact.getKeyId() != null) 
                            ? contact.getKeyId() 
                            : (extractedKeyId != null ? extractedKeyId : "temporary");
                    
                    if (isSigned) {
                        log.info("Sending PGP/MIME encrypted and signed email to {} using OpenPGP key ID: {}", toEmail, finalKeyId);
                    } else {
                        log.info("Sending PGP/MIME encrypted email to {} using OpenPGP key ID: {} (not signed - no private key configured)", toEmail, finalKeyId);
                        log.debug("Email encrypted but not signed. To enable signing, configure a private key in SMTP settings.");
                    }
                } catch (Exception e) {
                    log.error("Failed to encrypt email for {}: {}", toEmail, e.getMessage(), e);
                    // Fallback to plain text if encryption fails
                    MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                    helper.setText(plainText, false);
                    log.warn("Sending unencrypted email to {} due to encryption failure", toEmail);
                }
            } else if (encryptionMethod == EmailEncryptionMethod.SMIME) {
                // Encrypt with S/MIME
                try {
                    String smimeCertificate = contact != null ? contact.getSmimeCertificate() : null;
                    if (smimeCertificate == null || smimeCertificate.trim().isEmpty()) {
                        log.warn("S/MIME certificate not found for {}, sending unencrypted email", toEmail);
                        MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                        helper.setText(plainText, false);
                    } else {
                        smimeService.encryptMessage(message, plainText, smimeCertificate);
                        log.info("Sending S/MIME encrypted email to {}", toEmail);
                    }
                } catch (Exception e) {
                    log.error("Failed to encrypt email with S/MIME for {}: {}", toEmail, e.getMessage(), e);
                    // Fallback to plain text if encryption fails
                    MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                    helper.setText(plainText, false);
                    log.warn("Sending unencrypted email to {} due to S/MIME encryption failure", toEmail);
                }
            } else {
                // Send plain text email (NONE or no encryption configured)
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                helper.setText(plainText, false);
                if (encryptionMethod == EmailEncryptionMethod.NONE) {
                    log.warn("⚠️ Sending unencrypted email to {} - no encryption method configured", toEmail);
                } else if (contact == null) {
                    log.debug("No contact configuration found for {}, sending unencrypted email", toEmail);
                } else if (contact.getActive() == null || !contact.getActive()) {
                    log.debug("Contact for {} is inactive, sending unencrypted email", toEmail);
                }
            }

            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
            return true;
        } catch (MailException | MessagingException e) {
            log.error("Failed to send email to {}", toEmail, e);
            return false;
        }
    }

    /**
     * Checks if an email address has an active OpenPGP key configured.
     */
    public boolean hasOpenPgpKey(String email) {
        return emailContactRepository.findByEmail(email)
                .map(contact -> contact.getOpenpgpPublicKey() != null 
                        && contact.getActive() != null 
                        && contact.getActive())
                .orElse(false);
    }

    /**
     * Generates a Thunderbird-compatible boundary.
     * Thunderbird uses: 14 dashes + alphanumeric string (e.g., "------------HuMw0yk9TdZYDq060bbphnvF")
     * 
     * @return A boundary string in Thunderbird format
     */
    private String generateThunderbirdCompatibleBoundary() {
        // Generate a random alphanumeric string (similar to Thunderbird)
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 28; i++) { // 28 characters like Thunderbird
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        // Thunderbird format: 14 dashes + alphanumeric string
        return "------------" + sb.toString();
    }
    
    /**
     * Gets the raw message content as a string for boundary replacement.
     */
    private String getRawMessageContent(MimeMessage message) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            message.writeTo(baos);
            try {
                return baos.toString("UTF-8");
            } catch (UnsupportedEncodingException e) {
                return baos.toString();
            }
        } catch (Exception e) {
            log.debug("Error getting raw message content", e);
            return null;
        }
    }
    
    /**
     * Extracts the auto-generated boundary from the raw message.
     */
    private String extractAutoGeneratedBoundary(String rawMessage) {
        // Look for boundary pattern in Content-Type header
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "boundary=\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(rawMessage);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    

    /**
     * Extracts the boundary from a message by parsing the raw message content.
     * The boundary appears in the message body as "------=boundary" markers.
     * @deprecated Use generateThunderbirdCompatibleBoundary() instead
     */
    @Deprecated
    private String extractBoundaryFromMessage(MimeMessage message) {
        try {
            // Try to get boundary from Content-Type first
            String contentType = message.getContentType();
            if (contentType != null) {
                // Try boundary="..." first
                int boundaryIndex = contentType.indexOf("boundary=\"");
                if (boundaryIndex >= 0) {
                    int start = boundaryIndex + 10; // length of "boundary=\""
                    int end = contentType.indexOf("\"", start);
                    if (end > start) {
                        return contentType.substring(start, end);
                    }
                }
                // Try boundary=... (without quotes)
                boundaryIndex = contentType.indexOf("boundary=");
                if (boundaryIndex >= 0) {
                    int start = boundaryIndex + 9; // length of "boundary="
                    int end = contentType.indexOf(";", start);
                    if (end < 0) {
                        end = contentType.length();
                    }
                    String boundary = contentType.substring(start, end).trim();
                    // Remove quotes if present
                    if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
                        boundary = boundary.substring(1, boundary.length() - 1);
                    }
                    return boundary;
                }
            }
            
            // If not in Content-Type, try to extract from message body
            // The boundary appears as "------=boundary" in multipart messages
            Object content = message.getContent();
            if (content instanceof MimeMultipart) {
                // Try to get boundary from the multipart's internal structure
                // Unfortunately, MimeMultipart doesn't expose getBoundary()
                // So we need to parse it from the raw message
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                message.writeTo(baos);
                String rawMessage;
                try {
                    rawMessage = baos.toString("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    rawMessage = baos.toString();
                }
                
                // Look for boundary pattern: "------=boundary" or "--boundary"
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                        "(?:^|\\r?\\n)--([^\\r\\n]+)(?:\\r?\\n|$)");
                java.util.regex.Matcher matcher = pattern.matcher(rawMessage);
                if (matcher.find()) {
                    String foundBoundary = matcher.group(1);
                    // Remove leading/trailing whitespace and quotes
                    foundBoundary = foundBoundary.trim();
                    if (foundBoundary.startsWith("=")) {
                        foundBoundary = foundBoundary.substring(1);
                    }
                    if (foundBoundary.startsWith("\"") && foundBoundary.endsWith("\"")) {
                        foundBoundary = foundBoundary.substring(1, foundBoundary.length() - 1);
                    }
                    return foundBoundary;
                }
            }
        } catch (MessagingException | IOException e) {
            log.debug("Error extracting boundary from message", e);
        }
        return null;
    }
}

