package de.bbajor.pvs.intravitreal.treatment.service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.repository.IvomDiagnosisRepository;
import de.bbajor.pvs.kbv.client.dto.KbvIcdEntryDto;
import de.bbajor.pvs.kbv.service.KbvMasterDataService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class IvomDiagnosisService {

    private final IvomDiagnosisRepository repository;
    private final KbvMasterDataService kbvMasterDataService;

    @Transactional
    public Diagnosis save(Diagnosis diagnosis) {
        Objects.requireNonNull(diagnosis);
        return repository.save(diagnosis);
    }

    public Collection<Diagnosis> getDiagnoses() {
        return repository.findAll();
    }

    @Transactional
    public List<Diagnosis> saveAll(List<Diagnosis> diagnosisList) {
        return repository.saveAll(diagnosisList);
    }

    public Diagnosis getByDiagnoseId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Diagnosis not found with id: " + id));
    }

    /**
     * Erstellt eine Diagnosis aus einem KBV-ICD-Eintrag.
     * Validiert den ICD-Code gegen KBV-Daten und setzt die entsprechenden Metadaten.
     *
     * @param icdCode der ICD-Code
     * @param quarter das Quartal der KBV-Daten (z.B. "2025-Q1"), optional
     * @return Optional mit der erstellten Diagnosis, oder empty wenn Code nicht gefunden
     */
    @Transactional
    public Optional<Diagnosis> createFromKbvIcd(String icdCode, String quarter) {
        if (icdCode == null || icdCode.isBlank()) {
            return Optional.empty();
        }

        // Suche in KBV-Daten
        List<KbvIcdEntryDto> kbvEntries = kbvMasterDataService.getIcdEntries(quarter, icdCode);
        if (kbvEntries.isEmpty()) {
            log.warn("ICD code {} not found in KBV data (quarter: {})", icdCode, quarter);
            return Optional.empty();
        }

        // Verwende den ersten Eintrag (sollte normalerweise nur einer sein)
        KbvIcdEntryDto kbvEntry = kbvEntries.get(0);

        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setIcdCode(kbvEntry.getCode());
        diagnosis.setName(kbvEntry.getTextContent());
        diagnosis.setDescription("ICD-10-GM: " + kbvEntry.getTextContent());
        diagnosis.setKbvQuarter(kbvEntry.getQuarter());
        diagnosis.setKbvValidFrom(kbvEntry.getValidFrom());
        diagnosis.setKbvValidTo(kbvEntry.getValidTo());
        diagnosis.setValidatedAgainstKbv(true);

        Diagnosis saved = repository.save(diagnosis);
        log.info("Created diagnosis from KBV ICD entry: {} (quarter: {})", icdCode, quarter);
        return Optional.of(saved);
    }

    /**
     * Validiert einen ICD-Code gegen KBV-Daten für ein bestimmtes Datum.
     *
     * @param icdCode der ICD-Code
     * @param date das Datum für die Validierung
     * @param quarter das Quartal der KBV-Daten (optional)
     * @return true wenn der Code gültig ist, false sonst
     */
    public boolean validateIcdCode(String icdCode, LocalDate date, String quarter) {
        if (icdCode == null || icdCode.isBlank() || date == null) {
            return false;
        }

        List<KbvIcdEntryDto> kbvEntries = kbvMasterDataService.getIcdEntries(quarter, icdCode);
        if (kbvEntries.isEmpty()) {
            return false;
        }

        // Prüfe, ob mindestens ein Eintrag für das Datum gültig ist
        return kbvEntries.stream().anyMatch(entry -> {
            LocalDate validFrom = entry.getValidFrom();
            LocalDate validTo = entry.getValidTo();
            if (validFrom == null) {
                return false;
            }
            if (date.isBefore(validFrom)) {
                return false;
            }
            if (validTo != null && date.isAfter(validTo)) {
                return false;
            }
            return true;
        });
    }

    /**
     * Validiert einen ICD-Code gegen KBV-Daten für heute.
     *
     * @param icdCode der ICD-Code
     * @param quarter das Quartal der KBV-Daten (optional)
     * @return true wenn der Code aktuell gültig ist, false sonst
     */
    public boolean validateIcdCodeCurrently(String icdCode, String quarter) {
        return validateIcdCode(icdCode, LocalDate.now(), quarter);
    }

    /**
     * Sucht nach ICD-Codes in KBV-Daten (für Autocomplete).
     *
     * @param searchTerm Suchbegriff (Code oder Text)
     * @param quarter das Quartal der KBV-Daten (optional)
     * @return Liste von KBV-ICD-Einträgen
     */
    public List<KbvIcdEntryDto> searchKbvIcdEntries(String searchTerm, String quarter) {
        if (searchTerm == null || searchTerm.isBlank()) {
            return List.of();
        }
        return kbvMasterDataService.getIcdEntries(quarter, searchTerm);
    }

    /**
     * Aktualisiert eine bestehende Diagnosis mit KBV-Metadaten.
     *
     * @param diagnosis die zu aktualisierende Diagnosis
     * @param quarter das Quartal der KBV-Daten (optional)
     * @return true wenn aktualisiert wurde, false wenn Code nicht gefunden
     */
    @Transactional
    public boolean updateWithKbvMetadata(Diagnosis diagnosis, String quarter) {
        if (diagnosis.getIcdCode() == null || diagnosis.getIcdCode().isBlank()) {
            return false;
        }

        List<KbvIcdEntryDto> kbvEntries = kbvMasterDataService.getIcdEntries(quarter, diagnosis.getIcdCode());
        if (kbvEntries.isEmpty()) {
            return false;
        }

        KbvIcdEntryDto kbvEntry = kbvEntries.get(0);
        diagnosis.setKbvQuarter(kbvEntry.getQuarter());
        diagnosis.setKbvValidFrom(kbvEntry.getValidFrom());
        diagnosis.setKbvValidTo(kbvEntry.getValidTo());
        diagnosis.setValidatedAgainstKbv(true);

        repository.save(diagnosis);
        log.info("Updated diagnosis {} with KBV metadata (quarter: {})", diagnosis.getIcdCode(), quarter);
        return true;
    }
}
