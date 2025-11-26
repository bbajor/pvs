package de.bbajor.pvs.cost.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.cost.model.PatientCostHistory;
import de.bbajor.pvs.cost.model.PricingModel;
import de.bbajor.pvs.cost.model.TreatmentCost;
import de.bbajor.pvs.cost.repository.PatientCostHistoryRepository;
import de.bbajor.pvs.cost.repository.TreatmentCostRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.security.domain.UserAccount;
import lombok.RequiredArgsConstructor;

/**
 * Service für Verwaltung von TreatmentCost-Entities.
 */
@Service
@RequiredArgsConstructor
public class TreatmentCostService {

    private final TreatmentCostRepository treatmentCostRepository;
    private final PatientCostHistoryRepository patientCostHistoryRepository;
    private final CostCalculationService costCalculationService;
    private final TreatmentRepository treatmentRepository;

    /**
     * Berechnet und speichert Kosten für eine Behandlung.
     */
    @Transactional
    public TreatmentCost calculateAndSaveTreatmentCost(Treatment treatment, UserAccount calculatedBy) {
        if (treatment.getSurgicalCenterTimeSlot() == null) {
            throw new IllegalArgumentException("Treatment must have a surgical center time slot");
        }

        // 1. Berechne Gesamtkosten für den Zeitslot
        BigDecimal totalCost = costCalculationService.calculateCostForTimeSlot(
                treatment.getSurgicalCenterTimeSlot(),
                treatment.getSurgicalCenterTimeSlot().getDate());

        // 2. Zähle Patienten im Zeitslot
        List<de.bbajor.pvs.intravitreal.treatment.model.Treatment> treatmentsInSlot = treatmentRepository
                .findByTimeSlotId(treatment.getSurgicalCenterTimeSlot().getId());
        int patientCount = treatmentsInSlot.size();

        // 3. Berechne Kostenanteil pro Patient
        BigDecimal costPerPatient = patientCount > 0
                ? totalCost.divide(BigDecimal.valueOf(patientCount), 2, RoundingMode.HALF_UP)
                : totalCost;

        // 4. Hole Preismodell
        PricingModel pricingModelUsed = costCalculationService
                .findActiveCalculation(
                        treatment.getSurgicalCenterTimeSlot().getSurgicalCenter().getId(),
                        treatment.getSurgicalCenterTimeSlot().getDate())
                .map(calc -> calc.getPricingModel())
                .orElse(null);

        // 5. Erstelle TreatmentCost
        TreatmentCost treatmentCost = new TreatmentCost();
        treatmentCost.setTreatment(treatment);
        treatmentCost.setTotalCost(totalCost);
        treatmentCost.setCostPerPatient(costPerPatient);
        treatmentCost.setPatientCountAtCalculation(patientCount);
        treatmentCost.setPricingModelUsed(pricingModelUsed);
        treatmentCost.setCalculatedAt(LocalDateTime.now());
        treatmentCost.setCalculatedBy(calculatedBy);

        // 6. Speichere
        treatmentCost = treatmentCostRepository.save(treatmentCost);

        // 7. Erstelle PatientCostHistory-Einträge für alle Patienten im Zeitslot
        createPatientCostHistory(treatmentsInSlot, treatmentCost, costPerPatient);

        return treatmentCost;
    }

    /**
     * Erstellt PatientCostHistory-Einträge für alle Patienten im Zeitslot.
     */
    private void createPatientCostHistory(
            List<Treatment> treatmentsInSlot,
            TreatmentCost treatmentCost,
            BigDecimal costPerPatient) {

        for (Treatment treatment : treatmentsInSlot) {
            PatientCostHistory history = new PatientCostHistory();
            history.setPatient(treatment.getTreatmentPlan().getPatient());
            history.setTreatment(treatment);
            history.setTreatmentCost(treatmentCost);
            history.setCostAmount(costPerPatient);
            history.setTreatmentDate(treatment.getSurgicalCenterTimeSlot().getDate());
            history.setSurgicalCenter(treatment.getSurgicalCenterTimeSlot().getSurgicalCenter());

            patientCostHistoryRepository.save(history);
        }
    }

    /**
     * Findet Kosten für eine Behandlung.
     */
    @Transactional(readOnly = true)
    public Optional<TreatmentCost> findByTreatmentId(Long treatmentId) {
        return treatmentCostRepository.findByTreatmentId(treatmentId);
    }

    /**
     * Findet Kostenhistorie für einen Patienten.
     */
    @Transactional(readOnly = true)
    public List<PatientCostHistory> findPatientCostHistory(Integer patientId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return List.of();
        }

        return patientCostHistoryRepository.findByPatientId(patientId, institutionId);
    }

    /**
     * Berechnet Gesamtkosten für einen Patienten.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCostsByPatientId(Integer patientId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return BigDecimal.ZERO;
        }

        return patientCostHistoryRepository.getTotalCostsByPatientId(patientId, institutionId)
                .orElse(BigDecimal.ZERO);
    }

    /**
     * Aggregiert monatliche Kosten für die aktuelle Institution.
     * Returns a map of "YYYY-MM" to total cost.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> getMonthlyCosts(LocalDate startDate) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return Map.of();
        }
        List<Object[]> results = treatmentCostRepository.getMonthlyCosts(institutionId, startDate);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }
}

