package de.bbajor.pvs.analytics.service;

import de.bbajor.pvs.analytics.dto.AgeGroupStatistics;
import de.bbajor.pvs.analytics.dto.AnalyticsData;
import de.bbajor.pvs.analytics.dto.InsuranceStatistics;
import de.bbajor.pvs.analytics.dto.MedicationStatistics;
import de.bbajor.pvs.analytics.dto.TimeSeriesData;
import de.bbajor.pvs.analytics.dto.TreatmentStatistics;
import de.bbajor.pvs.analytics.repository.AnalyticsRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.patient.model.Patient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.DateAndTimeUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service für Analytics-Datenaggregation.
 * Alle Methoden filtern automatisch nach Institution (aktueller Login).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final AnalyticsRepository analyticsRepository;
    
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM.yyyy", Locale.GERMAN);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.GERMAN);

    /**
     * Lädt alle Analytics-Daten für die aktuelle Institution.
     */
    public AnalyticsData getAllAnalyticsData() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access analytics data without institution context");
        }

        return new AnalyticsData(
            getTreatmentStatistics(institutionId),
            getAgeGroupStatistics(institutionId),
            getInsuranceStatistics(institutionId),
            getMedicationStatistics(institutionId)
        );
    }

    /**
     * Lädt Behandlungs-Statistiken.
     */
    public TreatmentStatistics getTreatmentStatistics(Long institutionId) {
        List<Treatment> treatments = analyticsRepository.findAllTreatmentsWithTimeSlot(institutionId);
        
        // Aggregation nach Monat
        Map<String, Long> monthlyMap = treatments.stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getDate() != null)
            .collect(Collectors.groupingBy(
                t -> {
                    LocalDate date = t.getSurgicalCenterTimeSlot().getDate();
                    // Format: MM.yyyy (z.B. "01.2024")
                    return date.format(MONTH_FORMATTER);
                },
                Collectors.counting()
            ));
        List<TimeSeriesData> monthlyData = monthlyMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        // Aggregation nach Jahr
        Map<String, Long> yearlyMap = treatments.stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getDate() != null)
            .collect(Collectors.groupingBy(
                t -> String.valueOf(t.getSurgicalCenterTimeSlot().getDate().getYear()),
                Collectors.counting()
            ));
        List<TimeSeriesData> yearlyData = yearlyMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        // Aggregation nach Zeitslot
        Map<String, Long> timeSlotMap = treatments.stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getDate() != null)
            .collect(Collectors.groupingBy(
                t -> {
                    var ts = t.getSurgicalCenterTimeSlot();
                    // Format: dd.MM.yyyy HH:mm (z.B. "15.01.2024 10:00")
                    if (ts.getStartTime() != null) {
                        return ts.getDate().atTime(ts.getStartTime()).format(DATETIME_FORMATTER);
                    }
                    return DateAndTimeUtils.getGermanDateTimeFormatter().format(ts.getDate());
                },
                Collectors.counting()
            ));
        List<TimeSeriesData> byTimeSlot = timeSlotMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        return new TreatmentStatistics(monthlyData, yearlyData, byTimeSlot);
    }

    /**
     * Lädt Altersgruppen-Statistiken.
     */
    public AgeGroupStatistics getAgeGroupStatistics(Long institutionId) {
        List<Patient> patients = analyticsRepository.findAllPatientsByInstitution(institutionId);
        
        Map<String, Long> ageGroups = patients.stream()
            .filter(p -> p.getBirth() != null)
            .collect(Collectors.groupingBy(
                p -> calculateAgeGroup(p.getBirth()),
                Collectors.counting()
            ));

        // Sortiere nach Altersgruppen-Reihenfolge
        Map<String, Long> sortedAgeGroups = new LinkedHashMap<>();
        String[] order = {"0-3", "4-12", "13-18", "19-30", "31-50", "51-65", "66-80", "81+"};
        for (String group : order) {
            if (ageGroups.containsKey(group)) {
                sortedAgeGroups.put(group, ageGroups.get(group));
            }
        }
        // Unbekannte Altersgruppen am Ende
        ageGroups.entrySet().stream()
            .filter(e -> !sortedAgeGroups.containsKey(e.getKey()))
            .forEach(e -> sortedAgeGroups.put(e.getKey(), e.getValue()));

        return new AgeGroupStatistics(sortedAgeGroups);
    }

    /**
     * Berechnet die Altersgruppe für ein Geburtsdatum.
     */
    private String calculateAgeGroup(LocalDate birthDate) {
        if (birthDate == null) {
            return "Unbekannt";
        }
        
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        
        if (age >= 0 && age <= 3) return "0-3";
        if (age >= 4 && age <= 12) return "4-12";
        if (age >= 13 && age <= 18) return "13-18";
        if (age >= 19 && age <= 30) return "19-30";
        if (age >= 31 && age <= 50) return "31-50";
        if (age >= 51 && age <= 65) return "51-65";
        if (age >= 66 && age <= 80) return "66-80";
        if (age >= 81) return "81+";
        
        return "Unbekannt";
    }

    /**
     * Lädt Versicherungs-Statistiken.
     */
    public InsuranceStatistics getInsuranceStatistics(Long institutionId) {
        List<Patient> patients = analyticsRepository.findAllPatientsByInstitution(institutionId);

        // Nach Versicherungsart (Kasse/privat)
        Map<String, Long> typeMap = patients.stream()
            .collect(Collectors.groupingBy(
                p -> Boolean.TRUE.equals(p.getIsPrivateInsurance()) ? "Privat" : "Kasse",
                Collectors.counting()
            ));

        // Nach Versicherungsanbieter
        Map<String, Long> providerMap = patients.stream()
            .collect(Collectors.groupingBy(
                p -> {
                    if (p.getHealthInsurance() != null && p.getHealthInsurance().getCostCarrierName() != null) {
                        return p.getHealthInsurance().getCostCarrierName();
                    }
                    return "Nicht angegeben";
                },
                Collectors.counting()
            ));

        // Sortiere Provider nach Anzahl (absteigend)
        Map<String, Long> sortedProviderMap = providerMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return new InsuranceStatistics(typeMap, sortedProviderMap);
    }

    /**
     * Lädt Medikamenten-Statistiken.
     */
    public MedicationStatistics getMedicationStatistics(Long institutionId) {
        List<Treatment> treatments = analyticsRepository.findAllTreatmentsWithMedication(institutionId);

        // Aggregation nach Medikament und Monat
        Map<String, Long> monthlyMap = treatments.stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getDate() != null)
            .collect(Collectors.groupingBy(
                t -> {
                    LocalDate date = t.getSurgicalCenterTimeSlot().getDate();
                    // Format: MM.yyyy (z.B. "01.2024")
                    String monthKey = date.format(MONTH_FORMATTER);
                    Medication med = t.getMedication();
                    String medName = (med != null && med.getArzneimittelbezeichnung() != null) 
                        ? med.getArzneimittelbezeichnung() 
                        : "Kein Medikament";
                    return monthKey + " - " + medName;
                },
                Collectors.counting()
            ));
        List<TimeSeriesData> monthlyData = monthlyMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        // Aggregation nach Medikament und Jahr
        Map<String, Long> yearlyMap = treatments.stream()
            .filter(t -> t.getSurgicalCenterTimeSlot() != null && t.getSurgicalCenterTimeSlot().getDate() != null)
            .collect(Collectors.groupingBy(
                t -> {
                    int year = t.getSurgicalCenterTimeSlot().getDate().getYear();
                    Medication med = t.getMedication();
                    String medName = (med != null && med.getArzneimittelbezeichnung() != null) 
                        ? med.getArzneimittelbezeichnung() 
                        : "Kein Medikament";
                    return year + " - " + medName;
                },
                Collectors.counting()
            ));
        List<TimeSeriesData> yearlyData = yearlyMap.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(e -> new TimeSeriesData(e.getKey(), e.getValue()))
            .collect(Collectors.toList());

        // Aggregation nach Medikament (gesamt)
        Map<String, Long> medicationMap = treatments.stream()
            .collect(Collectors.groupingBy(
                t -> {
                    Medication med = t.getMedication();
                    return (med != null && med.getArzneimittelbezeichnung() != null) 
                        ? med.getArzneimittelbezeichnung() 
                        : "Kein Medikament";
                },
                Collectors.counting()
            ));

        // Sortiere nach Anzahl (absteigend)
        Map<String, Long> sortedMedicationMap = medicationMap.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        return new MedicationStatistics(monthlyData, yearlyData, sortedMedicationMap);
    }
}











