package de.bbajor.pvs.kbv.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;
import de.bbajor.pvs.kbv.model.KbvIcdEntry;
import de.bbajor.pvs.kbv.model.KbvInsurance;
import de.bbajor.pvs.kbv.repository.KbvCostCarrierRepository;
import de.bbajor.pvs.kbv.repository.KbvIcdEntryRepository;
import de.bbajor.pvs.kbv.repository.KbvInsuranceRepository;

@Service
public class KbvHistoricizationService {

    private static final Logger log = LoggerFactory.getLogger(KbvHistoricizationService.class);

    private final KbvIcdEntryRepository icdEntryRepository;
    private final KbvCostCarrierRepository costCarrierRepository;
    private final KbvInsuranceRepository insuranceRepository;

    public KbvHistoricizationService(
            KbvIcdEntryRepository icdEntryRepository,
            KbvCostCarrierRepository costCarrierRepository,
            KbvInsuranceRepository insuranceRepository) {
        this.icdEntryRepository = icdEntryRepository;
        this.costCarrierRepository = costCarrierRepository;
        this.insuranceRepository = insuranceRepository;
    }

    @Transactional
    public void deactivatePreviousVersions(String quarter, LocalDate newValidFrom) {
        log.info("Deactivating previous versions for quarter {} with new valid_from {}", quarter, newValidFrom);

        // Deactivate ICD entries
        List<KbvIcdEntry> activeIcdEntries = icdEntryRepository.findByQuarter(quarter)
                .stream()
                .filter(e -> e.getValidTo() == null)
                .toList();
        for (KbvIcdEntry entry : activeIcdEntries) {
            entry.setValidTo(newValidFrom.minusDays(1));
        }
        icdEntryRepository.saveAll(activeIcdEntries);

        // Deactivate cost carriers
        List<KbvCostCarrier> activeCostCarriers = costCarrierRepository.findByQuarter(quarter)
                .stream()
                .filter(c -> c.getValidTo() == null)
                .toList();
        for (KbvCostCarrier carrier : activeCostCarriers) {
            carrier.setValidTo(newValidFrom.minusDays(1));
        }
        costCarrierRepository.saveAll(activeCostCarriers);

        // Deactivate insurances
        List<KbvInsurance> activeInsurances = insuranceRepository.findByQuarter(quarter)
                .stream()
                .filter(i -> i.getValidTo() == null)
                .toList();
        for (KbvInsurance insurance : activeInsurances) {
            insurance.setValidTo(newValidFrom.minusDays(1));
        }
        insuranceRepository.saveAll(activeInsurances);

        log.info("Deactivated {} ICD entries, {} cost carriers, {} insurances",
                activeIcdEntries.size(), activeCostCarriers.size(), activeInsurances.size());
    }

    public LocalDate calculateValidFrom(String quarter) {
        // Quarter format: YYYY-Q1, YYYY-Q2, YYYY-Q3, YYYY-Q4
        String[] parts = quarter.split("-Q");
        if (parts.length != 2) {
            log.warn("Invalid quarter format: {}, using current date", quarter);
            return LocalDate.now();
        }

        int year = Integer.parseInt(parts[0]);
        int quarterNum = Integer.parseInt(parts[1]);

        // Q1: Jan 1, Q2: Apr 1, Q3: Jul 1, Q4: Oct 1
        int month = (quarterNum - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }
}
