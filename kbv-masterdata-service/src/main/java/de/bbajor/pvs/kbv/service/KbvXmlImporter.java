package de.bbajor.pvs.kbv.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;
import de.bbajor.pvs.kbv.model.KbvIcdEntry;
import de.bbajor.pvs.kbv.model.KbvImportHistory;
import de.bbajor.pvs.kbv.model.KbvInsurance;
import de.bbajor.pvs.kbv.repository.KbvCostCarrierRepository;
import de.bbajor.pvs.kbv.repository.KbvIcdEntryRepository;
import de.bbajor.pvs.kbv.repository.KbvImportHistoryRepository;
import de.bbajor.pvs.kbv.repository.KbvInsuranceRepository;

@Service
public class KbvXmlImporter {

    private static final Logger log = LoggerFactory.getLogger(KbvXmlImporter.class);

    private final KbvXmlParser xmlParser;
    private final KbvHistoricizationService historicizationService;
    private final KbvIcdEntryRepository icdEntryRepository;
    private final KbvCostCarrierRepository costCarrierRepository;
    private final KbvInsuranceRepository insuranceRepository;
    private final KbvImportHistoryRepository importHistoryRepository;

    public KbvXmlImporter(
            KbvXmlParser xmlParser,
            KbvHistoricizationService historicizationService,
            KbvIcdEntryRepository icdEntryRepository,
            KbvCostCarrierRepository costCarrierRepository,
            KbvInsuranceRepository insuranceRepository,
            KbvImportHistoryRepository importHistoryRepository) {
        this.xmlParser = xmlParser;
        this.historicizationService = historicizationService;
        this.icdEntryRepository = icdEntryRepository;
        this.costCarrierRepository = costCarrierRepository;
        this.insuranceRepository = insuranceRepository;
        this.importHistoryRepository = importHistoryRepository;
    }

    @Transactional
    public void importFromDirectory(Path root) throws IOException {
        if (!Files.exists(root)) {
            log.warn("KBV import directory {} does not exist", root);
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(p -> p.toString().endsWith(".xml"))
                    .forEach(this::importXmlFile);
        }
    }

    @Transactional
    public ImportResult importXmlFile(Path file, String quarter, String version) {
        String filename = file.getFileName().toString().toLowerCase();
        log.info("KBV import: processing {}", file);

        if (quarter == null) {
            quarter = xmlParser.extractQuarter(filename);
        }

        try (InputStream inputStream = Files.newInputStream(file)) {
            if (filename.contains("icd") || filename.contains("gm")) {
                return importIcdEntries(inputStream, quarter, version, file.toString());
            } else if (filename.contains("kostentraeger") || filename.contains("cost") || filename.contains("carrier")) {
                return importCostCarriers(inputStream, quarter, version, file.toString());
            } else if (filename.contains("versicherung") || filename.contains("insurance")) {
                return importInsurances(inputStream, quarter, version, file.toString());
            } else {
                log.warn("Unknown file type, trying ICD parser: {}", filename);
                return importIcdEntries(inputStream, quarter, version, file.toString());
            }
        } catch (Exception e) {
            log.error("Error importing file: {}", file, e);
            return new ImportResult(KbvImportHistory.ImportType.FULL, 0, false, e.getMessage());
        }
    }

