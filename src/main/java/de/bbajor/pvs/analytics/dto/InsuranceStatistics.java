package de.bbajor.pvs.analytics.dto;

import java.util.Map;

/**
 * DTO für Versicherungs-Statistiken.
 */
public record InsuranceStatistics(
    Map<String, Long> byType,
    Map<String, Long> byProvider
) {
}











