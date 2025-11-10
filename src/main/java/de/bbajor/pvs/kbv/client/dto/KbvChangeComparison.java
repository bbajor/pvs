package de.bbajor.pvs.kbv.client.dto;

import java.util.List;

import lombok.Data;

@Data
public class KbvChangeComparison {
    private String fromQuarter;
    private String toQuarter;
    private List<ChangeRecord<KbvIcdEntryDto>> icdChanges;
    private List<ChangeRecord<KbvCostCarrierDto>> costCarrierChanges;
    private List<ChangeRecord<KbvInsuranceDto>> insuranceChanges;

    @Data
    public static class ChangeRecord<T> {
        private String type; // NEW, MODIFIED, DELETED
        private T newValue;
        private T oldValue;
    }
}
