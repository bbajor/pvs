package de.bbajor.pvs.analytics.dto;

/**
 * Container-DTO für alle Analytics-Daten.
 */
public record AnalyticsData(
    TreatmentStatistics treatmentStatistics,
    AgeGroupStatistics ageGroupStatistics,
    InsuranceStatistics insuranceStatistics,
    MedicationStatistics medicationStatistics
) {
}

