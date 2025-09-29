package de.bbajor.pvs.intravitreal.treatment.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;

@Component
public class TreatmentPlanListPresenter {

    @Autowired
    private TreatmentPlanService treatmentPlanService;
    @Autowired
    private TreatmentPlanPresenter treatmentPlanPresenter;

    public List<TreatmentPlanDto> generateDailyList() {
        return treatmentPlanService.generateDailyList();
    }

    public List<TreatmentPlanDto> findAllBy(String searchString) {
        return treatmentPlanService.getTreatmentPlans(searchString);
    }

    public TreatmentPlanDto save(TreatmentPlanDto ivomPlanDto, List<TreatmentDto> timeSlotsToCreate) {
        return treatmentPlanPresenter.saveNewTreatments(timeSlotsToCreate);
    }

    public List<TreatmentPlanDto> findAll() {
        return treatmentPlanService.getTreatmentPlans();
    }

}
