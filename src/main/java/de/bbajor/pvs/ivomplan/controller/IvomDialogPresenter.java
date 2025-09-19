package de.bbajor.pvs.ivomplan.controller;

import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.model.IvomPlan;
import de.bbajor.pvs.ivomplan.service.IvomPlanService;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;

@Component
public class IvomDialogPresenter {

    private final PatientDialogPresenter patientDialogPresenter;
    private final IvomPlanService ivomService;
    private IvomPlanDto workingCopy;
    private IvomPlan original;

    public IvomDialogPresenter(IvomPlanService ivomService, PatientDialogPresenter patientDialogPresenter) {
        this.ivomService = ivomService;
        this.patientDialogPresenter = patientDialogPresenter;
    }

    public void loadIvomById(Long id) {
        if (id != null) {
            this.original = ivomService.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Ivom not found: " + id));
            this.workingCopy = copyFromEntity(original);
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
            IvomPlan newEntity = mapToIvomEntity(new IvomPlan(), workingCopy);
            ivomService.save(newEntity);
        } else {
            mapToIvomEntity(original, workingCopy);
            ivomService.save(original);
        }
    }

    public IvomPlanDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new IvomPlanDto();
        }
        return workingCopy;
    }

    private IvomPlanDto copyFromEntity(IvomPlan e) {
        IvomPlanDto dto = new IvomPlanDto();
        dto.setId(e.getId())
                .setCreationDate(e.getCreationDate());
        // TODO: map other fields
        return dto;
    }

    private IvomPlan mapToIvomEntity(IvomPlan entity, IvomPlanDto dto) {

        if (dto == null) {
            return null;
        }

        if (entity == null) {
            entity = new IvomPlan();
        }
        entity
                .setCreationDate(dto.getCreationDate())
                .setDescription(dto.getAdditionalInformation());
        // TODO: map other fields
        return entity;
    }

    public List<PatientDto> getPatients() {
        return patientDialogPresenter.getPatients();
    }
}
