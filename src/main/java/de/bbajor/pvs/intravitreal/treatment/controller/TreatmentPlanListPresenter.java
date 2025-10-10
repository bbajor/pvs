package de.bbajor.pvs.intravitreal.treatment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;

@Component
public class TreatmentPlanListPresenter {

    @Autowired
    private TreatmentPlanService treatmentPlanService;
    @Autowired
    private TreatmentPlanPresenter treatmentPlanPresenter;

    public List<Treatment> generateWeeklyList() {
        return treatmentPlanService.generateWeeklyList(LocalDate.now());
    }

    public List<TreatmentPlan> findAllBy(String searchString) {
        return treatmentPlanService.findTreatmentPlans(searchString);
    }

    public TreatmentPlan saveNewTreatments(Long ivomPlanId, List<Treatment> timeSlotsToCreate) {
        return treatmentPlanPresenter.save(ivomPlanId, timeSlotsToCreate);
    }

    public List<TreatmentPlan> findAll() {
        return treatmentPlanService.findAll();
    }

}
