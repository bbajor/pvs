package de.bbajor.pvs.security.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/**
 * Mail configuration that provides a dummy JavaMailSender for development
 * when no SMTP configuration is available.
 * 
 * <p>
 * This configuration ensures that EmailService can be instantiated even
 * without SMTP configuration. The dummy sender will log emails instead of
 * actually sending them.
 * </p>
 * 
 * <p>
 * Note: DynamicJavaMailSender is used as the primary JavaMailSender implementation
 * which reads configuration from the database at runtime.
 * </p>
 */
@Configuration
@ConditionalOnMissingBean(name = "dynamicJavaMailSender")
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    public JavaMailSender javaMailSender() {
        log.warn("No SMTP configuration found - creating dummy JavaMailSender for development. " +
                "Emails will be logged but not sent.");
        
        JavaMailSenderImpl sender = new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage simpleMessage) {
                log.info("DUMMY MAIL SENDER - Would send email to: {}, Subject: {}, From: {}",
                        simpleMessage.getTo() != null ? String.join(", ", simpleMessage.getTo()) : "N/A",
                        simpleMessage.getSubject(),
                        simpleMessage.getFrom());
                log.debug("Email body: {}", simpleMessage.getText());
            }
        };
        
        // Set minimal properties to avoid NPE
        sender.setHost("localhost");
        sender.setPort(25);
        
        return sender;
    }
}

