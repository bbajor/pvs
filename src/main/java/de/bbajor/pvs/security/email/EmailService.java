package de.bbajor.pvs.security.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.security.email.service.EncryptedEmailService;
import de.bbajor.pvs.security.email.service.SmtpConfigService;

/**
 * Service for sending emails, particularly for MFA setup and password reset.
 * 
 * <p>
 * This service provides methods for sending various types of emails:
 * <ul>
 * <li>MFA setup confirmation</li>
 * <li>Password reset notifications (with OpenPGP encryption if configured)</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Email configuration can be done via:
 * <ul>
 * <li>Database configuration (preferred) - via SmtpConfigService</li>
 * <li>Spring Mail properties (SMTP_HOST, SMTP_PORT, etc.) - fallback</li>
 * </ul>
 * </p>
 * 
 * <p>
 * If OpenPGP keys are configured for recipient email addresses, emails will be automatically encrypted.
 * </p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EncryptedEmailService encryptedEmailService;
    private final SmtpConfigService smtpConfigService;
    private final String fallbackFromAddress;
    private final String fallbackSmtpHost;

    public EmailService(
            JavaMailSender mailSender,
            EncryptedEmailService encryptedEmailService,
            SmtpConfigService smtpConfigService,
            @Value("${spring.mail.username:}") String fallbackFromAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender = mailSender;
        this.encryptedEmailService = encryptedEmailService;
        this.smtpConfigService = smtpConfigService;
        this.fallbackFromAddress = fallbackFromAddress;
        this.fallbackSmtpHost = smtpHost;
    }

    /**
     * Checks if email service is enabled by reading current configuration from database.
     * This is checked dynamically to allow runtime configuration changes.
     */
    private boolean isEnabled() {
        var config = smtpConfigService.getSmtpConfig();
        boolean dbEnabled = config.getEnabled() != null && config.getEnabled();
        return dbEnabled || (fallbackSmtpHost != null && !fallbackSmtpHost.isEmpty());
    }

    /**
     * Gets the from address from database config or fallback to environment variable.
     * This is checked dynamically to allow runtime configuration changes.
     */
    private String getFromAddress() {
        var config = smtpConfigService.getSmtpConfig();
        String dbFromAddress = config.getFromAddress();
        return dbFromAddress != null && !dbFromAddress.isEmpty() 
                ? dbFromAddress 
                : fallbackFromAddress;
    }

    /**
     * Sends an email notification when MFA is set up for a user.
     * 
     * @param toEmail the recipient email address
     * @param username the username
     */
    public void sendMfaSetupEmail(String toEmail, String username) {
        if (!isEnabled()) {
            log.debug("Email service disabled - skipping MFA setup email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(getFromAddress());
            message.setTo(toEmail);
            message.setSubject("MFA erfolgreich eingerichtet");
            message.setText(String.format(
                    "Hallo %s,\n\n" +
                    "Multi-Faktor-Authentifizierung wurde erfolgreich für Ihr Konto eingerichtet.\n\n" +
                    "Bei zukünftigen Logins müssen Sie zusätzlich zum Passwort einen Code aus Ihrer Authenticator-App eingeben.\n\n" +
                    "Falls Sie diese Einrichtung nicht vorgenommen haben, kontaktieren Sie bitte sofort den Administrator.\n\n" +
                    "Mit freundlichen Grüßen,\n" +
                    "PVS System",
                    username));

            mailSender.send(message);
            log.info("MFA setup email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send MFA setup email to {}", toEmail, e);
            // Don't throw - email failure shouldn't break the flow
        }
    }

    /**
     * Sends a password reset email with automatic OpenPGP encryption if configured.
     * 
     * @param toEmail the recipient email address
     * @param username the username
     * @param resetToken the password reset token (if implemented)
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        if (!isEnabled()) {
            log.debug("Email service disabled - skipping password reset email to {}", toEmail);
            return;
        }

        String messageText = String.format(
                "Hallo %s,\n\n" +
                "Sie haben eine Passwort-Zurücksetzung angefordert.\n\n" +
                "Token: %s\n\n" +
                "Bitte verwenden Sie diesen Token, um Ihr Passwort zurückzusetzen.\n\n" +
                "Falls Sie diese Anfrage nicht gestellt haben, ignorieren Sie diese E-Mail.\n\n" +
                "Mit freundlichen Grüßen,\n" +
                "PVS System",
                username, resetToken);

        // Use encrypted email service which automatically encrypts if OpenPGP key is available
        boolean sent = encryptedEmailService.sendEmail(toEmail, "Passwort zurücksetzen", messageText, getFromAddress());
        
        if (sent) {
            log.info("Password reset email sent to {} (encrypted: {})", 
                    toEmail, encryptedEmailService.hasOpenPgpKey(toEmail));
        } else {
            log.error("Failed to send password reset email to {}", toEmail);
        }
    }

    /**
     * Sends a general email with automatic OpenPGP encryption if configured.
     * 
     * @param toEmail the recipient email address
     * @param subject the email subject
     * @param messageText the email body text
     */
    public void sendEmail(String toEmail, String subject, String messageText) {
        if (!isEnabled()) {
            log.debug("Email service disabled - skipping email to {}", toEmail);
            return;
        }

        // Use encrypted email service which automatically encrypts if OpenPGP key is available
        boolean sent = encryptedEmailService.sendEmail(toEmail, subject, messageText, getFromAddress());
        
        if (sent) {
            log.info("Email sent to {} (encrypted: {})", 
                    toEmail, encryptedEmailService.hasOpenPgpKey(toEmail));
        } else {
            log.error("Failed to send email to {}", toEmail);
        }
    }
}
