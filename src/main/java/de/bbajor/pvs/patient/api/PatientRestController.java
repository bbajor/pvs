package de.bbajor.pvs.patient.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientRestController {

    private final PatientService patientService;

    @GetMapping
    public List<Patient> list() {
        return patientService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Patient> get(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(patientService.findById(id));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<Patient> upsert(@RequestBody Patient patient) {
        Patient saved = patientService.save(patient);
        return ResponseEntity.ok(saved);
    }
}
