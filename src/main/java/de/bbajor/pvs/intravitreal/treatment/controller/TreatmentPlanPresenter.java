package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.checkerframework.checker.units.qual.t;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.dto.SideOfEye;
import de.bbajor.pvs.base.dto.TimePeriod;
import de.bbajor.pvs.base.dto.TimeSlotRepetition;
import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.IntravitrealTreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentSlotDto;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentSlot;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class TreatmentPlanPresenter {

    private final ModelToDtoMapper modelToDtoMapper;
    private final TreatmentPlanService treatmentPlanService;
    private final SurgicalCenterService surgicalCenterService;
    private final PatientDialogPresenter patientDialogPresenter;

    private IntravitrealTreatmentDto workingCopy;
    private TreatmentPlan original;

    public TreatmentPlanPresenter(PatientDialogPresenter patientDialogPresenter,
            SurgicalCenterService surgicalCenterService,
            TreatmentPlanService treatmentPlanService, ModelToDtoMapper modelToDtoMapper) {
        this.patientDialogPresenter = patientDialogPresenter;
        this.surgicalCenterService = surgicalCenterService;
        this.treatmentPlanService = treatmentPlanService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public void loadTreatmentPlanById(Long id) {
        if (id != null) {
            this.original = treatmentPlanService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ivom not found: " + id));
            this.workingCopy = modelToDtoMapper.toDto(original);
        } else {
            this.original = null;
            this.workingCopy = new IntravitrealTreatmentDto(); // leere WorkingCopy für Neuanlage
        }
    }

    public void saveChanges() {
        if (workingCopy == null) {
            throw new IllegalStateException("No data loaded into dialog");
        }
        if (original == null) {
            TreatmentPlan newEntity = modelToDtoMapper.toEntity(workingCopy);
            original = treatmentPlanService.save(newEntity);
        } else {
            modelToDtoMapper.updateEntityFromDto(workingCopy, original);
            original = treatmentPlanService.save(original);
        }
    }

    public IntravitrealTreatmentDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new IntravitrealTreatmentDto();
        }
        return workingCopy;
    }

    public List<PatientDto> getPatients() {
        return patientDialogPresenter.getPatients();
    }

    public List<IntravitrealMedicationDto> getDrugs() {
        return patientDialogPresenter.getDrugs();
    }

    public List<SurgicalCenterDto> getSurgicalCenterList() {
        return patientDialogPresenter.getSurgicalCenterList();
    }

    public DiagnosisDto saveDiagnosis(DiagnosisDto newDto) {
        Diagnosis newEntity = toEntity(newDto);
        return patientDialogPresenter.save(newEntity);
    }

    private Diagnosis toEntity(DiagnosisDto dto) {
        return modelToDtoMapper.toEntity(dto);
    }

    public List<SurgicalCenterTimeSlotDto> loadAvailableSurgicalCenterTimeSlots(
            SurgicalCenterDto selectedSurgicalCenter) {
        List<SurgicalCenterDto> surgicalCenterDtos = new ArrayList<>();
        if (selectedSurgicalCenter == null) { // if no specific surgeryunit has been selected, choose all
            surgicalCenterDtos.addAll(patientDialogPresenter.getSurgicalCenterList());
        } else {
            surgicalCenterDtos.add(selectedSurgicalCenter);
        }
        List<SurgicalCenterTimeSlotDto> resultList = new ArrayList<>();
        for (SurgicalCenterDto surgicalCenter : surgicalCenterDtos) {
            List<SurgicalCenterTimeSlot> availableTimeSlotsSurgeryUnit = patientDialogPresenter
                    .getAvailableSurgeryUnitTimeSlots(surgicalCenter.getId());
            if (availableTimeSlotsSurgeryUnit != null && !availableTimeSlotsSurgeryUnit.isEmpty()) {
                resultList.addAll(availableTimeSlotsSurgeryUnit.stream().map(this::toDto).toList());
            }
        }
        return resultList;
    }

    private SurgicalCenterTimeSlotDto toDto(SurgicalCenterTimeSlot entity) {
        return modelToDtoMapper.toDto(entity);
    }

    public Collection<DiagnosisDto> getDiseases() {
        return patientDialogPresenter.getDiagnoses();
    }

    public Collection<SurgicalCenterTimeSlotDto> getAllTimeSlotsFilteredBy(LocalDate start, TimePeriod period,
            TimeSlotRepetition repetition,
            Integer surgicalCenterId) {
        Collection<SurgicalCenterTimeSlot> surgeryUnitTimeSlots = surgicalCenterService
                .findAvailableTimeSlotsFilteredBy(start, period, surgicalCenterId);

        List<SurgicalCenterTimeSlot> fullyFiltered = new ArrayList<>();

        var end = period.calculateEndDate(start);
        var repeatEveryWeeks = repetition.getRepeatEveryWeeks();

        for (SurgicalCenterTimeSlot slot : surgeryUnitTimeSlots) {
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

        List<SurgicalCenterTimeSlotDto> filteredDtos = new ArrayList<>();
        for (SurgicalCenterTimeSlot surgeryUnitTimeSlot : fullyFiltered) {
            SurgicalCenterTimeSlotDto dto = toDto(surgeryUnitTimeSlot);
            filteredDtos.add(dto);
        }

        return filteredDtos;
    }

    public List<TreatmentSlotDto> save(IntravitrealTreatmentDto ivomPlanDto,
            List<TreatmentSlotDto> timeSlotDtosToCreate) {

        workingCopy = ivomPlanDto;
        saveChanges();
        List<TreatmentSlot> ivomPlanTimeSlotsToCreate = new ArrayList<>();

        for (TreatmentSlotDto ivomPlanTimeSlotDto : timeSlotDtosToCreate) {

            TreatmentSlot ivomPlanTimeSlotToCreate = modelToDtoMapper.toEntity(ivomPlanTimeSlotDto);
            ivomPlanTimeSlotToCreate.setTreatmentPlan(original);
            Optional<SurgicalCenterTimeSlot> surgicalCenterTimeSlot = surgicalCenterService
                    .findSurgicalCenterTimeSlotById(ivomPlanTimeSlotDto.getSurgicalCenterTimeSlot().getId()); // TODO in
                                                                                                              // einem
            // select
            // abfragen und
            // per hashmap
            // bereitstellen
            // (.. where id
            // in (...))
            ivomPlanTimeSlotToCreate.setSurgicalCenterTimeSlot(surgicalCenterTimeSlot.get());
            ivomPlanTimeSlotsToCreate.add(ivomPlanTimeSlotToCreate);
        }
        List<TreatmentSlot> savedIvomPlanTimeSlots = treatmentPlanService.saveTimeSlots(ivomPlanTimeSlotsToCreate);
        List<TreatmentSlotDto> resultList = new ArrayList<>();
        for (TreatmentSlot ivomPlanTimeSlot : savedIvomPlanTimeSlots) {
            resultList.add(modelToDtoMapper.toDto(ivomPlanTimeSlot));
        }
        return resultList;
    }

    public IntravitrealTreatmentDto getById(Long id) {
        Optional<TreatmentPlan> ivomPlan = treatmentPlanService.findById(id);
        if (ivomPlan.isPresent()) {
            return modelToDtoMapper.toDto(ivomPlan.get());
        }
        return null;
    }

    public PatientDto getCurrentPatient() {
        return getWorkingCopy().getPatient();
    }

    public List<TreatmentSlotDto> getTreatments(SideOfEye sideOfEye) {
        List<TreatmentSlot> treatmentSlots = treatmentPlanService.getTreatmentSlots(getWorkingCopy().getId(),
                sideOfEye.asDbString());
        List<TreatmentSlotDto> treatmentSlotDtos = new ArrayList<>();
        for (TreatmentSlot treatmentSlot : treatmentSlots) {
            treatmentSlotDtos.add(modelToDtoMapper.toDto(treatmentSlot));
        }
        return treatmentSlotDtos;
    }
}
