package de.bbajor.pvs.institution.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;

/**
 * Enforces "one app instance = one practice" by guaranteeing exactly one active institution.
 */
@Component
@Profile("!test")
@EnableConfigurationProperties(SinglePracticeProperties.class)
@ConditionalOnProperty(name = "app.single-practice.enabled", havingValue = "true", matchIfMissing = true)
public class SinglePracticeInstitutionEnforcer {

    private static final Logger log = LoggerFactory.getLogger(SinglePracticeInstitutionEnforcer.class);

    private final InstitutionRepository institutionRepository;
    private final SinglePracticeProperties properties;

    public SinglePracticeInstitutionEnforcer(
            InstitutionRepository institutionRepository,
            SinglePracticeProperties properties) {
        this.institutionRepository = institutionRepository;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void enforceSinglePracticeInstitution() {
        List<Institution> active = institutionRepository.findByActiveTrueOrderByIdAsc();

        if (active.isEmpty()) {
            if (!properties.autoProvisionInstitution()) {
                throw new IllegalStateException(
                        "Single-practice mode requires one active institution, but none exist.");
            }
            Institution created = createDefaultInstitution();
            log.warn("Single-practice mode auto-provisioned institution: {}", created.getInstitutionCode());
            return;
        }

        if (active.size() > 1) {
            throw new IllegalStateException(
                    "Single-practice mode requires exactly one active institution, found: " + active.size());
        }
    }

    private Institution createDefaultInstitution() {
        Institution institution = new Institution()
                .setInstitutionCode(properties.defaultInstitutionCode())
                .setInstitutionName(properties.defaultInstitutionName())
                .setDescription(properties.defaultInstitutionDescription())
                .setActive(true);

        // Legacy fields kept for backward compatibility while institution remains technical singleton.
        institution.setDatabaseName("pvs_single");
        institution.setContainerName("pvs-single");

        return institutionRepository.save(institution);
    }
}

