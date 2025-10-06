package de.bbajor.pvs.init;

import static org.instancio.Select.all;
import static org.instancio.Select.field;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.instancio.Instancio;
import org.instancio.settings.Settings;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterAddress;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class TestDataInitializer implements CommandLineRunner {

        private final PatientService patientService;
        private final IntravitrealMedicationService medicationService;
        private final SurgicalCenterService surgicalCenterService;

        @Override
        public void run(String... args) {
                Settings settings = Settings.create()
                                .lock();

                List<Patient> patients = Instancio.ofList(Patient.class)
                                .size(20)
                                .withSettings(settings)
                                .ignore(field(Patient::getId))
                                .ignore(field(Patient::getVersion))
                                .supply(field(Patient::getEmail), () -> "user" + UUID.randomUUID() + "@example.com")
                                .ignore(field(Address::getId))
                                .ignore(field(Address::getVersion))
                                .ignore(field(HealthInsurance::getId))
                                .ignore(field(HealthInsurance::getVersion))
                                .ignore(field(Patient::getPatientHistory))
                                .create();
                patientService.saveAll(patients);
                List<Medication> medications = Instancio.ofList(Medication.class).size(10).ignore(all(
                                field(Medication::getId),
                                field(Medication::getVersion))).create();
                medicationService.saveAll(medications);

                SurgicalCenter surgicalCenter = Instancio.of(SurgicalCenter.class)
                                .ignore(field(SurgicalCenter::getId))
                                .ignore(field(SurgicalCenter::getVersion))
                                .ignore(field(SurgicalCenterAddress::getId))
                                .ignore(field(SurgicalCenterAddress::getVersion))
                                .ignore(field(SurgicalCenterTimeSlot::getId))
                                .ignore(field(SurgicalCenterTimeSlot::getVersion))
                                .create();
                SurgicalCenterTimeSlot timeSlot = Instancio.of(SurgicalCenterTimeSlot.class)
                                .ignore(field(SurgicalCenterTimeSlot::getId))
                                .ignore(field(SurgicalCenterTimeSlot::getVersion))
                                .create();
                timeSlot.setDate(LocalDate.now().plusDays(5));
                timeSlot.setStartTime(LocalTime.of(7, 0));
                timeSlot.setEndTime(LocalTime.of(9, 0));
                timeSlot.setSurgicalCenter(surgicalCenter);
                surgicalCenter.setAvailableTimeSlots(List.of(timeSlot));
                surgicalCenterService.saveTimeSlotsAndSurgicalCenter(surgicalCenter.getAvailableTimeSlots(), surgicalCenter);
        }
}
