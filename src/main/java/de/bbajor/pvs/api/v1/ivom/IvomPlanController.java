package de.bbajor.pvs.api.v1.ivom;

import static de.bbajor.pvs.api.v1.common.ApiPaging.page;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.api.error.ResourceNotFoundException;
import de.bbajor.pvs.api.v1.common.SliceResponse;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;

@RestController
@RequestMapping("/api/v1/ivom-plans")
public class IvomPlanController {

    private final TreatmentPlanService treatmentPlanService;

    public IvomPlanController(TreatmentPlanService treatmentPlanService) {
        this.treatmentPlanService = treatmentPlanService;
    }

    @GetMapping
    public SliceResponse<IvomPlanSummaryDto> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "25") int size) {
        Pageable pageable = page(page, size);

        var slice = (query == null || query.isBlank())
                ? treatmentPlanService.findAll(pageable)
                : treatmentPlanService.findTreatmentPlans(query, pageable);

        return new SliceResponse<>(
                slice.getContent().stream().map(IvomPlanController::toSummary).toList(),
                slice.hasNext());
    }

    @GetMapping("/{id}")
    public IvomPlanDetailDto detail(@PathVariable("id") Long id) {
        try {
            TreatmentPlan plan = treatmentPlanService.findByIdWithDetails(id);
            return toDetail(plan);
        } catch (RuntimeException ex) {
            throw new ResourceNotFoundException("IVOM plan not found.");
        }
    }

    private static IvomPlanSummaryDto toSummary(TreatmentPlan plan) {
        String diagnosisName = plan.getDiagnosis() == null ? null : plan.getDiagnosis().getName();
        return new IvomPlanSummaryDto(
                plan.getId(),
                plan.getPatient() == null ? null : plan.getPatient().getLastName(),
                plan.getPatient() == null ? null : plan.getPatient().getFirstName(),
                plan.getPatient() == null ? null : plan.getPatient().getBirth(),
                diagnosisName,
                plan.getCreationDate(),
                plan.getFinishedDate());
    }

    private static IvomPlanDetailDto toDetail(TreatmentPlan plan) {
        var patient = plan.getPatient();
        var patientDto = patient == null
                ? null
                : new IvomPlanDetailDto.PatientDto(
                        patient.getId(),
                        patient.getFirstName(),
                        patient.getLastName(),
                        patient.getBirth(),
                        plan.getHealthInsurance());

        var diagnosis = plan.getDiagnosis();
        var diagnosisDto = diagnosis == null
                ? null
                : new IvomPlanDetailDto.DiagnosisDto(diagnosis.getId(), diagnosis.getName(), diagnosis.getIcdCode());

        var findingsDto = new IvomPlanDetailDto.FindingsDto(
                plan.getSubretinalFluid(),
                plan.getIntraretinalFluidIncrease(),
                plan.getSerousRpeDetachmentIncrease(),
                plan.getNewRetinalHemorrhage(),
                plan.getVisualAcuityInitialLeft(),
                plan.getVisualAcuityInitialRight());

        List<IvomTreatmentDto> treatments = plan.getTreatments() == null
                ? List.of()
                : plan.getTreatments().stream().map(IvomPlanController::toTreatmentDto).toList();

        return new IvomPlanDetailDto(
                plan.getId(),
                plan.getCreationDate(),
                plan.getFinishedDate(),
                plan.getDescription(),
                plan.getAdditionalInformation(),
                patientDto,
                diagnosisDto,
                findingsDto,
                treatments);
    }

    private static IvomTreatmentDto toTreatmentDto(Treatment t) {
        var ts = t.getSurgicalCenterTimeSlot();
        String scName = (ts != null && ts.getSurgicalCenter() != null) ? ts.getSurgicalCenter().getName() : null;
        String medicationName = t.getMedication() == null ? null : t.getMedication().getArzneimittelbezeichnung();
        return new IvomTreatmentDto(
                t.getId(),
                t.getSideOfEye() == null ? null : t.getSideOfEye().name(),
                ts == null ? null : ts.getDate(),
                ts == null ? null : ts.getStartTime(),
                ts == null ? null : ts.getEndTime(),
                scName,
                medicationName,
                t.getFrequency(),
                t.getDosage(),
                t.getTreatmentStatus() == null ? null : t.getTreatmentStatus().name());
    }
}

