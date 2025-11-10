package de.bbajor.pvs.kbv.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.kbv.events.KbvTenantDistributionEvent;
import lombok.RequiredArgsConstructor;

/**
 * Kümmert sich um das Ausrollen der KBV-Stammdaten auf alle aktiven Institutionen.
 */
@Service
@RequiredArgsConstructor
public class KbvMasterDataDistributionService {

    private static final Logger log = LoggerFactory.getLogger(KbvMasterDataDistributionService.class);

    private final InstitutionRepository institutionRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publiziert für jede aktive Institution ein Event, damit die Stammdaten
     * mandantenspezifisch aktualisiert werden können.
     *
     * @param quarter Quartal des Imports (z. B. 2025-Q1)
     * @param version optionale Versionskennung
     */
    public void distributeAcrossTenants(String quarter, String version) {
        List<Institution> institutions = institutionRepository.findAll()
                .stream()
                .filter(Institution::isActive)
                .toList();

        log.info("Trigger KBV distribution for {} active institutions (quarter={}, version={})",
                institutions.size(), quarter, version);

        institutions.forEach(institution -> eventPublisher.publishEvent(
                new KbvTenantDistributionEvent(institution.getInstitutionCode(), quarter, version)));
    }
}
