package de.bbajor.pvs.appointment.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.repository.AppointmentSchedulerRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service for global appointment operations across all schedulers.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GlobalAppointmentService {

    private final AppointmentSchedulerRepository schedulerRepository;
    private final AppointmentService appointmentService;

    /**
     * Finds the next available appointment slot across ALL active schedulers.
     * Returns the earliest available slot with the scheduler information.
     * 
     * @param from Start time to search from
     * @param durationMinutes Duration of appointment in minutes
     * @return Optional containing the next available slot info with scheduler and time
     */
    public Optional<NextAvailableSlot> findNextAvailableSlotGlobally(
            LocalDateTime from, 
            int durationMinutes) {
        
        List<AppointmentScheduler> activeSchedulers = schedulerRepository.findAll().stream()
            .filter(AppointmentScheduler::isActive)
            .toList();

        return activeSchedulers.stream()
            .map(scheduler -> {
                Optional<LocalDateTime> slotTime = appointmentService.findNextAvailableSlot(
                    scheduler, from, durationMinutes
                );
                return slotTime.map(time -> new NextAvailableSlot(scheduler, time));
            })
            .filter(Optional::isPresent)
            .map(Optional::get)
            .min(Comparator.comparing(NextAvailableSlot::time));
    }

    /**
     * Record containing the next available appointment slot information.
     * 
     * @param scheduler The scheduler where the slot is available
     * @param time The start time of the available slot
     */
    public record NextAvailableSlot(AppointmentScheduler scheduler, LocalDateTime time) {}
}
