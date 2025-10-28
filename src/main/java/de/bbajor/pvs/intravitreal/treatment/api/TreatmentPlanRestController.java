package de.bbajor.pvs.intravitreal.treatment.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/treatment-plans")
@RequiredArgsConstructor
public class TreatmentPlanRestController {

    private final TreatmentPlanService treatmentPlanService;

    @GetMapping
    public List<TreatmentPlan> list() {
        return treatmentPlanService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TreatmentPlan> get(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(treatmentPlanService.loadTreatmentPlanWithFullDetails(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<TreatmentPlan> upsert(@RequestBody TreatmentPlan treatmentPlan) {
        TreatmentPlan saved = treatmentPlanService.saveTreatmentPlan(treatmentPlan);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/{id}/treatments")
    public List<Treatment> getTreatments(@PathVariable Long id) {
        return treatmentPlanService.getTreatmentSlots(id);
    }
}
