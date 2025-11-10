package de.bbajor.pvs.kbv.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.bbajor.pvs.kbv.model.KbvCostCarrier;
import de.bbajor.pvs.kbv.model.KbvIcdEntry;
import de.bbajor.pvs.kbv.model.KbvInsurance;
import de.bbajor.pvs.kbv.repository.KbvCostCarrierRepository;
import de.bbajor.pvs.kbv.repository.KbvIcdEntryRepository;
import de.bbajor.pvs.kbv.repository.KbvInsuranceRepository;
import de.bbajor.pvs.kbv.service.KbvChangeDetectionService;

@RestController
@RequestMapping("/api/kbv")
public class KbvDataController {

    private final KbvIcdEntryRepository icdEntryRepository;
    private final KbvCostCarrierRepository costCarrierRepository;
    private final KbvInsuranceRepository insuranceRepository;
    private final KbvChangeDetectionService changeDetectionService;

    public KbvDataController(
            KbvIcdEntryRepository icdEntryRepository,
            KbvCostCarrierRepository costCarrierRepository,
            KbvInsuranceRepository insuranceRepository,
            KbvChangeDetectionService changeDetectionService) {
        this.icdEntryRepository = icdEntryRepository;
        this.costCarrierRepository = costCarrierRepository;
        this.insuranceRepository = insuranceRepository;
        this.changeDetectionService = changeDetectionService;
    }

    @GetMapping("/icd")
    public ResponseEntity<List<KbvIcdEntry>> getIcdEntries(
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            LocalDate queryDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            Pageable pageable = PageRequest.of(page, size);

            List<KbvIcdEntry> entries;
            if (code != null && !code.isBlank()) {
                entries = icdEntryRepository.findActiveByCodeAndDate(code, queryDate)
                        .map(List::of)
                        .orElse(List.of());
            } else if (quarter != null && !quarter.isBlank()) {
                entries = icdEntryRepository.findActiveByQuarterAndDate(quarter, queryDate);
            } else {
                entries = icdEntryRepository.findAll(pageable).getContent();
            }

            return ResponseEntity.ok(entries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/cost-carriers")
    public ResponseEntity<List<KbvCostCarrier>> getCostCarriers(
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            LocalDate queryDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            Pageable pageable = PageRequest.of(page, size);

            List<KbvCostCarrier> carriers;
            if (code != null && !code.isBlank()) {
                carriers = costCarrierRepository.findActiveByCodeAndDate(code, queryDate)
                        .map(List::of)
                        .orElse(List.of());
            } else if (quarter != null && !quarter.isBlank()) {
                carriers = costCarrierRepository.findActiveByQuarterAndDate(quarter, queryDate);
            } else {
                carriers = costCarrierRepository.findAll(pageable).getContent();
            }

            return ResponseEntity.ok(carriers);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/insurances")
    public ResponseEntity<List<KbvInsurance>> getInsurances(
            @RequestParam(required = false) String quarter,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            LocalDate queryDate = date != null ? LocalDate.parse(date) : LocalDate.now();
            Pageable pageable = PageRequest.of(page, size);

            List<KbvInsurance> insurances;
            if (code != null && !code.isBlank()) {
                insurances = insuranceRepository.findActiveByCodeAndDate(code, queryDate)
                        .map(List::of)
                        .orElse(List.of());
            } else if (quarter != null && !quarter.isBlank()) {
                insurances = insuranceRepository.findActiveByQuarterAndDate(quarter, queryDate);
            } else {
                insurances = insuranceRepository.findAll(pageable).getContent();
            }

            return ResponseEntity.ok(insurances);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/changes")
    public ResponseEntity<KbvChangeDetectionService.ChangeComparison> getChanges(
            @RequestParam String fromQuarter,
            @RequestParam String toQuarter) {
        try {
            KbvChangeDetectionService.ChangeComparison comparison = 
                    changeDetectionService.compareQuarters(fromQuarter, toQuarter);
            return ResponseEntity.ok(comparison);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
