package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.dto.SideOfEye;
import de.bbajor.pvs.base.service.PatientService;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.service.IvomDiagnosisService;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

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
    @Autowired
    private IntravitrealMedicationService medicationService;

    private TreatmentPlanDto workingCopy;

    public void loadTreatmentPlanById(Long id) {
        this.workingCopy = id != null ? treatmentPlanService.loadTreatmentPlanDto(id) : new TreatmentPlanDto();
    }

    public void saveChanges() {
        treatmentPlanService.saveTreatmentPlan(workingCopy);
    }

    public TreatmentPlanDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new TreatmentPlanDto();
        }
        return workingCopy;
    }

    public List<PatientDto> getPatients() {
        return patientService.getAll();
    }

    public List<IntravitrealMedicationDto> getDrugs() {
        return medicationService.getMedicationListFavorites();
    }

    public List<SurgicalCenterDto> getSurgicalCenters() {
        return surgicalCenterService.getSurgicalCenters();
    }

    public DiagnosisDto saveDiagnosis(DiagnosisDto newDto) {
        return ivomDiagnosisService.save(newDto);
    }

    public List<SurgicalCenterTimeSlotDto> loadAvailableSurgicalCenterTimeSlots(
            SurgicalCenterDto selectedSurgicalCenter) {
        List<SurgicalCenterDto> surgicalCenterDtos = new ArrayList<>();
        if (selectedSurgicalCenter == null) { // if no specific surgeryunit has been selected, choose all
            surgicalCenterDtos.addAll(surgicalCenterService.getSurgicalCenters());
        } else {
            surgicalCenterDtos.add(selectedSurgicalCenter);
        }
        List<SurgicalCenterTimeSlotDto> resultList = new ArrayList<>();
        for (SurgicalCenterDto surgicalCenter : surgicalCenterDtos) {
            resultList.addAll(surgicalCenter.getAvailableTimeSlots());
        }
        return resultList;
    }

    public Collection<SurgicalCenterTimeSlotDto> getAllTimeSlotsFilteredBy(LocalDate start, TimePeriod period,
            TimeSlotRepetition repetition,
            Integer surgicalCenterId) {
        Collection<SurgicalCenterTimeSlotDto> surgeryUnitTimeSlots = surgicalCenterService
                .findAvailableTimeSlotsFilteredBy(start, period, surgicalCenterId);

        List<SurgicalCenterTimeSlotDto> fullyFiltered = new ArrayList<>();

        var end = period.calculateEndDate(start);
        var repeatEveryWeeks = repetition.getRepeatEveryWeeks();

        for (SurgicalCenterTimeSlotDto slot : surgeryUnitTimeSlots) {
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

    public TreatmentPlanDto saveNewTreatments(List<TreatmentDto> treatmentDtos) {
        saveChanges();
        List<TreatmentDto> savedTreatments = treatmentPlanService.saveTreatments(treatmentDtos);
        workingCopy.getTreatments().addAll(savedTreatments);
        return workingCopy;
    }

    public TreatmentPlanDto getById(Long id) {
        TreatmentPlanDto treatmentPlan = treatmentPlanService.getTreatmentPlanById(id);
        if (treatmentPlan == null) {
            return new TreatmentPlanDto();
        }
        return treatmentPlan;
    }

    public PatientDto getCurrentPatient() {
        return getWorkingCopy().getPatient();
    }

    public List<TreatmentDto> getTreatmentDtosOriginal(SideOfEye sideOfEye) {
        List<TreatmentDto> treatmentSlots = treatmentPlanService
                .getTreatmentSlotsByTreatmentPlanId(workingCopy.getId());
        if (sideOfEye != null) {
            treatmentSlots.removeIf(e -> !sideOfEye.asDbString().equals(e.getSideOfEye()));
        }
        treatmentSlots.sort((o1, o2) -> {
            return o1.getSurgicalCenterTimeSlot().getDate().isAfter(o2.getSurgicalCenterTimeSlot().getDate()) ? 1 : -1;
        });
        return treatmentSlots;
    }

    public List<TreatmentDto> getTreatmentDtosWorkingCopy(SideOfEye sideOfEye) {
        return workingCopy.getTreatments();
    }

    public void setWorkingCopy(TreatmentPlanDto workingCopy) {
        this.workingCopy = workingCopy;
    }

    public Collection<DiagnosisDto> getResaonsForTreatment() {
        return ivomDiagnosisService.getDiagnosisDtos();
    }
}
