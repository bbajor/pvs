package de.bbajor.pvs.kbv.service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;
import de.bbajor.pvs.kbv.model.KbvIcdEntry;
import de.bbajor.pvs.kbv.model.KbvInsurance;

/**
 * Base parser for KBV XML structures.
 * Provides common parsing utilities and delegates to specific parsers.
 */
@Component
public class KbvXmlParser {

    private static final Logger log = LoggerFactory.getLogger(KbvXmlParser.class);

    private final KbvIcdParser icdParser;
    private final KbvCostCarrierParser costCarrierParser;
    private final KbvInsuranceParser insuranceParser;

    public KbvXmlParser(
            KbvIcdParser icdParser,
            KbvCostCarrierParser costCarrierParser,
            KbvInsuranceParser insuranceParser) {
        this.icdParser = icdParser;
        this.costCarrierParser = costCarrierParser;
        this.insuranceParser = insuranceParser;
    }

    public List<KbvIcdEntry> parseIcdEntries(InputStream xmlStream, String quarter, String version) {
        return icdParser.parse(xmlStream, quarter, version);
    }

    public List<KbvCostCarrier> parseCostCarriers(InputStream xmlStream, String quarter, String version) {
        return costCarrierParser.parse(xmlStream, quarter, version);
    }

    public List<KbvInsurance> parseInsurances(InputStream xmlStream, String quarter, String version) {
        return insuranceParser.parse(xmlStream, quarter, version);
    }

    /**
     * Extracts quarter and version from filename or XML metadata.
     * Format: YYYY-Q1, YYYY-Q2, YYYY-Q3, YYYY-Q4
     */
    public String extractQuarter(String filename) {
        // Try to extract from filename pattern like "icd10gm2024q1.xml" or "2024-Q1"
        String lower = filename.toLowerCase();
        if (lower.contains("q1") || lower.contains("q2") || lower.contains("q3") || lower.contains("q4")) {
            // Extract year and quarter
            String year = extractYear(lower);
            String quarter = extractQuarterNumber(lower);
            if (year != null && quarter != null) {
                return year + "-Q" + quarter;
            }
        }
        // Default to current quarter
        LocalDate now = LocalDate.now();
        int currentQuarter = (now.getMonthValue() - 1) / 3 + 1;
        return now.getYear() + "-Q" + currentQuarter;
    }

    private String extractYear(String filename) {
        // Look for 4-digit year
        for (int i = 0; i <= filename.length() - 4; i++) {
            String candidate = filename.substring(i, i + 4);
            try {
                int year = Integer.parseInt(candidate);
                if (year >= 2000 && year <= 2100) {
                    return candidate;
                }
            } catch (NumberFormatException e) {
                // Continue
            }
        }
        return null;
    }

    private String extractQuarterNumber(String filename) {
        if (filename.contains("q1")) return "1";
        if (filename.contains("q2")) return "2";
        if (filename.contains("q3")) return "3";
        if (filename.contains("q4")) return "4";
        return null;
    }
}
