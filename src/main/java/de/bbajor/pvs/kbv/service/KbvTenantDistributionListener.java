package de.bbajor.pvs.kbv.service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.model.InstitutionSettings;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.repository.InstitutionSettingsRepository;
import de.bbajor.pvs.kbv.events.KbvTenantDistributionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Reagiert auf KBV-Verteilungsereignisse, aktualisiert Meta-Informationen pro Institution
 * und bildet den Einstiegspunkt für spätere mandantenspezifische Update-Jobs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KbvTenantDistributionListener {

    private final InstitutionRepository institutionRepository;
    private final InstitutionSettingsRepository institutionSettingsRepository;

    @EventListener
    @Transactional
    public void onDistributionRequested(KbvTenantDistributionEvent event) {
        institutionRepository.findByInstitutionCode(event.institutionCode())
                .ifPresentOrElse(institution -> {
                    InstitutionSettings settings = institutionSettingsRepository
                            .findByInstitutionInstitutionCode(event.institutionCode())
                            .orElseGet(() -> institutionSettingsRepository.save(institution.ensureSettings()));

                    settings.setKbvLastImportQuarter(event.quarter());
                    settings.setKbvLastImportVersion(event.version());
                    settings.setKbvLastImportedAt(OffsetDateTime.now(ZoneOffset.UTC));

                    log.info("Recorded KBV distribution request for institution={} (quarter={}, version={})",
                            event.institutionCode(), event.quarter(), event.version());

                    institutionSettingsRepository.save(settings);

                    // TODO: Sobald Tenant-Routing verfügbar ist, hier mandantenspezifische Sync-Logik ausführen.
                }, () -> log.warn("Skip KBV distribution for unknown institution {}", event.institutionCode()));
    }
}
