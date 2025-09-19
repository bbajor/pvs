package de.bbajor.pvs.ivomdrug.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.ivomdrug.repository.IvomDrugRepository;
import de.bbajor.pvs.ivomplan.model.IvomDrug;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IvomDrugImportService {

    private final IvomDrugRepository repo;

    @Transactional
    public int importNewDrugs(List<IvomDrug> drugListToImport) throws Exception {

        LocalDate heute = LocalDate.now();

        // Schritt 1: vorhandene Präparate laden
        List<IvomDrug> bestehende = repo.findAll();

        // Map nach Zulassungsnummer für schnellen Abgleich
        Map<String, IvomDrug> bestehendeMap = bestehende.stream()
                .collect(Collectors.toMap(IvomDrug::getZulassungsNr, d -> d));

        AtomicInteger updateCount = new AtomicInteger(0);
        // Schritt 2: Neue/aktualisierte Präparate übernehmen
        for (IvomDrug neu : drugListToImport) {
            IvomDrug alt = bestehendeMap.get(neu.getZulassungsNr());
            if (alt == null) {
                // komplett neu → einfügen
                neu.setValidFrom(heute);
                repo.save(neu);
                updateCount.incrementAndGet();
            } else {
                if (alt.getValidUntil() != null) {
                    // war schon abgelaufen → reaktivieren
                    alt.setValidFrom(heute);
                    alt.setValidUntil(null);
                    repo.save(alt);
                    updateCount.incrementAndGet();
                }
                // aus Map entfernen → wurde gesehen
                bestehendeMap.remove(neu.getZulassungsNr());
            }
        }

        // Schritt 3: Alles, was übrig bleibt, ist nicht mehr gültig
        bestehendeMap.values().forEach(drug -> {
            if (drug.getValidUntil() == null) {
                drug.setValidUntil(heute);
                repo.save(drug);
                updateCount.incrementAndGet();
            }
        });
        return updateCount.get();
    }

}
