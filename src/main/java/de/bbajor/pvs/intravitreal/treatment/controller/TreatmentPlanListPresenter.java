package de.bbajor.pvs.intravitreal.treatment.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class TreatmentPlanListPresenter {

    private final SurgicalCenterService surgicalCenterService;
    private final TreatmentPlanService treatmentPlanService;

    @Autowired
    private TreatmentPlanPresenter treatmentPlanPresenter;

    public TreatmentPlanListPresenter(TreatmentPlanService treatmentPlanService,
            PatientDialogPresenter patientDialogPresenter,
            SurgicalCenterService surgicalCenterService) {
        this.treatmentPlanService = treatmentPlanService;
        this.surgicalCenterService = surgicalCenterService;
    }

    public List<TreatmentPlanDto> generateDailyList() {
        return treatmentPlanService.generateDailyList();
    }

    public List<TreatmentPlanDto> findAllBy(String searchString) {
        return treatmentPlanService.getTreatmentPlans(searchString);
    }

    public void save(TreatmentPlanDto ivomPlanDto, List<TreatmentDto> timeSlotsToCreate) {
        treatmentPlanPresenter.saveNewTreatments(timeSlotsToCreate);
    }

    public List<TreatmentPlanDto> findAll() {
        return treatmentPlanService.getTreatmentPlans();
    }

}
