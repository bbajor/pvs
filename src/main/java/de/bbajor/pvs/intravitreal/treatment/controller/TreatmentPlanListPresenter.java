package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.intravitreal.treatment.dto.IntravitrealTreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentSlotDto;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class TreatmentPlanListPresenter {

    private final ModelToDtoMapper modelToDtoMapper;
    private final SurgicalCenterService surgicalCenterService;
    private final TreatmentPlanService treatmentPlanService;
    private final PatientDialogPresenter patientDialogPresenter;

    public TreatmentPlanListPresenter(TreatmentPlanService treatmentPlanService, PatientDialogPresenter patientDialogPresenter,
            SurgicalCenterService surgicalCenterService, ModelToDtoMapper modelToDtoMapper) {
        this.treatmentPlanService = treatmentPlanService;
        this.patientDialogPresenter = patientDialogPresenter;
        this.surgicalCenterService = surgicalCenterService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public TreatmentPlanPresenter getDialogPresenter() {
        return new TreatmentPlanPresenter(patientDialogPresenter, surgicalCenterService, treatmentPlanService, modelToDtoMapper);
    }

    public List<IntravitrealTreatmentDto> generateDailyList(LocalDate date) {
        return treatmentPlanService.generateDailyList(date)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<IntravitrealTreatmentDto> findAllBy(String searchString) {
        List<TreatmentPlan> ivoms = treatmentPlanService.findIvoms(searchString);
        return ivoms.stream()
                .map(this::mapToDto)
                .toList();
    }

    private IntravitrealTreatmentDto mapToDto(TreatmentPlan entity) {
        IntravitrealTreatmentDto dto = new IntravitrealTreatmentDto();
        dto.setId(entity.getId())
                .setCreationDate(entity.getCreationDate());
        return dto;
    }

    public void save(IntravitrealTreatmentDto ivomPlanDto, List<TreatmentSlotDto> timeSlotsToCreate) {
        getDialogPresenter().save(ivomPlanDto, timeSlotsToCreate);
    }

}
