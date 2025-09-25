package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomDiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomPlanDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentSlotDto;
import de.bbajor.pvs.intravitreal.treatment.model.IvomDiagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.IvomPlan;
import de.bbajor.pvs.intravitreal.treatment.model.IvomPlanTimeSlot;
import de.bbajor.pvs.intravitreal.treatment.service.IvomPlanService;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class IvomPlanPresenter {

    private final ModelToDtoMapper modelToDtoMapper;

    private final IvomPlanService ivomPlanService;
    private final SurgicalCenterService surgeryUnitService;
    private final PatientDialogPresenter patientDialogPresenter;

    private IvomPlanDto workingCopy;
    private IvomPlan original;

    public IvomPlanPresenter(PatientDialogPresenter patientDialogPresenter, SurgicalCenterService surgeryUnitService,
            IvomPlanService ivomPlanService, ModelToDtoMapper modelToDtoMapper) {
        this.patientDialogPresenter = patientDialogPresenter;
        this.surgeryUnitService = surgeryUnitService;
        this.ivomPlanService = ivomPlanService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public void loadIvomById(Long id) {
        if (id != null) {
            this.original = ivomPlanService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ivom not found: " + id));
            this.workingCopy = modelToDtoMapper.toDto(original);
        } else {
            this.original = null;
            this.workingCopy = new IvomPlanDto(); // leere WorkingCopy für Neuanlage
        }
    }

    public void saveChanges() {
        if (workingCopy == null) {
            throw new IllegalStateException("No data loaded into dialog");
        }
        if (original == null) {
            IvomPlan newEntity = modelToDtoMapper.toEntity(workingCopy);
            original = ivomPlanService.save(newEntity);
        } else {
            modelToDtoMapper.updateEntityFromDto(workingCopy, original);
            original = ivomPlanService.save(original);
        }
    }

    public IvomPlanDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new IvomPlanDto();
        }
        return workingCopy;
    }

    public List<PatientDto> getPatients() {
        return patientDialogPresenter.getPatients();
    }

    public List<IntravitrealMedicationDto> getDrugs() {
        return patientDialogPresenter.getDrugs();
    }

    public List<SurgicalCenterDto> getSurgeryUnits() {
        return patientDialogPresenter.getSurgeryUnits();
    }

    public IvomDiagnosisDto saveDiagnosis(IvomDiagnosisDto newDto) {
        IvomDiagnosis newEntity = toEntity(newDto);
        return patientDialogPresenter.save(newEntity);
    }

    private IvomDiagnosis toEntity(IvomDiagnosisDto dto) {
        return modelToDtoMapper.toEntity(dto);
    }

    public List<SurgicalCenterTimeSlotDto> loadAvailableSurgeryUnitTimeSlots(SurgicalCenterDto selectedSurgeryUnit) {
        List<SurgicalCenterDto> surgeryUnits = new ArrayList<>();
        if (selectedSurgeryUnit == null) { // if no specific surgeryunit has been selected, choose all
            surgeryUnits.addAll(patientDialogPresenter.getSurgeryUnits());
        } else {
            surgeryUnits.add(selectedSurgeryUnit);
        }
        List<SurgicalCenterTimeSlotDto> resultList = new ArrayList<>();
        for (SurgicalCenterDto surgeryUnit : surgeryUnits) {
            List<SurgicalCenterTimeSlot> availableTimeSlotsSurgeryUnit = patientDialogPresenter
                    .getAvailableSurgeryUnitTimeSlots(surgeryUnit.getId());
            if (availableTimeSlotsSurgeryUnit != null && !availableTimeSlotsSurgeryUnit.isEmpty()) {
                resultList.addAll(availableTimeSlotsSurgeryUnit.stream().map(this::toDto).toList());
            }
        }
        return resultList;
    }

    private SurgicalCenterTimeSlotDto toDto(SurgicalCenterTimeSlot entity) {
        return modelToDtoMapper.toDto(entity);
    }

    public Collection<IvomDiagnosisDto> getDiseases() {
        return patientDialogPresenter.getDiagnoses();
    }

    public Collection<SurgicalCenterTimeSlotDto> getAllTimeSlotsFilteredBy(TimeSlotConfig currentConfig,
            SurgicalCenterDto surgeryUnitDto) {
        Collection<SurgicalCenterTimeSlot> surgeryUnitTimeSlots = surgeryUnitService.findAvailableTimeSlotsFilteredBy(
                currentConfig.getPeriodStartDate(),
                currentConfig.getTimePeriod(), surgeryUnitDto.getId());

        List<SurgicalCenterTimeSlot> fullyFiltered = new ArrayList<>();
        LocalDate startDate = currentConfig.getPeriodStartDate();
        LocalDate endDate = currentConfig.getTimePeriod().calculateEndDate(startDate);

        int repeatEveryWeeks = currentConfig.getTimeSlotRepetition().getRepeatEveryWeeks();

        for (SurgicalCenterTimeSlot slot : surgeryUnitTimeSlots) {
            LocalDate slotDate = slot.getDate();

            // nur Slots innerhalb des Zeitraums beachten
            if (!slotDate.isBefore(startDate) && !slotDate.isAfter(endDate)) {

                // Abstands-Berechnung in Wochen (inkl. Jahrwechsel)
                long weeksBetween = ChronoUnit.WEEKS.between(startDate, slotDate);

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

    public List<TreatmentSlotDto> save(IvomPlanDto ivomPlanDto, List<TreatmentSlotDto> timeSlotDtosToCreate) {

        workingCopy = ivomPlanDto;
        saveChanges();
        List<IvomPlanTimeSlot> ivomPlanTimeSlotsToCreate = new ArrayList<>();

        for (TreatmentSlotDto ivomPlanTimeSlotDto : timeSlotDtosToCreate) {

            IvomPlanTimeSlot ivomPlanTimeSlotToCreate = modelToDtoMapper.toEntity(ivomPlanTimeSlotDto);
            ivomPlanTimeSlotToCreate.setIvomPlan(original);
            Optional<SurgicalCenterTimeSlot> surgicalCenterTimeSlot = surgeryUnitService
                    .findSurgicalCenterTimeSlotById(ivomPlanTimeSlotDto.getSurgicalCenterTimeSlot().getId()); // TODO in einem
                                                                                                        // select
                                                                                                        // abfragen und
                                                                                                        // per hashmap
                                                                                                        // bereitstellen
                                                                                                        // (.. where id
                                                                                                        // in (...))
            ivomPlanTimeSlotToCreate.setSurgicalCenterTimeSlot(surgicalCenterTimeSlot.get());
            ivomPlanTimeSlotsToCreate.add(ivomPlanTimeSlotToCreate);
        }
        List<IvomPlanTimeSlot> savedIvomPlanTimeSlots = ivomPlanService.saveTimeSlots(ivomPlanTimeSlotsToCreate);
        List<TreatmentSlotDto> resultList = new ArrayList<>();
        for (IvomPlanTimeSlot ivomPlanTimeSlot : savedIvomPlanTimeSlots) {
            resultList.add(modelToDtoMapper.toDto(ivomPlanTimeSlot));
        }
        return resultList;
    }

    public IvomPlanDto getById(Long id) {
        Optional<IvomPlan> ivomPlan = ivomPlanService.findById(id);
        if (ivomPlan.isPresent()) {
            return modelToDtoMapper.toDto(ivomPlan.get());
        }
        return null;
    }
}
