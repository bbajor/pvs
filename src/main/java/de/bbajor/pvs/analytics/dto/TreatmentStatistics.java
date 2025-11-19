package de.bbajor.pvs.analytics.dto;

import java.util.List;

/**
 * DTO für Behandlungs-Statistiken.
 */
public record TreatmentStatistics(
    List<TimeSeriesData> monthlyData,
    List<TimeSeriesData> yearlyData,
    List<TimeSeriesData> byTimeSlot
) {
}







