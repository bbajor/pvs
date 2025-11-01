package de.bbajor.pvs.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.repository.AppointmentRepository;
import de.bbajor.pvs.appointment.repository.OfficeHoursRepository;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.tenant.model.Tenant;

/**
 * Unit tests for OfficeHoursService business logic and validations.
 */
@ExtendWith(MockitoExtension.class)
class OfficeHoursServiceTest {

    @Mock
    private OfficeHoursRepository officeHoursRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private OfficeHoursService officeHoursService;

    private AppointmentScheduler scheduler;
    private OfficeHours officeHours;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        // Setup tenant
        tenant = new Tenant();
        tenant.setId(1L);
        tenant.setTenantCode("TEST-2024-A1B2");
        tenant.setTenantName("Test Praxis MVZ");

        Practice practice = new Practice();
        practice.setId(1L);
        practice.setPracticeName("Test Praxis");
        practice.setTenant(tenant);

        scheduler = new AppointmentScheduler();
        scheduler.setId(1L);
        scheduler.setName("Test Scheduler");
        scheduler.setPractice(practice);
        scheduler.setTenant(tenant);

        officeHours = new OfficeHours();
        officeHours.setScheduler(scheduler);
        officeHours.setDayOfWeek(DayOfWeek.MONDAY);
        officeHours.setStartTime(LocalTime.of(8, 0));
        officeHours.setEndTime(LocalTime.of(17, 0));
        officeHours.setSlotDurationMinutes(30);
        officeHours.setActive(true);
    }

    @Test
    @DisplayName("Should save valid office hours")
    void shouldSaveValidOfficeHours() {
        // Arrange
        when(officeHoursRepository.save(officeHours)).thenReturn(officeHours);

        // Act
        OfficeHours saved = officeHoursService.save(officeHours);

        // Assert
        assertNotNull(saved);
        verify(officeHoursRepository).save(officeHours);
    }

    @Test
    @DisplayName("Should reject office hours with start time after end time")
    void shouldRejectOfficeHoursWithInvalidTimeRange() {
        // Arrange
        officeHours.setStartTime(LocalTime.of(17, 0));
        officeHours.setEndTime(LocalTime.of(8, 0)); // End before start

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            officeHoursService.save(officeHours);
        });

        verify(officeHoursRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject office hours with invalid slot duration")
    void shouldRejectOfficeHoursWithInvalidSlotDuration() {
        // Arrange
        officeHours.setSlotDurationMinutes(150); // Too long (> 120 minutes)

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            officeHoursService.save(officeHours);
        });

        verify(officeHoursRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject office hours with too short slot duration")
    void shouldRejectOfficeHoursWithTooShortSlotDuration() {
        // Arrange
        officeHours.setSlotDurationMinutes(2); // Too short (< 5 minutes)

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            officeHoursService.save(officeHours);
        });

        verify(officeHoursRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should find office hours by scheduler")
    void shouldFindOfficeHoursByScheduler() {
        // Arrange
        when(officeHoursRepository.findByScheduler(scheduler))
            .thenReturn(List.of(officeHours));

        // Act
        List<OfficeHours> found = officeHoursService.findByScheduler(scheduler);

        // Assert
        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(officeHours, found.get(0));
    }

    @Test
    @DisplayName("Should find office hours by day of week")
    void shouldFindOfficeHoursByDayOfWeek() {
        // Arrange
        when(officeHoursRepository.findBySchedulerAndDayOfWeekAndActiveTrue(
            scheduler, DayOfWeek.MONDAY))
            .thenReturn(List.of(officeHours));

        // Act
        List<OfficeHours> found = officeHoursService.findBySchedulerAndDayOfWeek(
            scheduler, DayOfWeek.MONDAY);

        // Assert
        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(officeHours, found.get(0));
    }

    @Test
    @DisplayName("Should deactivate office hours instead of deleting")
    void shouldDeactivateOfficeHours() {
        // Arrange
        when(officeHoursRepository.save(officeHours)).thenReturn(officeHours);

        // Act
        officeHoursService.deactivate(officeHours);

        // Assert
        assertFalse(officeHours.isActive());
        verify(officeHoursRepository).save(officeHours);
        verify(officeHoursRepository, never()).delete(any());
    }
}
