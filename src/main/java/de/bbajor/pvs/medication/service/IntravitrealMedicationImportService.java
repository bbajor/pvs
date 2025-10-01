package de.bbajor.pvs.medication.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntravitrealMedicationImportService {

    private final MedicationRepository repo;

    @Transactional
    public int importNewIntravitrealMedications(List<Medication> drugListToImport) throws Exception {

        LocalDate heute = LocalDate.now();

        // Schritt 1: vorhandene Präparate laden
        List<Medication> bestehende = repo.findAll();

        // Map nach Zulassungsnummer für schnellen Abgleich
        Map<String, Medication> bestehendeMap = bestehende.stream()
                .collect(Collectors.toMap(Medication::getZulassungsNr, d -> d));

        AtomicInteger updateCount = new AtomicInteger(0);
        // Schritt 2: Neue/aktualisierte Präparate übernehmen
        for (Medication neu : drugListToImport) {
            Medication alt = bestehendeMap.get(neu.getZulassungsNr());
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
