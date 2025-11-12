package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;
import jakarta.transaction.Transactional;

@Component
public class TreatmentPlanPresenter {

    @Autowired
    private IvomDiagnosisService ivomDiagnosisService;
    @Autowired
    private TreatmentPlanService treatmentPlanService;
    @Autowired
    private SurgicalCenterService surgicalCenterService;
    @Autowired
    private PatientService patientService;
    public TreatmentPlan loadTreatmentPlanByIdWithFullDetails(Long id) throws NoSuchElementException {
        return treatmentPlanService.loadTreatmentPlanWithFullDetails(id);
    }

    @Transactional
    public TreatmentPlan saveTreatmentPlanAndTreatments(TreatmentPlan treatmentPlan, List<Treatment> newTreatments) {
        TreatmentPlan saved = treatmentPlanService.saveTreatmentPlan(treatmentPlan);
        return saveNewTreatments(saved.getId(), newTreatments);
    }

    public List<Patient> getPatients() {
        return patientService.getAll();
    }

    public List<MedicationFavourite> getDrugs() {
        return treatmentPlanService.getFavouriteMedications();
    }

    public List<MedicationFavourite> getDrugsForInstitution(Long institutionId) {
        return treatmentPlanService.getFavouriteMedicationsForInstitution(institutionId);
    }

    public List<SurgicalCenter> getSurgicalCenters() {
        return surgicalCenterService.getSurgicalCenters();
    }

    public List<SurgicalCenter> getSurgicalCentersForInstitution(Long institutionId) {
        return surgicalCenterService.getSurgicalCentersForInstitution(institutionId);
    }

    public Diagnosis saveDiagnosis(Diagnosis newDto) {
        return ivomDiagnosisService.save(newDto);
    }

    public List<SurgicalCenterTimeSlot> loadAvailableSurgicalCenterTimeSlots(
            SurgicalCenter selectedSurgicalCenter) {
        return surgicalCenterService.findByIdWithDetails(selectedSurgicalCenter.getId()).getAvailableTimeSlots();
    }

    public Collection<SurgicalCenterTimeSlot> getAllTimeSlotsFilteredBy(LocalDate start, TimePeriod period,
            TimeSlotRepetition repetition,
            Integer surgicalCenterId) {
        Collection<SurgicalCenterTimeSlot> surgicalCenterTimeSlots = surgicalCenterService
                .findAvailableTimeSlotsFilteredBy(start, period, surgicalCenterId);

        List<SurgicalCenterTimeSlot> fullyFiltered = new ArrayList<>();

        var end = period.calculateEndDate(start);
        var repeatEveryWeeks = repetition.getRepeatEveryWeeks();

        for (SurgicalCenterTimeSlot slot : surgicalCenterTimeSlots) {
            LocalDate slotDate = slot.getDate();

            // nur Slots innerhalb des Zeitraums beachten
            if (!slotDate.isBefore(start) && !slotDate.isAfter(end)) {

                // Abstands-Berechnung in Wochen (inkl. Jahrwechsel)
                long weeksBetween = ChronoUnit.WEEKS.between(start, slotDate);

                // nur Slots im Wiederholungsrhythmus aufnehmen
                if (weeksBetween % repeatEveryWeeks == 0) {
                    fullyFiltered.add(slot);
                }
            }
        }

        return fullyFiltered;
    }

    @Transactional
    private TreatmentPlan saveNewTreatments(Long treatmentPlanId, List<Treatment> treatments) {
        List<Treatment> savedTreatments = treatmentPlanService.saveNewTreatmentsForExistingPlan(treatments,
                treatmentPlanId);
        return treatmentPlanService.findByIdWithDetails(treatmentPlanId);
    }

    public TreatmentPlan getByIdWithFullDetails(Long id) {
        TreatmentPlan treatmentPlan = treatmentPlanService.findByIdWithDetails(id);
        if (treatmentPlan == null) {
            return new TreatmentPlan();
        }
        return treatmentPlan;
    }

    public List<Treatment> getTreatmentDtos(SideOfEye sideOfEye, Long treatmentPlanId) {
        if (treatmentPlanId == null || treatmentPlanId == -1) {
            return new ArrayList<>();
        }
        List<Treatment> treatmentSlots = treatmentPlanService.getTreatmentSlots(treatmentPlanId);
        if (sideOfEye != null) {
            treatmentSlots.removeIf(e -> !sideOfEye.equals(e.getSideOfEye()));
        }
        return treatmentSlots;
    }

    public Collection<Diagnosis> getResaonsForTreatment() {
        return ivomDiagnosisService.getDiagnoses();
    }

    public TreatmentPlan save(Long ivomPlanId, List<Treatment> timeSlotsToCreate) {
        return saveNewTreatments(ivomPlanId, timeSlotsToCreate);
    }

    public Collection<MedicationFavourite> getFavouriteMedications() {
        return treatmentPlanService.getFavouriteMedications();
    }
}
