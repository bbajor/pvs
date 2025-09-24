package de.bbajor.pvs.ivomplan.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.model.IvomPlan;
import de.bbajor.pvs.ivomplan.service.IvomPlanService;
import de.bbajor.pvs.ivomplan.service.SurgeryUnitService;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;

@Component
public class IvomListPresenter {

    private final SurgeryUnitService surgeryUnitService;

    private final IvomPlanService ivomService;
    private final PatientDialogPresenter patientDialogPresenter;

    public IvomListPresenter(IvomPlanService ivomService, PatientDialogPresenter patientDialogPresenter, SurgeryUnitService surgeryUnitService) {
        this.ivomService = ivomService;
        this.patientDialogPresenter = patientDialogPresenter;
        this.surgeryUnitService = surgeryUnitService;
    }

    public IvomDialogPresenter getDialogPresenter() {
        return new IvomDialogPresenter(patientDialogPresenter, surgeryUnitService);
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

}
