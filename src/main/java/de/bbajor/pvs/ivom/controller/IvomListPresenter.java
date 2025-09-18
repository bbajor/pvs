package de.bbajor.pvs.ivom.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivom.dto.IvomPlanDto;
import de.bbajor.pvs.ivom.model.IvomPlan;
import de.bbajor.pvs.ivom.service.IvomService;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;

@Component
public class IvomListPresenter {

    private final IvomService ivomService;
    private final PatientDialogPresenter patientDialogPresenter;

    public IvomListPresenter(IvomService ivomService, PatientDialogPresenter patientDialogPresenter) {
        this.ivomService = ivomService;
        this.patientDialogPresenter = patientDialogPresenter;
    }

    public IvomDialogPresenter getDialogPresenter() {
        return new IvomDialogPresenter(ivomService, patientDialogPresenter);
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
