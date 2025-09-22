package de.bbajor.pvs.ivomplan.controller;

import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivomdrug.dto.IvomDrugDto;
import de.bbajor.pvs.ivomplan.dto.IvomPlanDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.model.IvomPlan;
import de.bbajor.pvs.ivomplan.service.IvomPlanService;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;

@Component
public class IvomDialogPresenter {

    private final PatientDialogPresenter patientDialogPresenter;
    private IvomPlanDto workingCopy;
    private IvomPlan original;

    public IvomDialogPresenter(PatientDialogPresenter patientDialogPresenter) {
        this.patientDialogPresenter = patientDialogPresenter;
    }

    public void loadIvomById(Long id) {
        if (id != null) {
            this.original = patientDialogPresenter.findById(id)
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
            IvomPlan newEntity = patientDialogPresenter.getModelToDtoMapper().toEntity(workingCopy);
            patientDialogPresenter.save(newEntity);
        } else {
            patientDialogPresenter.getModelToDtoMapper().updateEntityFromDto(workingCopy, original);
            patientDialogPresenter.save(original);
        }
    }

    public IvomPlanDto getWorkingCopy() {
        if (workingCopy == null) {
            workingCopy = new IvomPlanDto();
        }
        return workingCopy;
    }

    private IvomPlanDto copyFromEntity(IvomPlan e) {
        return patientDialogPresenter.getModelToDtoMapper().toDto(e);
    }

    private IvomPlan mapToEntity(IvomPlanDto dto) {
        return patientDialogPresenter.getModelToDtoMapper().toEntity(dto);
    }

    public List<PatientDto> getPatients() {
        return patientDialogPresenter.getPatients();
    }

    public List<IvomDrugDto> getDrugs() {
        return patientDialogPresenter.getDrugs();
    }

    public List<SurgeryUnitDto> getSurgeryUnits() {
        return patientDialogPresenter.getSurgeryUnits();
    }
}
