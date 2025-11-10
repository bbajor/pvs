package de.bbajor.pvs.medication.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IntravitrealMedicationImportService {

    private final MedicationRepository repo;
    private final MedicationFavouriteService medicationFavouriteService;

    @Transactional
    public int importNewIntravitrealMedications(List<Medication> drugListToImport) throws Exception {

        LocalDate heute = LocalDate.now();

        // Schritt 1: aktive Präparate laden
        List<Medication> alleMedikamente = repo.findAll();
        Map<Long, Medication> aktiveMedikamenteNachId = new HashMap<>();
        Map<String, Medication> aktiveNachZulassungsnummer = new HashMap<>();
        Map<String, Medication> aktiveNachEingangsnummer = new HashMap<>();

        for (Medication medication : alleMedikamente) {
            if (medication.getValidUntil() == null) {
                aktiveMedikamenteNachId.put(medication.getId(), medication);
                if (hasText(medication.getZulassungsNr())) {
                    aktiveNachZulassungsnummer.put(medication.getZulassungsNr(), medication);
                }
                if (hasText(medication.getEingangsnummer())) {
                    aktiveNachEingangsnummer.put(medication.getEingangsnummer(), medication);
                }
            }
        }

        Set<Long> unangetasteteAktiveIds = new HashSet<>(aktiveMedikamenteNachId.keySet());

        AtomicInteger updateCount = new AtomicInteger(0);
        // Schritt 2: Neue/aktualisierte Präparate übernehmen
        for (Medication neu : drugListToImport) {
            if (!hasText(neu.getZulassungsNr()) && hasText(neu.getEingangsnummer())) {
                neu.setZulassungsNr(neu.getEingangsnummer());
            }

            Medication aktiv = null;
            if (hasText(neu.getZulassungsNr())) {
                aktiv = aktiveNachZulassungsnummer.get(neu.getZulassungsNr());
            }
            if (aktiv == null && hasText(neu.getEingangsnummer())) {
                aktiv = aktiveNachEingangsnummer.get(neu.getEingangsnummer());
            }

            if (aktiv == null) {
                // komplett neu → einfügen
                neu.setValidFrom(heute);
                neu.setValidUntil(null);
                neu.setId(null);
                neu.setVersion(0L);
                Medication gespeichert = repo.save(neu);
                if (hasText(gespeichert.getZulassungsNr())) {
                    aktiveNachZulassungsnummer.put(gespeichert.getZulassungsNr(), gespeichert);
                }
                if (hasText(gespeichert.getEingangsnummer())) {
                    aktiveNachEingangsnummer.put(gespeichert.getEingangsnummer(), gespeichert);
                }
                updateCount.incrementAndGet();
            } else {
                unangetasteteAktiveIds.remove(aktiv.getId());

                if (hasRelevantDifferences(aktiv, neu)) {
                    // Bestehenden Datensatz historisieren und neuen anlegen
                    LocalDate validUntil = determineValidUntil(heute, aktiv.getValidFrom());
                    aktiv.setValidUntil(validUntil);
                    repo.save(aktiv);

                    if (hasText(aktiv.getZulassungsNr())) {
                        aktiveNachZulassungsnummer.remove(aktiv.getZulassungsNr());
                    }
                    if (hasText(aktiv.getEingangsnummer())) {
                        aktiveNachEingangsnummer.remove(aktiv.getEingangsnummer());
                    }

                    neu.setId(null);
                    neu.setVersion(0L);
                    neu.setValidFrom(heute);
                    neu.setValidUntil(null);
                    Medication neueVersion = repo.save(neu);

                    if (hasText(neueVersion.getZulassungsNr())) {
                        aktiveNachZulassungsnummer.put(neueVersion.getZulassungsNr(), neueVersion);
                    }
                    if (hasText(neueVersion.getEingangsnummer())) {
                        aktiveNachEingangsnummer.put(neueVersion.getEingangsnummer(), neueVersion);
                    }

                    medicationFavouriteService.replaceActiveFavouritesWithNewMedication(aktiv, neueVersion, heute);
                    updateCount.incrementAndGet();
                } else if (aktiv.getValidUntil() != null) {
                    // Datensatz war deaktiviert und wird reaktiviert
                    aktiv.setValidUntil(null);
                    repo.save(aktiv);
                    updateCount.incrementAndGet();
                }
            }
        }

        // Schritt 3: Alles, was übrig bleibt, ist nicht mehr gültig
        for (Long medicationId : unangetasteteAktiveIds) {
            Medication medication = aktiveMedikamenteNachId.get(medicationId);
            if (medication != null && medication.getValidUntil() == null) {
                medication.setValidUntil(heute);
                repo.save(medication);
                updateCount.incrementAndGet();
            }
        }
        return updateCount.get();
    }

    private boolean hasRelevantDifferences(Medication bestaendiger, Medication neu) {
        return !Objects.equals(bestaendiger.getArzneimittelbezeichnung(), neu.getArzneimittelbezeichnung())
                || !Objects.equals(bestaendiger.getDarreichungsform(), neu.getDarreichungsform())
                || !Objects.equals(bestaendiger.getZielgruppe(), neu.getZielgruppe())
                || !Objects.equals(bestaendiger.getAnwendungsart(), neu.getAnwendungsart())
                || !Objects.equals(bestaendiger.getAnwendungsgebiete(), neu.getAnwendungsgebiete())
                || !Objects.equals(bestaendiger.getIndikationAtc(), neu.getIndikationAtc())
                || !Objects.equals(bestaendiger.getBescheiddatumZulassung(), neu.getBescheiddatumZulassung())
                || !Objects.equals(bestaendiger.getZulassungsstatus(), neu.getZulassungsstatus())
                || !Objects.equals(bestaendiger.getZulassungsRegNrOderKennziffer(), neu.getZulassungsRegNrOderKennziffer())
                || !Objects.equals(bestaendiger.getVerkehrsfaehigkeit(), neu.getVerkehrsfaehigkeit())
                || !Objects.equals(bestaendiger.getParallelimportinformationen(), neu.getParallelimportinformationen())
                || !Objects.equals(bestaendiger.getEuVerfahrensnummer(), neu.getEuVerfahrensnummer())
                || !Objects.equals(bestaendiger.getZulassungsinhaber(), neu.getZulassungsinhaber())
                || !Objects.equals(bestaendiger.getHerstellerEndfreigabe(), neu.getHerstellerEndfreigabe())
                || !Objects.equals(bestaendiger.getVertreiber(), neu.getVertreiber())
                || !Objects.equals(bestaendiger.getOertlicherVertreter(), neu.getOertlicherVertreter())
                || !Objects.equals(bestaendiger.getWirkstoffe(), neu.getWirkstoffe())
                || !Objects.equals(bestaendiger.getPackungsgroessenGruppe(), neu.getPackungsgroessenGruppe())
                || !Objects.equals(bestaendiger.getAmKlassifikationen(), neu.getAmKlassifikationen())
                || !Objects.equals(bestaendiger.getDescription(), neu.getDescription())
                || !Objects.equals(bestaendiger.getAdditionalNotes(), neu.getAdditionalNotes())
                || !Objects.equals(bestaendiger.getEingangsnummer(), neu.getEingangsnummer());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private LocalDate determineValidUntil(LocalDate today, LocalDate validFrom) {
        if (validFrom == null) {
            return today;
        }
        if (today.isAfter(validFrom)) {
            return today.minusDays(1);
        }
        return today;
    }
}
