package de.bbajor.pvs.kbv.service;

import java.util.List;

import org.springframework.stereotype.Service;

import de.bbajor.pvs.kbv.client.KbvServiceClient;
import de.bbajor.pvs.kbv.client.dto.KbvImportHistoryDto;

@Service
public class KbvImportService {

    private final KbvServiceClient serviceClient;

    public KbvImportService(KbvServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    public boolean triggerImport(String filePath, String quarter, String version) {
        return serviceClient.triggerImport(filePath, quarter, version);
    }

    public List<KbvImportHistoryDto> getImportHistory(String quarter) {
        return serviceClient.getImportHistory(quarter, null)
                .orElse(List.of());
    }

    public List<KbvImportHistoryDto> getImportHistoryByStatus(String status) {
        return serviceClient.getImportHistory(null, status)
                .orElse(List.of());
    }
}
