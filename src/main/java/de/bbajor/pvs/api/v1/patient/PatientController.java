package de.bbajor.pvs.api.v1.patient;

import static de.bbajor.pvs.api.v1.common.ApiPaging.page;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.api.v1.common.SliceResponse;
import de.bbajor.pvs.api.error.ResourceNotFoundException;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;

@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public SliceResponse<PatientSummaryDto> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size) {
        Pageable pageable = page(page, size);
        var slice = patientService.findPatients(query, pageable);
        return new SliceResponse<>(
                slice.getContent().stream().map(PatientController::toSummary).toList(),
                slice.hasNext());
    }

    @GetMapping("/{id}")
    public PatientSummaryDto byId(@PathVariable("id") Integer id) {
        try {
            Patient patient = patientService.findById(id);
            return toSummary(patient);
        } catch (RuntimeException ex) {
            throw new ResourceNotFoundException("Patient not found.");
        }
    }

    private static PatientSummaryDto toSummary(Patient patient) {
        return new PatientSummaryDto(
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getBirth(),
                patient.getInsuranceNumber(),
                patient.getIsPrivateInsurance());
    }
}

