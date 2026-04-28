package de.bbajor.pvs.api.v1.ivom;

import java.time.LocalDate;
import java.time.LocalTime;

public record IvomTreatmentDto(
        Long id,
        String sideOfEye,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String surgicalCenterName,
        String medicationName,
        String frequency,
        String dosage,
        String status) {
}

