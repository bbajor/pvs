package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomPlanDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentSlotDto;
import de.bbajor.pvs.intravitreal.treatment.model.IvomPlan;
import de.bbajor.pvs.intravitreal.treatment.service.IvomPlanService;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class IvomListPresenter {

    private final ModelToDtoMapper modelToDtoMapper;
    private final SurgicalCenterService surgeryUnitService;
    private final IvomPlanService ivomService;
    private final PatientDialogPresenter patientDialogPresenter;

    public IvomListPresenter(IvomPlanService ivomService, PatientDialogPresenter patientDialogPresenter,
            SurgicalCenterService surgeryUnitService, ModelToDtoMapper modelToDtoMapper) {
        this.ivomService = ivomService;
        this.patientDialogPresenter = patientDialogPresenter;
        this.surgeryUnitService = surgeryUnitService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public IvomPlanPresenter getDialogPresenter() {
        return new IvomPlanPresenter(patientDialogPresenter, surgeryUnitService, ivomService, modelToDtoMapper);
    }

    public List<IvomPlanDto> generateDailyList(LocalDate date) {
        return ivomService.generateDailyList(date)
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    public List<IvomPlanDto> findAllBy(String searchString) {
        List<IvomPlan> ivoms = ivomService.findIvoms(searchString);
        return ivoms.stream()
                .map(this::mapToDto)
                .toList();
    }

    private IvomPlanDto mapToDto(IvomPlan entity) {
        IvomPlanDto dto = new IvomPlanDto();
        dto.setId(entity.getId())
                .setCreationDate(entity.getCreationDate());
        return dto;
    }

    public void save(IvomPlanDto ivomPlanDto, List<TreatmentSlotDto> timeSlotsToCreate) {
        getDialogPresenter().save(ivomPlanDto, timeSlotsToCreate);
    }

}
