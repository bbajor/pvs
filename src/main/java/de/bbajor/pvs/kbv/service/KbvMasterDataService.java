package de.bbajor.pvs.kbv.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.kbv.client.KbvServiceClient;
import de.bbajor.pvs.kbv.client.dto.KbvChangeComparison;
import de.bbajor.pvs.kbv.client.dto.KbvCostCarrierDto;
import de.bbajor.pvs.kbv.client.dto.KbvIcdEntryDto;
import de.bbajor.pvs.kbv.client.dto.KbvImportHistoryDto;
import de.bbajor.pvs.kbv.client.dto.KbvInsuranceDto;

@Service
public class KbvMasterDataService {

    private final KbvServiceClient serviceClient;

    public KbvMasterDataService(KbvServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    public List<KbvIcdEntryDto> getIcdEntries(String quarter, String code) {
        return serviceClient.getIcdEntries(quarter, code, LocalDate.now())
                .orElse(List.of());
    }

    public List<KbvCostCarrierDto> getCostCarriers(String quarter, String code) {
        return serviceClient.getCostCarriers(quarter, code, LocalDate.now())
                .orElse(List.of());
    }

    public List<KbvInsuranceDto> getInsurances(String quarter, String code) {
        return serviceClient.getInsurances(quarter, code, LocalDate.now())
                .orElse(List.of());
    }

    public List<KbvImportHistoryDto> getImportHistory(String quarter, String status) {
        return serviceClient.getImportHistory(quarter, status)
                .orElse(List.of());
    }

    public boolean triggerImport(String filePath, String quarter, String version) {
        return serviceClient.triggerImport(filePath, quarter, version);
    }

    public Optional<KbvChangeComparison> getChanges(String fromQuarter, String toQuarter) {
        return serviceClient.getChanges(fromQuarter, toQuarter);
    }
}