    @Transactional
    public ImportResult importIcdEntries(InputStream xmlStream, String quarter, String version, String source) {
        KbvImportHistory history = createImportHistory(quarter, version, KbvImportHistory.ImportType.ICD, source);
        try {
            List<KbvIcdEntry> entries = xmlParser.parseIcdEntries(xmlStream, quarter, version);
            if (!entries.isEmpty()) {
                LocalDateTime validFrom = historicizationService.calculateValidFrom(quarter).atStartOfDay();
                historicizationService.deactivatePreviousVersions(quarter, validFrom.toLocalDate());
                icdEntryRepository.saveAll(entries);
            }
            history.setStatus(KbvImportHistory.ImportStatus.SUCCESS);
            history.setRecordsImported(entries.size());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepository.save(history);
            log.info("Imported {} ICD entries for quarter {}", entries.size(), quarter);
            return new ImportResult(KbvImportHistory.ImportType.ICD, entries.size(), true, null);
        } catch (Exception e) {
            history.setStatus(KbvImportHistory.ImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepository.save(history);
            throw new RuntimeException("Failed to import ICD entries", e);
        }
    }

    @Transactional
    public ImportResult importCostCarriers(InputStream xmlStream, String quarter, String version, String source) {
        KbvImportHistory history = createImportHistory(quarter, version, KbvImportHistory.ImportType.COST_CARRIER, source);
        try {
            List<KbvCostCarrier> carriers = xmlParser.parseCostCarriers(xmlStream, quarter, version);
            if (!carriers.isEmpty()) {
                LocalDateTime validFrom = historicizationService.calculateValidFrom(quarter).atStartOfDay();
                historicizationService.deactivatePreviousVersions(quarter, validFrom.toLocalDate());
                costCarrierRepository.saveAll(carriers);
            }
            history.setStatus(KbvImportHistory.ImportStatus.SUCCESS);
            history.setRecordsImported(carriers.size());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepository.save(history);
            log.info("Imported {} cost carriers for quarter {}", carriers.size(), quarter);
            return new ImportResult(KbvImportHistory.ImportType.COST_CARRIER, carriers.size(), true, null);
        } catch (Exception e) {
            history.setStatus(KbvImportHistory.ImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepository.save(history);
            throw new RuntimeException("Failed to import cost carriers", e);
        }
    }

    @Transactional
    public ImportResult importInsurances(InputStream xmlStream, String quarter, String version, String source) {
        KbvImportHistory history = createImportHistory(quarter, version, KbvImportHistory.ImportType.INSURANCE, source);
        try {
            List<KbvInsurance> insurances = xmlParser.parseInsurances(xmlStream, quarter, version);
            if (!insurances.isEmpty()) {
                LocalDateTime validFrom = historicizationService.calculateValidFrom(quarter).atStartOfDay();
                historicizationService.deactivatePreviousVersions(quarter, validFrom.toLocalDate());
                insuranceRepository.saveAll(insurances);
            }
            history.setStatus(KbvImportHistory.ImportStatus.SUCCESS);
            history.setRecordsImported(insurances.size());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepository.save(history);
            log.info("Imported {} insurances for quarter {}", insurances.size(), quarter);
            return new ImportResult(KbvImportHistory.ImportType.INSURANCE, insurances.size(), true, null);
        } catch (Exception e) {
            history.setStatus(KbvImportHistory.ImportStatus.FAILED);
            history.setErrorMessage(e.getMessage());
            history.setCompletedAt(LocalDateTime.now());
            importHistoryRepository.save(history);
            throw new RuntimeException("Failed to import insurances", e);
        }
    }

    private KbvImportHistory createImportHistory(String quarter, String version, KbvImportHistory.ImportType type, String source) {
        KbvImportHistory history = new KbvImportHistory();
        history.setQuarter(quarter);
        history.setVersion(version != null ? version : "unknown");
        history.setImportType(type);
        history.setStatus(KbvImportHistory.ImportStatus.RUNNING);
        history.setStartedAt(LocalDateTime.now());
        history.setRecordsImported(0);
        return importHistoryRepository.save(history);
    }

    public static class ImportResult {
        private final KbvImportHistory.ImportType type;
        private final int recordsImported;
        private final boolean success;
        private final String errorMessage;

        public ImportResult(KbvImportHistory.ImportType type, int recordsImported, boolean success, String errorMessage) {
            this.type = type;
            this.recordsImported = recordsImported;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public KbvImportHistory.ImportType getType() {
            return type;
        }

        public int getRecordsImported() {
            return recordsImported;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
