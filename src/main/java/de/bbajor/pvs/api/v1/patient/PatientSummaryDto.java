package de.bbajor.pvs.api.v1.patient;

import java.time.LocalDate;

public record PatientSummaryDto(
        Integer id,
        String firstName,
        String lastName,
        LocalDate birth,
        String insuranceNumber,
        Boolean privateInsurance) {
}

