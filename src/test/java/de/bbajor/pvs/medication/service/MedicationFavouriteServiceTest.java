package de.bbajor.pvs.medication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.repository.MedicationFavouriteRepository;
import de.bbajor.pvs.medication.repository.MedicationRepository;

@ExtendWith(MockitoExtension.class)
class MedicationFavouriteServiceTest {

    @Mock
    private MedicationFavouriteRepository favouriteRepository;
    @Mock
    private MedicationRepository medicationRepository;
    @Mock
    private InstitutionRepository institutionRepository;

    @Captor
    private ArgumentCaptor<MedicationFavourite> favouriteCaptor;

    private MedicationFavouriteService service;

    @BeforeEach
    void setUp() {
        service = new MedicationFavouriteService(favouriteRepository, medicationRepository, institutionRepository);
    }

    @Test
    void replaceActiveFavouritesWithNewMedication_closesOldFavouriteAndCreatesReplacement() {
        Medication oldMedication = new Medication();
        oldMedication.setId(1L);
        Medication newMedication = new Medication();
        newMedication.setId(2L);

        Institution institution = new Institution();
        institution.setId(10L);

        MedicationFavourite existingFavourite = new MedicationFavourite()
                .setInstitution(institution)
                .setMedication(oldMedication)
                .setDisplayName("Lucentis")
                .setActive(true)
                .setValidFrom(LocalDate.of(2024, 1, 1));

        when(favouriteRepository.findByMedicationIdAndActiveTrue(oldMedication.getId()))
                .thenReturn(List.of(existingFavourite));

        LocalDate effectiveFrom = LocalDate.of(2025, 1, 1);
        service.replaceActiveFavouritesWithNewMedication(oldMedication, newMedication, effectiveFrom);

        verify(favouriteRepository, times(2)).save(favouriteCaptor.capture());
        List<MedicationFavourite> savedFavourites = favouriteCaptor.getAllValues();

        MedicationFavourite deactivatedFavourite = savedFavourites.get(0);
        MedicationFavourite replacementFavourite = savedFavourites.get(1);

        assertFalse(deactivatedFavourite.isActive());
        assertEquals(LocalDate.of(2024, 12, 31), deactivatedFavourite.getValidUntil());

        assertTrue(replacementFavourite.isActive());
        assertEquals(newMedication, replacementFavourite.getMedication());
        assertEquals(institution, replacementFavourite.getInstitution());
        assertEquals("Lucentis", replacementFavourite.getDisplayName());
        assertEquals(effectiveFrom, replacementFavourite.getValidFrom());
        assertEquals(null, replacementFavourite.getValidUntil());
    }

    @Test
    void replaceActiveFavouritesWithNewMedication_whenEffectiveOnValidFrom_setsValidUntilToEffectiveDate() {
        Medication oldMedication = new Medication();
        oldMedication.setId(3L);
        Medication newMedication = new Medication();
        newMedication.setId(4L);

        Institution institution = new Institution();
        institution.setId(20L);

        MedicationFavourite existingFavourite = new MedicationFavourite()
                .setInstitution(institution)
                .setMedication(oldMedication)
                .setDisplayName("Eylea")
                .setActive(true)
                .setValidFrom(LocalDate.of(2025, 2, 1));

        when(favouriteRepository.findByMedicationIdAndActiveTrue(oldMedication.getId()))
                .thenReturn(List.of(existingFavourite));

        LocalDate effectiveFrom = LocalDate.of(2025, 2, 1);
        service.replaceActiveFavouritesWithNewMedication(oldMedication, newMedication, effectiveFrom);

        verify(favouriteRepository, times(2)).save(favouriteCaptor.capture());

        MedicationFavourite deactivatedFavourite = favouriteCaptor.getAllValues().get(0);
        assertFalse(deactivatedFavourite.isActive());
        assertEquals(effectiveFrom, deactivatedFavourite.getValidUntil());
    }
}

