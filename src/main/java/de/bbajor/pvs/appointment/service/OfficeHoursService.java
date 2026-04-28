package de.bbajor.pvs.appointment.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.model.OfficeHours;
import de.bbajor.pvs.appointment.repository.AppointmentRepository;
import de.bbajor.pvs.appointment.repository.OfficeHoursRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing office hours with business logic and validations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OfficeHoursService {

    private final OfficeHoursRepository officeHoursRepository;
    private final AppointmentRepository appointmentRepository;

    /**
     * Find all office hours for a scheduler.
     */
    public List<OfficeHours> findByScheduler(AppointmentScheduler scheduler) {
        return officeHoursRepository.findByScheduler(scheduler);
    }

    /**
     * Find active office hours for a scheduler.
     */
    public List<OfficeHours> findActiveByScheduler(AppointmentScheduler scheduler) {
        return officeHoursRepository.findBySchedulerAndActiveTrue(scheduler);
    }

    /**
     * Find office hours for a specific day of week.
     */
    public List<OfficeHours> findBySchedulerAndDayOfWeek(
            AppointmentScheduler scheduler, 
            DayOfWeek dayOfWeek) {
        return officeHoursRepository.findBySchedulerAndDayOfWeekAndActiveTrue(scheduler, dayOfWeek);
    }

    /**
     * Find office hours for a specific date.
     */
    public List<OfficeHours> findBySchedulerAndDate(AppointmentScheduler scheduler, LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return findBySchedulerAndDayOfWeek(scheduler, dayOfWeek);
    }

    /**
     * Find office hours by ID.
     */
    public Optional<OfficeHours> findById(Long id) {
        return officeHoursRepository.findById(id);
    }

    /**
     * Save office hours with validation.
     * 
     * @throws IllegalStateException if there are existing appointments during these hours
     */
    public OfficeHours save(OfficeHours officeHours) {
        validateOfficeHours(officeHours);
        return officeHoursRepository.save(officeHours);
    }

    /**
     * Delete office hours.
     * Only allowed if no appointments are scheduled during these hours.
     * 
     * @throws IllegalStateException if appointments exist during these hours
     */
    public void delete(OfficeHours officeHours) {
        validateNoAppointmentsExist(officeHours);
        officeHoursRepository.delete(officeHours);
    }

    /**
     * Validate office hours business rules.
     */
    private void validateOfficeHours(OfficeHours officeHours) {
        // Rule 1: Start time must be before end time
        if (officeHours.getStartTime().isAfter(officeHours.getEndTime())) {
            throw new IllegalArgumentException("Startzeit muss vor Endzeit liegen");
        }

        // Rule 2: Slot duration must be reasonable (5-120 minutes)
        if (officeHours.getSlotDurationMinutes() < 5 || officeHours.getSlotDurationMinutes() > 120) {
            throw new IllegalArgumentException("Slot-Dauer muss zwischen 5 und 120 Minuten liegen");
        }

        // Rule 3: Cannot modify office hours if appointments exist (for existing hours)
        if (officeHours.getId() != null) {
            Optional<OfficeHours> existing = officeHoursRepository.findById(officeHours.getId());
            if (existing.isPresent() && hasAppointments(existing.get())) {
                throw new IllegalStateException(
                    "Sprechzeiten können nicht geändert werden, wenn bereits Patienten zugeordnet sind"
                );
            }
        }
    }

    /**
     * Validate that no appointments exist during these office hours.
     */
    private void validateNoAppointmentsExist(OfficeHours officeHours) {
        if (hasAppointments(officeHours)) {
            throw new IllegalStateException(
                "Sprechzeiten können nicht gelöscht werden, wenn bereits Patienten zugeordnet sind"
            );
        }
    }

    /**
     * Check if any appointments exist during these office hours.
     */
    private boolean hasAppointments(OfficeHours officeHours) {
        // Get all appointments for the scheduler
        List<Appointment> appointments = appointmentRepository.findByScheduler(officeHours.getScheduler());

        // Filter appointments that fall on the same day of week and within the time range
        return appointments.stream()
            .filter(apt -> apt.getStartTime().getDayOfWeek() == officeHours.getDayOfWeek())
            .filter(apt -> {
                var aptTime = apt.getStartTime().toLocalTime();
                return !aptTime.isBefore(officeHours.getStartTime()) 
                    && !aptTime.isAfter(officeHours.getEndTime());
            })
            .findAny()
            .isPresent();
    }

    /**
     * Deactivate office hours instead of deleting them.
     */
    public void deactivate(OfficeHours officeHours) {
        officeHours.setActive(false);
        officeHoursRepository.save(officeHours);
    }

    /**
     * Find all office hours.
     */
    public List<OfficeHours> findAll() {
        return officeHoursRepository.findAll();
    }
}
