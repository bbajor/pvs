package de.bbajor.pvs.security.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service for sending emails, particularly for MFA setup and password reset.
 * 
 * <p>
 * This service provides methods for sending various types of emails:
 * <ul>
 * <li>MFA setup confirmation</li>
 * <li>Password reset notifications</li>
 * </ul>
 * </p>
 * 
 * <p>
 * Email configuration is done via Spring Mail properties (SMTP_HOST, SMTP_PORT, etc.).
 * If SMTP is not configured, email sending will fail gracefully.
 * </p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean enabled;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username:}") String fromAddress,
            @Value("${spring.mail.host:}") String smtpHost) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = smtpHost != null && !smtpHost.isEmpty();
        
        if (!enabled) {
            log.warn("Email service is disabled - SMTP_HOST is not configured");
        }
    }

    /**
     * Sends an email notification when MFA is set up for a user.
     * 
     * @param toEmail the recipient email address
     * @param username the username
     */
    public void sendMfaSetupEmail(String toEmail, String username) {
        if (!enabled) {
            log.debug("Email service disabled - skipping MFA setup email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
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
     * Sends a password reset email (optional feature for future use).
     * 
     * @param toEmail the recipient email address
     * @param username the username
     * @param resetToken the password reset token (if implemented)
     */
    public void sendPasswordResetEmail(String toEmail, String username, String resetToken) {
        if (!enabled) {
            log.debug("Email service disabled - skipping password reset email to {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Passwort zurücksetzen");
            message.setText(String.format(
                    "Hallo %s,\n\n" +
                    "Sie haben eine Passwort-Zurücksetzung angefordert.\n\n" +
                    "Token: %s\n\n" +
                    "Bitte verwenden Sie diesen Token, um Ihr Passwort zurückzusetzen.\n\n" +
                    "Falls Sie diese Anfrage nicht gestellt haben, ignorieren Sie diese E-Mail.\n\n" +
                    "Mit freundlichen Grüßen,\n" +
                    "PVS System",
                    username, resetToken));

            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
            // Don't throw - email failure shouldn't break the flow
        }
    }
}
