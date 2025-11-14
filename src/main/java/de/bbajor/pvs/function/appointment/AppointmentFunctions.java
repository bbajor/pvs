package de.bbajor.pvs.function.appointment;

import de.bbajor.pvs.appointment.model.Appointment;
import de.bbajor.pvs.appointment.model.AppointmentScheduler;
import de.bbajor.pvs.appointment.service.AppointmentService;
import de.bbajor.pvs.common.function.FunctionRequest;
import de.bbajor.pvs.common.function.FunctionResponse;
import de.bbajor.pvs.function.core.FunctionWrapper;
import de.bbajor.pvs.patient.model.Patient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Spring Cloud Functions for Appointment Service.
 */
@Configuration
@RequiredArgsConstructor
public class AppointmentFunctions {
    
    private final AppointmentService appointmentService;
    
    @Bean
    public Function<ScheduleAppointmentRequest, AppointmentResponse> scheduleAppointment() {
        return FunctionWrapper.wrap(
            request -> {
                Appointment appointment = request.getAppointment();
                Appointment saved = appointmentService.save(appointment);
                
                AppointmentResponse response = new AppointmentResponse();
                response.setAppointment(saved);
                return response;
            },
            "scheduleAppointment"
        );
    }
    
    @Bean
    public Function<CancelAppointmentRequest, AppointmentResponse> cancelAppointment() {
        return FunctionWrapper.wrap(
            request -> {
                Optional<Appointment> appointmentOpt = appointmentService.findById(request.getAppointmentId());
                if (appointmentOpt.isEmpty()) {
                    AppointmentResponse response = new AppointmentResponse();
                    response.setErrorMessage("Appointment not found: " + request.getAppointmentId());
                    response.setSuccess(false);
                    return response;
                }
                
                Appointment appointment = appointmentOpt.get();
                appointmentService.delete(appointment);
                
                AppointmentResponse response = new AppointmentResponse();
                response.setAppointment(appointment);
                return response;
            },
            "cancelAppointment"
        );
    }
    
    @Bean
    public Function<GetAppointmentsRequest, AppointmentListResponse> getAppointments() {
        return FunctionWrapper.wrap(
            request -> {
                List<Appointment> appointments;
                
                if (request.getSchedulerId() != null) {
                    AppointmentScheduler scheduler = new AppointmentScheduler();
                    scheduler.setId(request.getSchedulerId());
                    appointments = appointmentService.findByScheduler(scheduler);
                } else if (request.getPatientId() != null) {
                    Patient patient = new Patient();
                    patient.setId(request.getPatientId());
                    appointments = appointmentService.findByPatient(patient);
                } else {
                    appointments = appointmentService.findAll();
                }
                
                AppointmentListResponse response = new AppointmentListResponse();
                response.setAppointments(appointments);
                return response;
            },
            "getAppointments"
        );
    }
    
    @Bean
    public Function<FindNextAvailableSlotRequest, FindNextAvailableSlotResponse> findNextAvailableSlot() {
        return FunctionWrapper.wrap(
            request -> {
                AppointmentScheduler scheduler = new AppointmentScheduler();
                scheduler.setId(request.getSchedulerId());
                
                Optional<LocalDateTime> slot = appointmentService.findNextAvailableSlot(
                    scheduler,
                    request.getFrom(),
                    request.getDurationMinutes()
                );
                
                FindNextAvailableSlotResponse response = new FindNextAvailableSlotResponse();
                slot.ifPresent(response::setSlot);
                if (slot.isEmpty()) {
                    response.setErrorMessage("No available slot found");
                    response.setSuccess(false);
                }
                return response;
            },
            "findNextAvailableSlot"
        );
    }
    
    // Request/Response classes
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class ScheduleAppointmentRequest extends FunctionRequest {
        private Appointment appointment;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class CancelAppointmentRequest extends FunctionRequest {
        private Long appointmentId;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class GetAppointmentsRequest extends FunctionRequest {
        private Long schedulerId;
        private Integer patientId;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class FindNextAvailableSlotRequest extends FunctionRequest {
        private Long schedulerId;
        private LocalDateTime from;
        private int durationMinutes;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AppointmentResponse extends FunctionResponse {
        private Appointment appointment;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class AppointmentListResponse extends FunctionResponse {
        private List<Appointment> appointments;
    }
    
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class FindNextAvailableSlotResponse extends FunctionResponse {
        private LocalDateTime slot;
    }
}

