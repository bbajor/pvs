package de.bbajor.pvs.kbv.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Koordiniert den zentralen KBV-Import und das anschließende Verteilen auf alle Mandanten.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KbvMasterDataOrchestrator {

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
