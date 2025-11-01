package de.bbajor.pvs.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.AppointmentStatus;
import de.bbajor.pvs.appointment.repository.AppointmentRepository;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.practice.model.Practice;
import de.bbajor.pvs.tenant.model.Tenant;
import de.bbajor.pvs.tenant.service.TenantAccessValidator;

/**
 * Unit tests for AppointmentService business logic and validations.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private OfficeHoursService officeHoursService;

    @Mock
    private TenantAccessValidator tenantAccessValidator;

    @InjectMocks
    private AppointmentService appointmentService;

    private AppointmentScheduler scheduler;
    private Patient patient;
    private Appointment appointment;
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

        patient = new Patient();
        patient.setId(1);
        patient.setFirstName("Max");
        patient.setLastName("Mustermann");
        patient.setTenant(tenant);

        appointment = new Appointment();
        appointment.setScheduler(scheduler);
        appointment.setPatient(patient);
        appointment.setTenant(tenant);
        appointment.setReason("Kontrolluntersuchung");
        appointment.setStatus(AppointmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("Should save valid appointment")
    void shouldSaveValidAppointment() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(30);
        appointment.setStartTime(start);
        appointment.setEndTime(end);

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any()))
            .thenReturn(Collections.emptyList());
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(officeHoursService.findBySchedulerAndDate(any(), any()))
            .thenReturn(Collections.emptyList());

        // Act
        Appointment saved = appointmentService.save(appointment);

        // Assert
        assertNotNull(saved);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("Should reject appointment with start time after end time")
    void shouldRejectAppointmentWithInvalidTimeRange() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.minusMinutes(30); // End before start
        appointment.setStartTime(start);
        appointment.setEndTime(end);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.save(appointment);
        });

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject overlapping appointments")
    void shouldRejectOverlappingAppointments() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(30);
        appointment.setStartTime(start);
        appointment.setEndTime(end);

        Appointment existingAppointment = new Appointment();
        existingAppointment.setId(999L);
        existingAppointment.setStartTime(start);
        existingAppointment.setEndTime(end);

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any()))
            .thenReturn(List.of(existingAppointment));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            appointmentService.save(appointment);
        });

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should not reject appointment when updating itself")
    void shouldNotRejectAppointmentWhenUpdatingItself() {
        // Arrange
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusMinutes(30);
        appointment.setId(1L);
        appointment.setStartTime(start);
        appointment.setEndTime(end);

        when(appointmentRepository.findOverlappingAppointments(any(), any(), any()))
            .thenReturn(List.of(appointment)); // Returns itself
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(officeHoursService.findBySchedulerAndDate(any(), any()))
            .thenReturn(Collections.emptyList());

        // Act
        Appointment saved = appointmentService.save(appointment);

        // Assert
        assertNotNull(saved);
        verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("Should reject modification of past appointments")
    void shouldRejectModificationOfPastAppointments() {
        // Arrange
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        LocalDateTime pastEnd = pastStart.plusMinutes(30);
        appointment.setId(1L);
        appointment.setStartTime(pastStart);
        appointment.setEndTime(pastEnd);

        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.save(appointment);
        });

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should reject deletion of past appointments")
    void shouldRejectDeletionOfPastAppointments() {
        // Arrange
        LocalDateTime pastStart = LocalDateTime.now().minusDays(1);
        appointment.setStartTime(pastStart);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            appointmentService.delete(appointment);
        });

        verify(appointmentRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Should allow deletion of future appointments")
    void shouldAllowDeletionOfFutureAppointments() {
        // Arrange
        LocalDateTime futureStart = LocalDateTime.now().plusDays(1);
        appointment.setStartTime(futureStart);

        // Act
        appointmentService.delete(appointment);

        // Assert
        verify(appointmentRepository).delete(appointment);
    }

    @Test
    @DisplayName("Should find appointments by scheduler")
    void shouldFindAppointmentsByScheduler() {
        // Arrange
        when(appointmentRepository.findByScheduler(scheduler))
            .thenReturn(List.of(appointment));

        // Act
        List<Appointment> found = appointmentService.findByScheduler(scheduler);

        // Assert
        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(appointment, found.get(0));
    }

    @Test
    @DisplayName("Should find appointments by patient")
    void shouldFindAppointmentsByPatient() {
        // Arrange
        when(appointmentRepository.findByPatient(patient))
            .thenReturn(List.of(appointment));

        // Act
        List<Appointment> found = appointmentService.findByPatient(patient);

        // Assert
        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(appointment, found.get(0));
    }
}
