package de.bbajor.pvs.kbv.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;
import de.bbajor.pvs.kbv.model.KbvIcdEntry;
import de.bbajor.pvs.kbv.model.KbvInsurance;
import de.bbajor.pvs.kbv.repository.KbvCostCarrierRepository;
import de.bbajor.pvs.kbv.repository.KbvIcdEntryRepository;
import de.bbajor.pvs.kbv.repository.KbvInsuranceRepository;

@Service
public class KbvChangeDetectionService {

    private static final Logger log = LoggerFactory.getLogger(KbvChangeDetectionService.class);

    private final KbvIcdEntryRepository icdEntryRepository;
    private final KbvCostCarrierRepository costCarrierRepository;
    private final KbvInsuranceRepository insuranceRepository;

    public KbvChangeDetectionService(
            KbvIcdEntryRepository icdEntryRepository,
            KbvCostCarrierRepository costCarrierRepository,
            KbvInsuranceRepository insuranceRepository) {
        this.icdEntryRepository = icdEntryRepository;
        this.costCarrierRepository = costCarrierRepository;
        this.insuranceRepository = insuranceRepository;
    }

    public ChangeComparison compareQuarters(String fromQuarter, String toQuarter) {
        log.info("Comparing quarters: {} -> {}", fromQuarter, toQuarter);

        ChangeComparison comparison = new ChangeComparison(fromQuarter, toQuarter);

        // Compare ICD entries
        List<KbvIcdEntry> fromIcd = icdEntryRepository.findByQuarter(fromQuarter);
        List<KbvIcdEntry> toIcd = icdEntryRepository.findByQuarter(toQuarter);
        comparison.setIcdChanges(compareIcdEntries(fromIcd, toIcd));

        // Compare cost carriers
        List<KbvCostCarrier> fromCarriers = costCarrierRepository.findByQuarter(fromQuarter);
        List<KbvCostCarrier> toCarriers = costCarrierRepository.findByQuarter(toQuarter);
        comparison.setCostCarrierChanges(compareCostCarriers(fromCarriers, toCarriers));

        // Compare insurances
        List<KbvInsurance> fromInsurances = insuranceRepository.findByQuarter(fromQuarter);
        List<KbvInsurance> toInsurances = insuranceRepository.findByQuarter(toQuarter);
        comparison.setInsuranceChanges(compareInsurances(fromInsurances, toInsurances));

        return comparison;
    }

    private List<ChangeRecord<KbvIcdEntry>> compareIcdEntries(List<KbvIcdEntry> from, List<KbvIcdEntry> to) {
        Map<String, KbvIcdEntry> fromMap = from.stream()
                .collect(Collectors.toMap(KbvIcdEntry::getCode, e -> e, (e1, e2) -> e1));

        Map<String, KbvIcdEntry> toMap = to.stream()
                .collect(Collectors.toMap(KbvIcdEntry::getCode, e -> e, (e1, e2) -> e1));

        List<ChangeRecord<KbvIcdEntry>> changes = new ArrayList<>();

        // Find new entries
        for (KbvIcdEntry entry : to) {
            if (!fromMap.containsKey(entry.getCode())) {
                changes.add(new ChangeRecord<>(ChangeType.NEW, entry, null));
            }
        }

        // Find deleted entries
        for (KbvIcdEntry entry : from) {
            if (!toMap.containsKey(entry.getCode())) {
                changes.add(new ChangeRecord<>(ChangeType.DELETED, null, entry));
            }
        }

        // Find modified entries
        for (KbvIcdEntry toEntry : to) {
            KbvIcdEntry fromEntry = fromMap.get(toEntry.getCode());
            if (fromEntry != null && !entriesEqual(fromEntry, toEntry)) {
                changes.add(new ChangeRecord<>(ChangeType.MODIFIED, toEntry, fromEntry));
            }
        }

        return changes;
    }

    private List<ChangeRecord<KbvCostCarrier>> compareCostCarriers(
            List<KbvCostCarrier> from, List<KbvCostCarrier> to) {
        Map<String, KbvCostCarrier> fromMap = from.stream()
                .collect(Collectors.toMap(KbvCostCarrier::getCode, c -> c, (c1, c2) -> c1));

        Map<String, KbvCostCarrier> toMap = to.stream()
                .collect(Collectors.toMap(KbvCostCarrier::getCode, c -> c, (c1, c2) -> c1));

        List<ChangeRecord<KbvCostCarrier>> changes = new ArrayList<>();

        for (KbvCostCarrier carrier : to) {
            if (!fromMap.containsKey(carrier.getCode())) {
                changes.add(new ChangeRecord<>(ChangeType.NEW, carrier, null));
            }
        }

        for (KbvCostCarrier carrier : from) {
            if (!toMap.containsKey(carrier.getCode())) {
                changes.add(new ChangeRecord<>(ChangeType.DELETED, null, carrier));
            }
        }

        for (KbvCostCarrier toCarrier : to) {
            KbvCostCarrier fromCarrier = fromMap.get(toCarrier.getCode());
            if (fromCarrier != null && !carriersEqual(fromCarrier, toCarrier)) {
                changes.add(new ChangeRecord<>(ChangeType.MODIFIED, toCarrier, fromCarrier));
            }
        }

        return changes;
    }

