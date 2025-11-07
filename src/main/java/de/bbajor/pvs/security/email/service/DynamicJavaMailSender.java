package de.bbajor.pvs.security.email.service;

import de.bbajor.pvs.security.email.model.SmtpConfig;
import de.bbajor.pvs.security.email.model.SmtpSecurityMethod;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Dynamic JavaMailSender that reads SMTP configuration from database at runtime.
 * This allows changing SMTP settings without restarting the application.
 */
@Component
@Primary
public class DynamicJavaMailSender implements JavaMailSender {

    private static final Logger log = LoggerFactory.getLogger(DynamicJavaMailSender.class);

    private final SmtpConfigService smtpConfigService;

    public DynamicJavaMailSender(SmtpConfigService smtpConfigService) {
        this.smtpConfigService = smtpConfigService;
    }

    /**
     * Creates a configured JavaMailSenderImpl based on current database configuration.
     */
    private JavaMailSenderImpl createConfiguredSender() {
        SmtpConfig config = smtpConfigService.getSmtpConfig();
        JavaMailSenderImpl sender = new JavaMailSenderImpl();

        // Check if enabled
        boolean enabled = config.getEnabled() != null && config.getEnabled();
        if (!enabled || config.getHost() == null || config.getHost().isEmpty()) {
            log.debug("SMTP not enabled or not configured - returning dummy sender");
            return createDummySender();
        }

        // Basic configuration
        sender.setHost(config.getHost());
        sender.setPort(config.getPort() != null ? config.getPort() : 587);
        
        if (config.getUsername() != null && !config.getUsername().isEmpty()) {
            sender.setUsername(config.getUsername());
        }
        
        if (config.getPassword() != null && !config.getPassword().isEmpty()) {
            String decryptedPassword = smtpConfigService.getDecryptedPassword();
            sender.setPassword(decryptedPassword);
        }

        // Configure security method
        SmtpSecurityMethod securityMethod = config.getSecurityMethod();
        if (securityMethod == null) {
            // Fallback to useTls for backward compatibility
            securityMethod = (config.getUseTls() != null && config.getUseTls()) 
                    ? SmtpSecurityMethod.STARTTLS 
                    : SmtpSecurityMethod.NONE;
        }

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
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

        log.debug("Configured JavaMailSender with host: {}, port: {}, security: {}", 
                config.getHost(), config.getPort(), securityMethod);

        return sender;
    }

    /**
     * Creates a dummy sender that logs emails instead of sending them.
     */
    private JavaMailSenderImpl createDummySender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage simpleMessage) {
                log.info("DUMMY MAIL SENDER - Would send email to: {}, Subject: {}, From: {}",
                        simpleMessage.getTo() != null ? String.join(", ", simpleMessage.getTo()) : "N/A",
                        simpleMessage.getSubject(),
                        simpleMessage.getFrom());
                log.debug("Email body: {}", simpleMessage.getText());
            }

            @Override
            public void send(MimeMessage mimeMessage) {
                try {
                    log.info("DUMMY MAIL SENDER - Would send MIME message to: {}, Subject: {}",
                            mimeMessage.getAllRecipients() != null 
                                    ? String.join(", ", mimeMessage.getAllRecipients().toString()) 
                                    : "N/A",
                            mimeMessage.getSubject());
                } catch (MessagingException e) {
                    log.debug("Error reading dummy message", e);
                }
            }
        };
        sender.setHost("localhost");
        sender.setPort(25);
        return sender;
    }

    @Override
    public MimeMessage createMimeMessage() {
        return createConfiguredSender().createMimeMessage();
    }

    @Override
    public MimeMessage createMimeMessage(java.io.InputStream contentStream) throws MailException {
        return createConfiguredSender().createMimeMessage(contentStream);
    }

    @Override
    public void send(MimeMessage mimeMessage) throws MailException {
        createConfiguredSender().send(mimeMessage);
    }

    @Override
    public void send(MimeMessage... mimeMessages) throws MailException {
        createConfiguredSender().send(mimeMessages);
    }

    @Override
    public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
        createConfiguredSender().send(mimeMessagePreparator);
    }

    @Override
    public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
        createConfiguredSender().send(mimeMessagePreparators);
    }

    @Override
    public void send(SimpleMailMessage simpleMessage) throws MailException {
        createConfiguredSender().send(simpleMessage);
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) throws MailException {
        createConfiguredSender().send(simpleMessages);
    }
}

