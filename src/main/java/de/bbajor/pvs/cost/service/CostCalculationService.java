package de.bbajor.pvs.cost.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.cost.model.CostCalculation;
import de.bbajor.pvs.cost.model.PricingModel;
import de.bbajor.pvs.cost.repository.CostCalculationRepository;
import de.bbajor.pvs.cost.repository.TreatmentCostRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import lombok.RequiredArgsConstructor;

/**
 * Service für Kostenberechnung basierend auf Preismodellen.
 */
@Service
@RequiredArgsConstructor
public class CostCalculationService {

    private final CostCalculationRepository costCalculationRepository;
    private final TreatmentCostRepository treatmentCostRepository;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Findet das aktive Preismodell für einen OP-Saal zum gegebenen Datum.
     */
    @Transactional(readOnly = true)
    public Optional<CostCalculation> findActiveCalculation(Integer surgicalCenterId, LocalDate date) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return Optional.empty();
        }

        List<CostCalculation> calculations = costCalculationRepository
                .findActiveBySurgicalCenterAndDate(surgicalCenterId, institutionId, date);

        // Nimm das neueste (erste in der Liste, da nach validFrom DESC sortiert)
        return calculations.stream().findFirst();
    }

    /**
     * Berechnet die Kosten für einen Behandlungsslot.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCostForTimeSlot(SurgicalCenterTimeSlot timeSlot, LocalDate treatmentDate) {
        if (timeSlot == null || timeSlot.getSurgicalCenter() == null) {
            return BigDecimal.ZERO;
        }

        Optional<CostCalculation> calculationOpt = findActiveCalculation(
                timeSlot.getSurgicalCenter().getId(),
                treatmentDate);

        if (calculationOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }

        CostCalculation calculation = calculationOpt.get();

        return switch (calculation.getPricingModel()) {
            case RENTAL -> calculateRentalCost(calculation, timeSlot);
            case OWNED -> calculateOwnedCost(calculation, timeSlot, treatmentDate);
        };
    }

    /**
     * Berechnet Kosten für RENTAL-Modell (Miete).
     */
    private BigDecimal calculateRentalCost(CostCalculation calculation, SurgicalCenterTimeSlot timeSlot) {
        // Fixpreis pro Zeitslot hat Priorität
        if (calculation.getPricePerSlot() != null) {
            return calculation.getPricePerSlot();
        }

        // Falls kein Fixpreis, berechne nach Stunden
        if (calculation.getPricePerHour() != null && timeSlot.getStartTime() != null
                && timeSlot.getEndTime() != null) {
            long hours = java.time.temporal.ChronoUnit.HOURS.between(
                    timeSlot.getStartTime(),
                    timeSlot.getEndTime());
            if (hours < 1) {
                // Mindestens 1 Stunde
                hours = 1;
            }
            return calculation.getPricePerHour()
                    .multiply(BigDecimal.valueOf(hours));
        }

        return BigDecimal.ZERO;
    }

    /**
     * Berechnet Kosten für OWNED-Modell (eigener OP-Saal).
     */
    private BigDecimal calculateOwnedCost(CostCalculation calculation, SurgicalCenterTimeSlot timeSlot,
            LocalDate treatmentDate) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return BigDecimal.ZERO;
        }

        // 1. Monatliche Fixkosten auf Behandlungen im Monat aufteilen
        String month = treatmentDate.format(MONTH_FORMATTER);
        Long treatmentsInMonth = treatmentCostRepository.countTreatmentsInMonth(
                timeSlot.getSurgicalCenter().getId(),
                institutionId,
                month);

        BigDecimal monthlyCostPerTreatment = BigDecimal.ZERO;
        if (treatmentsInMonth != null && treatmentsInMonth > 0) {
            monthlyCostPerTreatment = calculation.getMonthlyFixedCosts()
                    .divide(BigDecimal.valueOf(treatmentsInMonth), 2, RoundingMode.HALF_UP);
        } else {
            // Falls keine Behandlungen im Monat, gesamte Fixkosten auf diese Behandlung
            monthlyCostPerTreatment = calculation.getMonthlyFixedCosts();
        }

        // 2. Variable Kosten hinzufügen
        BigDecimal variableCost = calculation.getVariableCostPerTreatment() != null
                ? calculation.getVariableCostPerTreatment()
                : BigDecimal.ZERO;

        return monthlyCostPerTreatment.add(variableCost);
    }

    /**
     * Findet alle Preismodelle für einen OP-Saal.
     */
    @Transactional(readOnly = true)
    public List<CostCalculation> findBySurgicalCenter(Integer surgicalCenterId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return List.of();
        }

        return costCalculationRepository.findBySurgicalCenterIdAndInstitutionId(surgicalCenterId, institutionId);
    }

    /**
     * Speichert ein Preismodell.
     */
    @Transactional
    public CostCalculation save(CostCalculation calculation) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot save cost calculation without institution context");
        }

        // Stelle sicher, dass Institution gesetzt ist
        if (calculation.getInstitution() == null
                || !calculation.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Cost calculation institution does not match current institution");
        }

        return costCalculationRepository.save(calculation);
    }
}

