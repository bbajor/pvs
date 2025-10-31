package de.bbajor.pvs.init;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Initializes test data for multi-tenancy in dev/test environments.
 */
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
@Slf4j
public class TenantTestDataInitializer {

    private final TenantRepository tenantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initTestTenants() {
        log.info("Initializing test tenants for multi-tenancy...");

        // Create default test tenant
        Tenant testTenant = createTenantIfNotExists(
                "DEV-TEST",
                "Test-Praxis (Dev)",
                "Standard-Test-Praxis für Entwicklung"
        );

        // Create sample tenants
        Tenant tenant1 = createTenantIfNotExists(
                "PRAX-001",
                "Augenarztpraxis Dr. Müller",
                "Praxis in Berlin"
        );

        Tenant tenant2 = createTenantIfNotExists(
                "PRAX-002",
                "MVZ Augenheilkunde Hamburg",
                "Medizinisches Versorgungszentrum"
        );

        // Create super admin (no tenant, can manage all tenants)
        createUserIfNotExists(
                null,
                "superadmin",
                "admin@pvs.local",
                "Super Administrator",
                Set.of("SUPER_ADMIN", "ADMIN", "USER")
        );

        // Create test users for each tenant
        createUserIfNotExists(
                testTenant,
                "testadmin",
                "testadmin@test.local",
                "Test Admin",
                Set.of("ADMIN", "USER")
        );

        createUserIfNotExists(
                tenant1,
                "dr.mueller",
                "mueller@praxis.local",
                "Dr. Müller",
                Set.of("ADMIN", "USER")
        );

        createUserIfNotExists(
                tenant2,
                "dr.schmidt",
                "schmidt@mvz.local",
                "Dr. Schmidt",
                Set.of("ADMIN", "USER")
        );

        log.info("Test tenants and users initialized successfully");
    }

    private Tenant createTenantIfNotExists(String code, String name, String description) {
        return tenantRepository.findByTenantCode(code)
                .orElseGet(() -> {
                    Tenant tenant = new Tenant()
                            .setTenantCode(code)
                            .setTenantName(name)
                            .setDescription(description)
                            .setActive(true);
                    Tenant saved = tenantRepository.save(tenant);
                    log.info("Created tenant: {} ({})", name, code);
                    return saved;
                });
    }

    private void createUserIfNotExists(Tenant tenant, String username, String email, 
            String fullName, Set<String> roles) {
        userAccountRepository.findByUsername(username)
                .orElseGet(() -> {
                    UserAccount user = new UserAccount()
                            .setUsername(username)
                            .setEmail(email)
                            .setFullName(fullName)
                            .setPasswordHash(passwordEncoder.encode("123"))
                            .setEnabled(true)
                            .setTenant(tenant)
                            .setRoles(roles);
                    UserAccount saved = userAccountRepository.save(user);
                    log.info("Created user: {} for tenant: {}", 
                            username, 
                            tenant != null ? tenant.getTenantCode() : "SUPER_ADMIN");
                    return saved;
                });
    }
}
