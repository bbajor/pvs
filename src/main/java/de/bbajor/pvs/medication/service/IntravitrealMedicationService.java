package de.bbajor.pvs.medication.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntravitrealMedicationService {

    @Autowired
    private MedicationMapper medicationMapper;
    @Autowired
    private MedicationRepository medicationRepository;

    public Optional<Medication> findById(Long id) {
        return medicationRepository.findById(id);
    }

    public List<Medication> findIntravitrealMedication(String filter) {
        Specification<Medication> spec = (root, query, cb) -> {
            String likeFilter = "%" + filter.toLowerCase() + "%";
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("arzneimittelbezeichnung")), likeFilter),
                    cb.like(cb.lower(root.get("zulassungsNr")), likeFilter)));
            return cb.or(predicates.toArray(new Predicate[0]));
        };

        return medicationRepository.findAll(spec);
    }

    public List<Medication> findAll() {
        return medicationRepository.findAll();
    }

    @Transactional
    public Medication save(Medication update) {
        if (update.getId() == null || update.getId() <= 0) {
            update.setId(null);
            return medicationRepository.save(update);
        } else {
            Medication medication = medicationRepository.getReferenceById(update.getId());
            medicationMapper.updateMedication(update, medication);
            return medicationRepository.save(medication);
        }
    }

    @Transactional
    public List<Medication> saveAll(List<Medication> medications) {
        return medicationRepository.saveAll(medications);
    }

    public Optional<Medication> findActiveByZulassungsNr(String zulassungsNr) {
        return medicationRepository.findFirstByZulassungsNrAndValidUntilIsNull(zulassungsNr);
    }

    public Optional<Medication> findActiveByEingangsnummer(String eingangsnummer) {
        return medicationRepository.findFirstByEingangsnummerAndValidUntilIsNull(eingangsnummer);
    }

    /**
     * Legt ein neues aktives Medikament an (ohne CSV/DIMDI).
     * Duplikatcheck: Eingangsnummer, Zulassungsnr., EU-Verfahrensnummer (falls gesetzt);
     * ohne diese Felder: Kombination Bezeichnung + Wirkstoffe.
     */
    @Transactional
    public Medication createManualMedication(Medication draft) {
        String bez = requireNonBlank(draft.getArzneimittelbezeichnung(), "Arzneimittelbezeichnung");
        String wirk = requireNonBlank(draft.getWirkstoffe(), "Wirkstoffe");

        String eingangsnummer = blankToNull(draft.getEingangsnummer());
        String zulassungsNr = blankToNull(draft.getZulassungsNr());
        String euNr = blankToNull(draft.getEuVerfahrensnummer());

        if (eingangsnummer != null && medicationRepository.existsActiveByEingangsnummerIgnoreCase(eingangsnummer)) {
            throw new MedicationDuplicateException("Eingangsnummer ist bereits vergeben.");
        }
        if (zulassungsNr != null && medicationRepository.existsActiveByZulassungsNrIgnoreCase(zulassungsNr)) {
            throw new MedicationDuplicateException("Zulassungsnummer ist bereits vergeben.");
        }
        if (euNr != null && medicationRepository.existsActiveByEuVerfahrensnummerIgnoreCase(euNr)) {
            throw new MedicationDuplicateException("EU-Verfahrensnummer ist bereits vergeben.");
        }
        boolean noStrongIds = eingangsnummer == null && zulassungsNr == null && euNr == null;
        if (noStrongIds
                && medicationRepository.existsActiveByBezeichnungAndWirkstoffIgnoreCase(bez.trim(), wirk.trim())) {
            throw new MedicationDuplicateException(
                    "Ein aktives Medikament mit gleicher Bezeichnung und denselben Wirkstoffen existiert bereits.");
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Medication m = new Medication();
        m.setArzneimittelbezeichnung(bez.trim());
        m.setWirkstoffe(wirk.trim());
        m.setEingangsnummer(eingangsnummer);
        m.setZulassungsNr(zulassungsNr);
        m.setZulassungsRegNrOderKennziffer(zulassungsNr);
        m.setEuVerfahrensnummer(euNr);
        m.setDarreichungsform(blankToNull(draft.getDarreichungsform()));
        m.setZulassungsinhaber(blankToNull(draft.getZulassungsinhaber()));
        m.setZielgruppe(blankToNull(draft.getZielgruppe()));
        m.setAnwendungsart(blankToNull(draft.getAnwendungsart()));
        m.setAnwendungsgebiete(blankToNull(draft.getAnwendungsgebiete()));
        m.setDescription(blankToNull(draft.getDescription()));
        m.setAdditionalNotes(blankToNull(draft.getAdditionalNotes()));
        m.setValidFrom(today);
        m.setValidUntil(null);
        m.setId(null);
        m.setVersion(0L);
        return medicationRepository.save(m);
    }

    private static String requireNonBlank(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " ist erforderlich.");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
