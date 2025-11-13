package de.bbajor.pvs.medication.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.repository.MedicationFavouriteRepository;
import de.bbajor.pvs.medication.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicationFavouriteService {

    private final MedicationFavouriteRepository favouriteRepository;
    private final MedicationRepository medicationRepository;
    private final InstitutionRepository institutionRepository;

    @Transactional(readOnly = true)
    public List<MedicationFavourite> getActiveFavouritesForCurrentInstitution() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return Collections.emptyList();
        }
        return favouriteRepository.findByInstitutionIdAndActiveTrueWithMedication(institutionId);
    }

    @Transactional
    public MedicationFavourite addFavouriteForCurrentInstitution(Long medicationId, String displayName) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Kein Institutionskontext vorhanden – Favoriten können nur innerhalb einer Institution verwaltet werden.");
        }
        return addOrReactivateFavourite(institutionId, medicationId, displayName, LocalDate.now());
    }

    @Transactional
    public MedicationFavourite addOrReactivateFavourite(Long institutionId, Long medicationId, String displayName, LocalDate validFrom) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannte Institution: " + institutionId));
        Medication medication = medicationRepository.findById(medicationId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekanntes Medikament: " + medicationId));

        Optional<MedicationFavourite> existing = favouriteRepository.findByInstitutionIdAndMedicationId(institutionId, medicationId);
        if (existing.isPresent()) {
            MedicationFavourite favourite = existing.get();
            favourite.setActive(true);
            favourite.setValidUntil(null);
            favourite.setValidFrom(validFrom);
            if (displayName != null && !displayName.isBlank()) {
                favourite.setDisplayName(displayName);
            }
            return favouriteRepository.save(favourite);
        }

        MedicationFavourite favourite = new MedicationFavourite()
                .setInstitution(institution)
                .setMedication(medication)
                .setDisplayName(displayName)
                .setActive(true)
                .setValidFrom(validFrom);

        return favouriteRepository.save(favourite);
    }

    @Transactional
    public void deactivateFavourite(Long favouriteId) {
        MedicationFavourite favourite = favouriteRepository.findById(favouriteId)
                .orElseThrow(() -> new IllegalArgumentException("Unbekannter Medikamentenfavorit: " + favouriteId));
        favourite.setActive(false);
        favourite.setValidUntil(LocalDate.now());
        favouriteRepository.save(favourite);
    }

    @Transactional(readOnly = true)
    public List<MedicationFavourite> findActiveFavouritesByMedication(Long medicationId) {
        return favouriteRepository.findByMedicationIdAndActiveTrue(medicationId);
    }

    @Transactional
    public void replaceActiveFavouritesWithNewMedication(Medication oldMedication, Medication newMedication, LocalDate effectiveFrom) {
        List<MedicationFavourite> activeFavourites = favouriteRepository
                .findByMedicationIdAndActiveTrue(oldMedication.getId());

        for (MedicationFavourite favourite : activeFavourites) {
            favourite.setActive(false);
            LocalDate endDate = effectiveFrom.minusDays(1);
            if (favourite.getValidFrom() != null && !effectiveFrom.isAfter(favourite.getValidFrom())) {
                endDate = effectiveFrom;
            }
            favourite.setValidUntil(endDate);
            favouriteRepository.save(favourite);

            MedicationFavourite replacement = new MedicationFavourite()
                    .setInstitution(favourite.getInstitution())
                    .setMedication(newMedication)
                    .setDisplayName(favourite.getDisplayName())
                    .setActive(true)
                    .setValidFrom(effectiveFrom);
            favouriteRepository.save(replacement);
        }
    }

    @Transactional(readOnly = true)
    public List<MedicationFavourite> getActiveFavouritesForInstitution(Long institutionId) {
        return favouriteRepository.findByInstitutionIdAndActiveTrue(institutionId);
    }
}

