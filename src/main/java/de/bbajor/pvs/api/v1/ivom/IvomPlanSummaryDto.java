package de.bbajor.pvs.api.v1.ivom;

import java.time.LocalDate;

public record IvomPlanSummaryDto(
        Long id,
        String patientLastName,
        String patientFirstName,
        LocalDate patientBirth,
        String diagnosisName,
        LocalDate createdDate,
        LocalDate finishedDate) {
}

