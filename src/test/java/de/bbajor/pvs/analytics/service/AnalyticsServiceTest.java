package de.bbajor.pvs.analytics.service;

import de.bbajor.pvs.analytics.dto.AgeGroupStatistics;
import de.bbajor.pvs.analytics.dto.AnalyticsData;
import de.bbajor.pvs.analytics.dto.InsuranceStatistics;
import de.bbajor.pvs.analytics.dto.MedicationStatistics;
import de.bbajor.pvs.analytics.dto.TreatmentStatistics;
import de.bbajor.pvs.analytics.repository.AnalyticsRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.model.Patient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Unit-Tests für AnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AnalyticsRepository analyticsRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private static final Long TEST_INSTITUTION_ID = 1L;

    @BeforeEach
    void setUp() {
        InstitutionContext.setInstitutionId(TEST_INSTITUTION_ID);
    }

    @AfterEach
    void tearDown() {
        InstitutionContext.clear();
    }

    @Test
    void getAllAnalyticsData_shouldReturnAnalyticsData() {
        // Given
        when(analyticsRepository.findAllTreatmentsWithTimeSlot(anyLong()))
            .thenReturn(Collections.emptyList());
        when(analyticsRepository.findAllTreatmentsWithMedication(anyLong()))
            .thenReturn(Collections.emptyList());
        when(analyticsRepository.findAllPatientsByInstitution(anyLong()))
            .thenReturn(Collections.emptyList());

        // When
        var result = analyticsService.getAllAnalyticsData();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.treatmentStatistics()).isNotNull();
        assertThat(result.ageGroupStatistics()).isNotNull();
        assertThat(result.insuranceStatistics()).isNotNull();
        assertThat(result.medicationStatistics()).isNotNull();
    }

    @Test
    void getAllAnalyticsData_withoutInstitutionContext_shouldThrowException() {
        // Given
        InstitutionContext.clear();

        // When/Then
        assertThatThrownBy(() -> analyticsService.getAllAnalyticsData())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("institution context");
    }

    @Test
    void getAgeGroupStatistics_withPatients_shouldCalculateAgeGroups() {
        // Given
        Patient patient1 = new Patient();
        patient1.setBirth(LocalDate.now().minusYears(25));
        
        Patient patient2 = new Patient();
        patient2.setBirth(LocalDate.now().minusYears(5));
        
        Patient patient3 = new Patient();
        patient3.setBirth(LocalDate.now().minusYears(60));

        when(analyticsRepository.findAllPatientsByInstitution(anyLong()))
            .thenReturn(List.of(patient1, patient2, patient3));

        // When
        AgeGroupStatistics result = analyticsService.getAgeGroupStatistics(TEST_INSTITUTION_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ageGroups()).isNotEmpty();
        assertThat(result.ageGroups()).containsKey("19-30"); // patient1
        assertThat(result.ageGroups()).containsKey("4-12"); // patient2
        assertThat(result.ageGroups()).containsKey("51-65"); // patient3
    }

    @Test
    void getAgeGroupStatistics_withEmptyData_shouldReturnEmptyMap() {
        // Given
        when(analyticsRepository.findAllPatientsByInstitution(anyLong()))
            .thenReturn(Collections.emptyList());

        // When
        AgeGroupStatistics result = analyticsService.getAgeGroupStatistics(TEST_INSTITUTION_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.ageGroups()).isEmpty();
    }

    @Test
    void getInsuranceStatistics_withPatients_shouldGroupByType() {
        // Given
        Patient patient1 = new Patient();
        patient1.setIsPrivateInsurance(true);
        
        Patient patient2 = new Patient();
        patient2.setIsPrivateInsurance(false);

        when(analyticsRepository.findAllPatientsByInstitution(anyLong()))
            .thenReturn(List.of(patient1, patient2));

        // When
        InsuranceStatistics result = analyticsService.getInsuranceStatistics(TEST_INSTITUTION_ID);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.byType()).containsKey("Privat");
        assertThat(result.byType()).containsKey("Kasse");
        assertThat(result.byType().get("Privat")).isEqualTo(1L);
        assertThat(result.byType().get("Kasse")).isEqualTo(1L);
    }
}


