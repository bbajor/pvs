package de.bbajor.pvs.init;

import java.security.SecureRandom;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

/**
 * Initializes the Super-Admin user for production environments.
 * 
 * <p>
 * This initializer runs after the application starts and:
 * <ul>
 * <li>Checks if a Super-Admin user already exists</li>
 * <li>If not, creates a Super-Admin with a randomly generated password</li>
 * <li>Outputs the generated password to the console (only on first startup)</li>
 * <li>Sets passwordChangeRequired=true and initialPasswordSet=false</li>
 * </ul>
 * </p>
 * 
 * <p>
 * The initial password can be overridden by setting the environment variable
 * {@code SUPER_ADMIN_INITIAL_PASSWORD}. If set, this password will be used instead of generating one.
 * </p>
 */
@Component
@Profile({"prod", "onpremise"})
public class ProductionSuperAdminInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProductionSuperAdminInitializer.class);
    private static final String SUPER_ADMIN_USERNAME = "superadmin";
    private static final int PASSWORD_LENGTH = 24;
    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public ProductionSuperAdminInitializer(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            Environment environment) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeSuperAdmin() {
        log.info("Checking for Super-Admin user...");

        // Check if Super-Admin already exists
        boolean superAdminExists = userAccountRepository.findAll().stream()
                .anyMatch(account -> account.getRoles().contains(AppRoles.SUPER_ADMIN));

        if (superAdminExists) {
            log.info("Super-Admin user already exists. Skipping initialization.");
            return;
        }

        log.info("Super-Admin user not found. Creating initial Super-Admin...");

        // Generate or get initial password
        String initialPassword = getInitialPassword();

        // Create Super-Admin user
        UserAccount superAdmin = new UserAccount();
        superAdmin.setUsername(SUPER_ADMIN_USERNAME);
        superAdmin.setFullName("Super Administrator");
        superAdmin.setEmail(environment.getProperty("app.super-admin.email", "admin@example.com"));
        superAdmin.setEnabled(true);
        superAdmin.setRoles(Set.of(AppRoles.SUPER_ADMIN));
        superAdmin.setPasswordHash(passwordEncoder.encode(initialPassword));
        superAdmin.setPasswordChangeRequired(true);
        superAdmin.setInitialPasswordSet(false);
        superAdmin.setMfaEnabled(false);

        // Generate userId if not set
        if (superAdmin.getUserId() == null || superAdmin.getUserId().isEmpty()) {
            superAdmin.setUserId(java.util.UUID.randomUUID().toString());
        }

        userAccountRepository.save(superAdmin);

        // Output password to console
        printInitialCredentialNotice(initialPassword);
    }

    /**
     * Gets the initial password from environment variable or generates a random one.
     */
    private String getInitialPassword() {
        String envPassword = environment.getProperty("SUPER_ADMIN_INITIAL_PASSWORD");
        if (envPassword != null && !envPassword.isEmpty()) {
            log.info("Using initial Super-Admin password from environment variable");
            return envPassword;
        }

        // Generate random password
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(PASSWORD_LENGTH);
        
        // Ensure at least one character from each required category
        password.append(getRandomChar("ABCDEFGHIJKLMNOPQRSTUVWXYZ", random)); // Uppercase
        password.append(getRandomChar("abcdefghijklmnopqrstuvwxyz", random)); // Lowercase
        password.append(getRandomChar("0123456789", random)); // Digit
        password.append(getRandomChar("!@#$%^&*", random)); // Special char
        
        // Fill the rest randomly
        for (int i = 4; i < PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_CHARS.charAt(random.nextInt(PASSWORD_CHARS.length())));
        }
        
        // Shuffle the password
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

    private void printInitialCredentialNotice(String initialPassword) {
        boolean configuredByEnvironment = environment.getProperty("SUPER_ADMIN_INITIAL_PASSWORD") != null
                && !environment.getProperty("SUPER_ADMIN_INITIAL_PASSWORD").isEmpty();

        log.warn("========================================");
        log.warn("SUPER-ADMIN INITIAL CREDENTIALS CREATED");
        log.warn("========================================");
        log.warn("Username: {}", SUPER_ADMIN_USERNAME);
        if (configuredByEnvironment) {
            log.warn("Password: configured via SUPER_ADMIN_INITIAL_PASSWORD");
        } else {
            log.warn("Password: {}", initialPassword);
            log.warn("This generated password is only shown once. Save it securely.");
        }
        log.warn("IMPORTANT: Change this password after first login.");
        log.warn("========================================");

        if (!configuredByEnvironment) {
            System.out.println("========================================");
            System.out.println("SUPER-ADMIN INITIAL CREDENTIALS CREATED");
            System.out.println("========================================");
            System.out.println("Username: " + SUPER_ADMIN_USERNAME);
            System.out.println("Password: " + initialPassword);
            System.out.println("IMPORTANT: Change this password after first login.");
            System.out.println("This generated password is only shown once. Save it securely.");
            System.out.println("========================================");
        }
    }
}
