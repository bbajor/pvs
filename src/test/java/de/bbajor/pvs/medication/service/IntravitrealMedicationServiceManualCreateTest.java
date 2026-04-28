package de.bbajor.pvs.medication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.repository.MedicationRepository;

@ExtendWith(MockitoExtension.class)
class IntravitrealMedicationServiceManualCreateTest {

    @Mock
    private MedicationMapper medicationMapper;

    @Mock
    private MedicationRepository medicationRepository;

    @InjectMocks
    private IntravitrealMedicationService service;

    @Test
    void createManualMedication_rejectsDuplicateEingangsnummer() {
        when(medicationRepository.existsActiveByEingangsnummerIgnoreCase("12345")).thenReturn(true);

        Medication draft = new Medication()
                .setArzneimittelbezeichnung("Test")
                .setWirkstoffe("Wirk A")
                .setEingangsnummer("12345");

        assertThatThrownBy(() -> service.createManualMedication(draft))
                .isInstanceOf(MedicationDuplicateException.class)
                .hasMessageContaining("Eingangsnummer");

        verify(medicationRepository, never()).save(any());
    }

    @Test
    void createManualMedication_savesWhenUnique() {
        when(medicationRepository.existsActiveByEingangsnummerIgnoreCase("999")).thenReturn(false);
        when(medicationRepository.save(any(Medication.class))).thenAnswer(invocation -> {
            Medication m = invocation.getArgument(0);
            if (m.getId() == null) {
                m.setId(42L);
            }
            return m;
        });

        Medication draft = new Medication()
                .setArzneimittelbezeichnung("Lux")
                .setWirkstoffe("Aflibercept")
                .setEingangsnummer("999");

        Medication saved = service.createManualMedication(draft);

        assertThat(saved.getId()).isEqualTo(42L);
        assertThat(saved.getValidUntil()).isNull();
        verify(medicationRepository).save(any(Medication.class));
    }

    @Test
    void createManualMedication_withoutIds_checksBezeichnungWirkstoff() {
        when(medicationRepository.existsActiveByBezeichnungAndWirkstoffIgnoreCase("X", "Y")).thenReturn(true);

        Medication draft = new Medication()
                .setArzneimittelbezeichnung("X")
                .setWirkstoffe("Y");

        assertThatThrownBy(() -> service.createManualMedication(draft))
                .isInstanceOf(MedicationDuplicateException.class)
                .hasMessageContaining("Wirkstoffen");
    }

    @Test
    void findById_delegatesToRepository() {
        Medication m = new Medication().setArzneimittelbezeichnung("A");
        when(medicationRepository.findById(1L)).thenReturn(Optional.of(m));

        assertThat(service.findById(1L)).contains(m);
    }
}
