package de.bbajor.pvs.appointment.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.repository.AppointmentRepository;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.tenant.context.TenantContext;
import de.bbajor.pvs.tenant.service.TenantAccessValidator;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing appointments with business logic and validations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final OfficeHoursService officeHoursService;
    private final TenantAccessValidator tenantAccessValidator;

    /**
     * Find all appointments for a scheduler.
     * Ensures tenant isolation.
     */
    public List<Appointment> findByScheduler(AppointmentScheduler scheduler) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            tenantAccessValidator.validateTenantAccess(scheduler.getTenant().getId(), 
                "AppointmentScheduler", scheduler.getId());
            return appointmentRepository.findBySchedulerAndTenantId(scheduler, tenantId);
        }
        return appointmentRepository.findByScheduler(scheduler);
    }

    /**
     * Find appointments for a scheduler within a date range.
     */
    public List<Appointment> findBySchedulerAndDateRange(
            AppointmentScheduler scheduler,
            LocalDateTime start,
            LocalDateTime end) {
        return appointmentRepository.findBySchedulerAndStartTimeBetween(scheduler, start, end);
    }

    /**
     * Find all appointments for a patient.
     */
    public List<Appointment> findByPatient(Patient patient) {
        return appointmentRepository.findByPatient(patient);
    }

    /**
     * Find appointment by ID.
     * Ensures tenant isolation.
     */
    public Optional<Appointment> findById(Long id) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            Optional<Appointment> appointment = appointmentRepository.findByIdAndTenantId(id, tenantId);
            appointment.ifPresent(a -> 
                tenantAccessValidator.validateTenantAccess(a.getTenant().getId(), "Appointment", a.getId()));
            return appointment;
        }
        return appointmentRepository.findById(id);
    }

    /**
     * Save an appointment with validations.
     * Ensures tenant consistency.
     * 
     * @throws IllegalStateException if appointment is in the past and being modified
     * @throws IllegalArgumentException if appointment overlaps with existing appointments
     */
    public Appointment save(Appointment appointment) {
        validateAppointment(appointment);
        validateAndSetTenant(appointment);
        return appointmentRepository.save(appointment);
    }

    /**
     * Delete an appointment.
     * Only allowed for future appointments.
     * 
     * @throws IllegalStateException if appointment is in the past
     */
    public void delete(Appointment appointment) {
        if (appointment.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Vergangene Termine können nicht gelöscht werden");
        }
        appointmentRepository.delete(appointment);
    }

    /**
     * Validate and set tenant for appointment.
     * Ensures tenant consistency across scheduler, patient, and appointment.
     */
    private void validateAndSetTenant(Appointment appointment) {
        Long tenantId = TenantContext.getTenantId();
        
        if (tenantId != null) {
            // Validate scheduler belongs to current tenant
            if (appointment.getScheduler() != null) {
                tenantAccessValidator.validateTenantAccess(
                    appointment.getScheduler().getTenant().getId(),
                    "AppointmentScheduler",
                    appointment.getScheduler().getId());
            }
            
            // Validate patient belongs to current tenant
            if (appointment.getPatient() != null) {
                tenantAccessValidator.validateTenantAccess(
                    appointment.getPatient().getTenant().getId(),
                    "Patient",
                    appointment.getPatient().getId());
            }
            
            // Set tenant from context if not set
            if (appointment.getTenant() == null) {
                appointment.setTenant(appointment.getScheduler().getTenant());
            }
            
            // Validate tenant consistency
            if (!appointment.getTenant().getId().equals(tenantId)) {
                throw new IllegalArgumentException(
                    "Termin gehört nicht zum aktuellen Mandanten");
            }
        }
    }

    /**
     * Validate appointment business rules.
     */
    private void validateAppointment(Appointment appointment) {
        // Rule 1: Cannot modify past appointments
        if (appointment.getId() != null) {
            Optional<Appointment> existing = appointmentRepository.findById(appointment.getId());
            if (existing.isPresent() && existing.get().getStartTime().isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("Vergangene Termine können nicht geändert werden");
            }
        }

        // Rule 2: Start time must be before end time
        if (appointment.getStartTime().isAfter(appointment.getEndTime())) {
            throw new IllegalArgumentException("Startzeit muss vor Endzeit liegen");
        }

        // Rule 3: Check for overlapping appointments
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
            appointment.getScheduler(),
            appointment.getStartTime(),
            appointment.getEndTime()
        );

        // Filter out the appointment being updated
        overlapping = overlapping.stream()
            .filter(a -> !a.getId().equals(appointment.getId()))
            .toList();

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException(
                String.format("Termin überschneidet sich mit bestehendem Termin: %s", 
                    overlapping.get(0).toString())
            );
        }

        // Rule 4: Appointment must be within office hours (warning, not error)
        // This is checked but doesn't block saving
        checkOfficeHours(appointment);
    }

    /**
     * Check if appointment is within office hours.
     * This is a soft check - doesn't throw exception but logs warning.
     */
    private void checkOfficeHours(Appointment appointment) {
        LocalDate date = appointment.getStartTime().toLocalDate();
        LocalTime time = appointment.getStartTime().toLocalTime();
        
        List<OfficeHours> officeHours = officeHoursService.findBySchedulerAndDate(
            appointment.getScheduler(), 
            date
        );

        boolean withinHours = officeHours.stream()
            .anyMatch(oh -> !time.isBefore(oh.getStartTime()) && !time.isAfter(oh.getEndTime()));

        if (!withinHours && !officeHours.isEmpty()) {
            // Log warning but don't block
            System.out.println("WARNUNG: Termin liegt außerhalb der regulären Sprechzeiten");
        }
    }

    /**
     * Find the next available appointment slot.
     */
    public Optional<LocalDateTime> findNextAvailableSlot(
            AppointmentScheduler scheduler,
            LocalDateTime from,
            int durationMinutes) {
        
        LocalDate currentDate = from.toLocalDate();
        LocalDate endDate = currentDate.plusWeeks(4); // Search up to 4 weeks ahead

        while (currentDate.isBefore(endDate)) {
            List<OfficeHours> dailyHours = officeHoursService.findBySchedulerAndDate(scheduler, currentDate);
            
            for (OfficeHours hours : dailyHours) {
                LocalDateTime slotStart = LocalDateTime.of(currentDate, hours.getStartTime());
                LocalDateTime slotEnd = LocalDateTime.of(currentDate, hours.getEndTime());

                while (slotStart.plusMinutes(durationMinutes).isBefore(slotEnd) 
                       || slotStart.plusMinutes(durationMinutes).equals(slotEnd)) {
                    
                    if (slotStart.isAfter(from)) {
                        LocalDateTime proposedEnd = slotStart.plusMinutes(durationMinutes);
                        
                        List<Appointment> conflicts = appointmentRepository.findOverlappingAppointments(
                            scheduler, slotStart, proposedEnd
                        );

                        if (conflicts.isEmpty()) {
                            return Optional.of(slotStart);
                        }
                    }

                    slotStart = slotStart.plusMinutes(hours.getSlotDurationMinutes());
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return Optional.empty();
    }

    /**
     * Get all appointments for today.
     */
    public List<Appointment> findTodayAppointments(AppointmentScheduler scheduler) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusSeconds(1);
        return findBySchedulerAndDateRange(scheduler, startOfDay, endOfDay);
    }

    /**
     * Get upcoming appointments for a scheduler.
     */
    public List<Appointment> findUpcomingAppointments(AppointmentScheduler scheduler) {
        return appointmentRepository.findUpcomingAppointments(scheduler, LocalDateTime.now());
    }

    /**
     * Find all appointments.
     */
    public List<Appointment> findAll() {
        return appointmentRepository.findAll();
    }
}
