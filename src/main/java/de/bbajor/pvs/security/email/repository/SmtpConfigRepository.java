package de.bbajor.pvs.security.email.repository;

import de.bbajor.pvs.security.email.model.SmtpConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SmtpConfigRepository extends JpaRepository<SmtpConfig, Long> {
    
    /**
     * Finds the first (and should be only) SMTP configuration.
     * In practice, there should only be one configuration.
     */
    Optional<SmtpConfig> findFirstByOrderByIdAsc();
}

