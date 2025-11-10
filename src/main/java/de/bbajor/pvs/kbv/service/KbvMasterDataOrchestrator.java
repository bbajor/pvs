package de.bbajor.pvs.kbv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Koordiniert den zentralen KBV-Import und das anschließende Verteilen auf alle Mandanten.
 */
@Service
@RequiredArgsConstructor
public class KbvMasterDataOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(KbvMasterDataOrchestrator.class);

    private final KbvImportService importService;
    private final KbvMasterDataDistributionService distributionService;

    /**
     * Führt den Import im zentralen KBV-Dienst aus und stößt danach das Mandanten-Rollout an.
     *
     * @return {@code true}, wenn der Import erfolgreich angestoßen wurde und die Verteilung gestartet wurde.
     */
    public boolean triggerImportAndDistribute(String filePath, String quarter, String version) {
        boolean importTriggered = importService.triggerImport(filePath, quarter, version);
        if (!importTriggered) {
            log.warn("KBV import trigger failed (filePath={}, quarter={}, version={})", filePath, quarter, version);
            return false;
        }

        distributionService.distributeAcrossTenants(quarter, version);
        return true;
    }
}
