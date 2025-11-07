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

        // Map nach Zulassungsnummer und Eingangsnummer für schnellen Abgleich
        // Verwende beide Felder, da CSV beide enthalten kann
        Map<String, Medication> bestehendeMap = bestehende.stream()
                .filter(d -> d.getZulassungsNr() != null && !d.getZulassungsNr().isEmpty())
                .collect(Collectors.toMap(Medication::getZulassungsNr, d -> d, (d1, d2) -> d1));
        
        // Zusätzliche Map für Eingangsnummer (falls ZulassungsNr nicht vorhanden)
        Map<String, Medication> bestehendeByEingangsnummer = bestehende.stream()
                .filter(d -> d.getEingangsnummer() != null && !d.getEingangsnummer().isEmpty())
                .collect(Collectors.toMap(Medication::getEingangsnummer, d -> d, (d1, d2) -> d1));

        AtomicInteger updateCount = new AtomicInteger(0);
        // Schritt 2: Neue/aktualisierte Präparate übernehmen
        for (Medication neu : drugListToImport) {
            if (neu.getZulassungsNr() == null || neu.getZulassungsNr().isEmpty()) {
                // Fallback: use eingangsnummer if zulassungsNr is empty
                if (neu.getEingangsnummer() != null && !neu.getEingangsnummer().isEmpty()) {
                    neu.setZulassungsNr(neu.getEingangsnummer());
                }
            }
            
            Medication alt = null;
            if (neu.getZulassungsNr() != null && !neu.getZulassungsNr().isEmpty()) {
                alt = bestehendeMap.get(neu.getZulassungsNr());
            }
            // Fallback: try to find by eingangsnummer
            if (alt == null && neu.getEingangsnummer() != null && !neu.getEingangsnummer().isEmpty()) {
                alt = bestehendeByEingangsnummer.get(neu.getEingangsnummer());
            }
            
            if (alt == null) {
                // komplett neu → einfügen
                neu.setValidFrom(heute);
                repo.save(neu);
                updateCount.incrementAndGet();
            } else {
                // Update existing medication with new data
                alt.setArzneimittelbezeichnung(neu.getArzneimittelbezeichnung());
                alt.setDarreichungsform(neu.getDarreichungsform());
                alt.setZielgruppe(neu.getZielgruppe());
                alt.setAnwendungsart(neu.getAnwendungsart());
                alt.setAnwendungsgebiete(neu.getAnwendungsgebiete());
                alt.setIndikationAtc(neu.getIndikationAtc());
                alt.setBescheiddatumZulassung(neu.getBescheiddatumZulassung());
                alt.setZulassungsstatus(neu.getZulassungsstatus());
                alt.setZulassungsRegNrOderKennziffer(neu.getZulassungsRegNrOderKennziffer());
                alt.setVerkehrsfaehigkeit(neu.getVerkehrsfaehigkeit());
                alt.setParallelimportinformationen(neu.getParallelimportinformationen());
                alt.setEuVerfahrensnummer(neu.getEuVerfahrensnummer());
                alt.setZulassungsinhaber(neu.getZulassungsinhaber());
                alt.setHerstellerEndfreigabe(neu.getHerstellerEndfreigabe());
                alt.setVertreiber(neu.getVertreiber());
                alt.setOertlicherVertreter(neu.getOertlicherVertreter());
                alt.setWirkstoffe(neu.getWirkstoffe());
                alt.setPackungsgroessenGruppe(neu.getPackungsgroessenGruppe());
                alt.setAmKlassifikationen(neu.getAmKlassifikationen());
                alt.setEingangsnummer(neu.getEingangsnummer());
                
                if (alt.getValidUntil() != null) {
                    // war schon abgelaufen → reaktivieren
                    alt.setValidFrom(heute);
                    alt.setValidUntil(null);
                }
                repo.save(alt);
                updateCount.incrementAndGet();
                
                // aus Map entfernen → wurde gesehen
                if (alt.getZulassungsNr() != null && !alt.getZulassungsNr().isEmpty()) {
                    bestehendeMap.remove(alt.getZulassungsNr());
                }
                if (alt.getEingangsnummer() != null && !alt.getEingangsnummer().isEmpty()) {
                    bestehendeByEingangsnummer.remove(alt.getEingangsnummer());
                }
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
