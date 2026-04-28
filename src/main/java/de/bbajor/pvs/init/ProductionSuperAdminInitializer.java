package de.bbajor.pvs.init;

import java.security.SecureRandom;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

/**
 * Legt den ersten {@link AppRoles#ADMIN} an, wenn eine Institution existiert, aber noch kein Admin-Benutzer.
 */
@Component
@Profile("prod")
public class ProductionSuperAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProductionSuperAdminInitializer.class);
    private static final String BOOTSTRAP_USERNAME = "admin";
    private static final int PASSWORD_LENGTH = 24;
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    private final UserAccountRepository userAccountRepository;
    private final InstitutionRepository institutionRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public ProductionSuperAdminInitializer(
            UserAccountRepository userAccountRepository,
            InstitutionRepository institutionRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        this.userAccountRepository = userAccountRepository;
        this.institutionRepository = institutionRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBootstrapAdmin() {
        log.info("Checking for initial admin user...");

        boolean adminExists = userAccountRepository.findAll().stream()
                .anyMatch(account -> account.getRoles().contains(AppRoles.ADMIN));

        if (adminExists) {
            log.info("An owner/admin user already exists. Skipping bootstrap.");
            return;
        }

        List<Institution> institutions = institutionRepository.findAll();
        if (institutions.isEmpty()) {
            log.warn("No institution in database — cannot create bootstrap admin.");
            return;
        }

        Institution institution = institutions.get(0);
        log.info("Creating initial admin for institution {}...", institution.getInstitutionCode());

        String initialPassword = getInitialPassword();

        UserAccount admin = new UserAccount();
        admin.setUsername(BOOTSTRAP_USERNAME);
        admin.setFullName("Administrator");
        admin.setEmail(environment.getProperty("app.bootstrap-admin.email", "admin@example.com"));
        admin.setEnabled(true);
        admin.setInstitution(institution);
        admin.setRoles(Set.of(AppRoles.ADMIN, AppRoles.USER));
        admin.setPasswordHash(passwordEncoder.encode(initialPassword));
        admin.setPasswordChangeRequired(true);
        admin.setInitialPasswordSet(false);
        admin.setMfaEnabled(false);

        if (admin.getUserId() == null || admin.getUserId().isEmpty()) {
            admin.setUserId(java.util.UUID.randomUUID().toString());
        }

        userAccountRepository.save(admin);

        log.warn("========================================");
        log.warn("INITIAL PRACTICE ADMIN PASSWORD");
        log.warn("========================================");
        log.warn("Username: {}", BOOTSTRAP_USERNAME);
        log.warn("Password: {}", initialPassword);
        log.warn("========================================");
        System.out.println("========================================");
        System.out.println("INITIAL ADMIN PASSWORD");
        System.out.println("========================================");
        System.out.println("Username: " + BOOTSTRAP_USERNAME);
        System.out.println("Password: " + initialPassword);
        System.out.println("========================================");
    }

    private String getInitialPassword() {
        String envPassword = environment.getProperty("BOOTSTRAP_ADMIN_INITIAL_PASSWORD");
        if (envPassword != null && !envPassword.isEmpty()) {
            log.info("Using password from BOOTSTRAP_ADMIN_INITIAL_PASSWORD");
            return envPassword;
        }
        String legacy = environment.getProperty("SUPER_ADMIN_INITIAL_PASSWORD");
        if (legacy != null && !legacy.isEmpty()) {
            log.info("Using password from SUPER_ADMIN_INITIAL_PASSWORD (deprecated alias)");
            return legacy;
        }

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        password.append(getRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ", random));
        password.append(getRandomChar("abcdefghijklmnopqrstuvwxyz", random));
        password.append(getRandomChar("0123456789", random));
        password.append(getRandomChar("!@#$%^&*", random));
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[j];
            passwordArray[j] = temp;
        }
        return new String(passwordArray);
    }

    private char getRandomChar(String chars, SecureRandom random) {
        return chars.charAt(random.nextInt(chars.length()));
    }
}
