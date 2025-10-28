package de.bbajor.pvs.medication.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.service.IntravitrealMedicationService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationRestController {

    private final IntravitrealMedicationService medicationService;

    @GetMapping
    public List<Medication> list() {
        return medicationService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Medication> get(@PathVariable Long id) {
        return medicationService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Medication> upsert(@RequestBody Medication medication) {
        Medication saved = medicationService.save(medication);
        return ResponseEntity.ok(saved);
    }
}
