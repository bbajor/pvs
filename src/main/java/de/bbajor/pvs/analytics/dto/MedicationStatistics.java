package de.bbajor.pvs.analytics.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO für Medikamenten-Statistiken.
 */
public record MedicationStatistics(
    List<TimeSeriesData> monthlyData,
    List<TimeSeriesData> yearlyData,
    Map<String, Long> byMedication
) {
}


