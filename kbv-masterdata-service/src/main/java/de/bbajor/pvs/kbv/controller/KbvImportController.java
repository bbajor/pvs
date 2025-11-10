package de.bbajor.pvs.kbv.controller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.kbv.model.KbvImportHistory;
import de.bbajor.pvs.kbv.repository.KbvImportHistoryRepository;
import de.bbajor.pvs.kbv.service.KbvXmlImporter;

@RestController
@RequestMapping("/api/kbv/import")
public class KbvImportController {

    private static final Logger log = LoggerFactory.getLogger(KbvImportController.class);

    private final KbvXmlImporter xmlImporter;
    private final KbvImportHistoryRepository importHistoryRepository;

    public KbvImportController(
            KbvXmlImporter xmlImporter,
            KbvImportHistoryRepository importHistoryRepository) {
        this.xmlImporter = xmlImporter;
        this.importHistoryRepository = importHistoryRepository;
    }

    @PostMapping("/trigger")
    public ResponseEntity<ImportTriggerResponse> triggerImport(@RequestBody ImportTriggerRequest request) {
        try {
            log.info("Import triggered: quarter={}, version={}, path={}", 
                    request.getQuarter(), request.getVersion(), request.getFilePath());

            Path filePath = Path.of(request.getFilePath());
            KbvXmlImporter.ImportResult result = xmlImporter.importXmlFile(
                    filePath, 
                    request.getQuarter(), 
                    request.getVersion());

            ImportTriggerResponse response = new ImportTriggerResponse(
                    result.isSuccess(),
                    result.getRecordsImported(),
                    result.getType().name(),
                    result.getErrorMessage());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Import failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ImportTriggerResponse(false, 0, "UNKNOWN", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<KbvImportHistory>> getImportHistory(
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String status) {
        try {
            List<KbvImportHistory> history;
            if (quarter != null && !quarter.isBlank()) {
                history = importHistoryRepository.findByQuarterOrderByStartedAtDesc(quarter);
            } else if (status != null && !status.isBlank()) {
                try {
                    KbvImportHistory.ImportStatus statusEnum = KbvImportHistory.ImportStatus.valueOf(status.toUpperCase());
                    history = importHistoryRepository.findByStatusOrderByStartedAtDesc(statusEnum);
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().build();
                }
            } else {
                history = importHistoryRepository.findAllOrderByStartedAtDesc();
            }
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching import history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    public static class ImportTriggerRequest {
        private String filePath;
        private String quarter;
        private String version;

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public String getQuarter() {
            return quarter;
        }

        public void setQuarter(String quarter) {
            this.quarter = quarter;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ImportTriggerResponse {
        private final boolean success;
        private final int recordsImported;
        private final String importType;
        private final String errorMessage;

        public ImportTriggerResponse(boolean success, int recordsImported, String importType, String errorMessage) {
            this.success = success;
            this.recordsImported = recordsImported;
            this.importType = importType;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getRecordsImported() {
            return recordsImported;
        }

        public String getImportType() {
            return importType;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
