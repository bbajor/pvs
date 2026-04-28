package de.bbajor.pvs.api.v1.ivom;

import java.time.LocalDate;
import java.util.List;

public record IvomPlanDetailDto(
        Long id,
        LocalDate createdDate,
        LocalDate finishedDate,
        String description,
        String additionalInformation,
        PatientDto patient,
        DiagnosisDto diagnosis,
        FindingsDto findings,
        List<IvomTreatmentDto> treatments) {

    public record PatientDto(
            Integer id,
            String firstName,
            String lastName,
            LocalDate birth,
            String insuranceLabel) {
    }

    public record DiagnosisDto(
            Long id,
            String name,
            String icdCode) {
    }

    public record FindingsDto(
            Boolean subretinalFluid,
            Boolean intraretinalFluidIncrease,
            Boolean serousRpeDetachmentIncrease,
            Boolean newRetinalHemorrhage,
            String visualAcuityInitialLeft,
            String visualAcuityInitialRight) {
    }
}