    private List<ChangeRecord<KbvInsurance>> compareInsurances(
            List<KbvInsurance> from, List<KbvInsurance> to) {
        Map<String, KbvInsurance> fromMap = from.stream()
                .collect(Collectors.toMap(KbvInsurance::getCode, i -> i, (i1, i2) -> i1));

        Map<String, KbvInsurance> toMap = to.stream()
                .collect(Collectors.toMap(KbvInsurance::getCode, i -> i, (i1, i2) -> i1));

        List<ChangeRecord<KbvInsurance>> changes = new ArrayList<>();

        for (KbvInsurance insurance : to) {
            if (!fromMap.containsKey(insurance.getCode())) {
                changes.add(new ChangeRecord<>(ChangeType.NEW, insurance, null));
            }
        }

        for (KbvInsurance insurance : from) {
            if (!toMap.containsKey(insurance.getCode())) {
                changes.add(new ChangeRecord<>(ChangeType.DELETED, null, insurance));
            }
        }

        for (KbvInsurance toInsurance : to) {
            KbvInsurance fromInsurance = fromMap.get(toInsurance.getCode());
            if (fromInsurance != null && !insurancesEqual(fromInsurance, toInsurance)) {
                changes.add(new ChangeRecord<>(ChangeType.MODIFIED, toInsurance, fromInsurance));
            }
        }

        return changes;
    }

    private boolean entriesEqual(KbvIcdEntry e1, KbvIcdEntry e2) {
        return e1.getCode().equals(e2.getCode()) &&
                e1.getTextContent().equals(e2.getTextContent()) &&
                java.util.Objects.equals(e1.getCodingType(), e2.getCodingType()) &&
                java.util.Objects.equals(e1.getPrintIndicator(), e2.getPrintIndicator());
    }

    private boolean carriersEqual(KbvCostCarrier c1, KbvCostCarrier c2) {
        return c1.getCode().equals(c2.getCode()) && c1.getName().equals(c2.getName());
    }

    private boolean insurancesEqual(KbvInsurance i1, KbvInsurance i2) {
        return i1.getCode().equals(i2.getCode()) && i1.getName().equals(i2.getName());
    }

    public static class ChangeComparison {
        private final String fromQuarter;
        private final String toQuarter;
        private List<ChangeRecord<KbvIcdEntry>> icdChanges = new ArrayList<>();
        private List<ChangeRecord<KbvCostCarrier>> costCarrierChanges = new ArrayList<>();
        private List<ChangeRecord<KbvInsurance>> insuranceChanges = new ArrayList<>();

        public ChangeComparison(String fromQuarter, String toQuarter) {
            this.fromQuarter = fromQuarter;
            this.toQuarter = toQuarter;
        }

        public String getFromQuarter() {
            return fromQuarter;
        }

        public String getToQuarter() {
            return toQuarter;
        }

        public List<ChangeRecord<KbvIcdEntry>> getIcdChanges() {
            return icdChanges;
        }

        public void setIcdChanges(List<ChangeRecord<KbvIcdEntry>> icdChanges) {
            this.icdChanges = icdChanges;
        }

        public List<ChangeRecord<KbvCostCarrier>> getCostCarrierChanges() {
            return costCarrierChanges;
        }

        public void setCostCarrierChanges(List<ChangeRecord<KbvCostCarrier>> costCarrierChanges) {
            this.costCarrierChanges = costCarrierChanges;
        }

        public List<ChangeRecord<KbvInsurance>> getInsuranceChanges() {
            return insuranceChanges;
        }

        public void setInsuranceChanges(List<ChangeRecord<KbvInsurance>> insuranceChanges) {
            this.insuranceChanges = insuranceChanges;
        }
    }

    public static class ChangeRecord<T> {
        private final ChangeType type;
        private final T newValue;
        private final T oldValue;

        public ChangeRecord(ChangeType type, T newValue, T oldValue) {
            this.type = type;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        public ChangeType getType() {
            return type;
        }

        public T getNewValue() {
            return newValue;
        }

        public T getOldValue() {
            return oldValue;
        }
    }

    public enum ChangeType {
        NEW, MODIFIED, DELETED
    }
}
