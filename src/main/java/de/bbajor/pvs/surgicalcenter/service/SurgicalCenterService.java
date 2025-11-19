package de.bbajor.pvs.surgicalcenter.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterRepository;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;

@Service
public class SurgicalCenterService {

    @Autowired
    protected SurgicalCenterTimeSlotRepository timeSlotRepository;
    @Autowired
    protected SurgicalCenterRepository surgicalCenterRepository;
    @Autowired
    protected SurgicalCenterMapper mapper;
    @Autowired
    protected InstitutionRepository institutionRepository;

    @Transactional(readOnly = true)
    public List<SurgicalCenter> findAll() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            // Return only surgical centers for current institution
            return surgicalCenterRepository.findByInstitutionId(institutionId);
        }
        // No institution context - return empty list to enforce data isolation
        return List.of();
    }

    @Transactional(readOnly = true)
    public SurgicalCenter findByIdWithDetails(Integer id) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access surgical center data without institution context");
        }
        // Find by ID and verify it belongs to current institution
        SurgicalCenter center = surgicalCenterRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Surgical center not found: " + id));
        
        // Verify institution match for data isolation
        if (center.getInstitution() == null || !center.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Surgical center does not belong to current institution");
        }
        
        return center;
    }

    @Transactional
    public SurgicalCenter saveSurgicalCenter(SurgicalCenter entityToSave) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot save surgical center without institution context");
        }
        
        // Ensure institution is set for new surgical centers
        if (entityToSave.getInstitution() == null) {
            // Load institution from context
            Institution institution = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
            entityToSave.setInstitution(institution);
        } else {
            // Verify institution match
            if (!entityToSave.getInstitution().getId().equals(institutionId)) {
                throw new IllegalStateException("Surgical center institution does not match current institution context");
            }
        }
        
        // Verify institution match for existing surgical centers
        if (entityToSave.getId() != null && entityToSave.getId() > 0) {
            SurgicalCenter existing = surgicalCenterRepository.findById(entityToSave.getId()).orElse(null);
            if (existing != null && existing.getInstitution() != null 
                    && !existing.getInstitution().getId().equals(institutionId)) {
                throw new IllegalStateException("Cannot modify surgical center from another institution");
            }
        }
        
        if (entityToSave.getId() != null && entityToSave.getId() == 0L) {
            entityToSave.setId(null);
        }
        return surgicalCenterRepository.save(entityToSave);
    }

    @Transactional
    private List<SurgicalCenterTimeSlot> saveTimeSlotsForExistingSurgicalCenter(
            List<SurgicalCenterTimeSlot> newTimeSlots,
            SurgicalCenter surgicalCenter) {
        Objects.requireNonNull(surgicalCenter);
        Objects.requireNonNull(newTimeSlots);
        if (newTimeSlots.isEmpty()) {
            return Collections.emptyList();
        }

        SurgicalCenter savedSurgicalCenter = surgicalCenterRepository.getReferenceById(surgicalCenter.getId());
        List<SurgicalCenterTimeSlot> uniqueTimeSlots = newTimeSlots.stream()
            .filter(slot -> !timeSlotRepository.existsBySurgicalCenterAndDateAndStartTimeAndEndTime(
                savedSurgicalCenter, slot.getDate(), slot.getStartTime(), slot.getEndTime()))
            .peek(e -> e.setSurgicalCenter(savedSurgicalCenter))
            .toList();
            
        return timeSlotRepository.saveAll(uniqueTimeSlots);
    }

    @Transactional
    protected List<SurgicalCenterTimeSlot> findTimeSlotsBySurgicalCenterId(Integer id) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access time slots without institution context");
        }
        
        Optional<SurgicalCenter> surgicalCenter = surgicalCenterRepository.findById(id);
        if (!surgicalCenter.isPresent()) {
            return new ArrayList<>();
        }
        
        // Verify institution match for data isolation
        if (surgicalCenter.get().getInstitution() == null || 
                !surgicalCenter.get().getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Surgical center does not belong to current institution");
        }

        List<SurgicalCenterTimeSlot> timeSlots = timeSlotRepository
                .findBySurgicalCenterAndDateGreaterThanEqual(surgicalCenter.get(), LocalDate.now());
        return timeSlots;
    }

    @Transactional(readOnly = true)
    public Collection<SurgicalCenterTimeSlot> findAvailableTimeSlotsFilteredBy(LocalDate periodStart,
            TimePeriod timePeriod,
            Integer surgicalCenterId) {

        Long institutionId = InstitutionContext.getInstitutionId();
        System.out.println("=== DEBUG: SurgicalCenterService.findAvailableTimeSlotsFilteredBy ===");
        System.out.println("InstitutionContext ID: " + institutionId);
        System.out.println("PeriodStart: " + periodStart);
        System.out.println("TimePeriod: " + timePeriod);
        System.out.println("SurgicalCenterId: " + surgicalCenterId);
        
        if (institutionId == null) {
            // No institution context - return empty list to enforce data isolation
            System.out.println("FEHLER: Kein InstitutionContext gesetzt!");
            return Collections.emptyList();
        }

        if (periodStart == null || timePeriod == null) {
            System.out.println("FEHLER: periodStart oder timePeriod ist null!");
            return Collections.emptyList();
        }

        LocalDate start = periodStart.isAfter(LocalDate.now()) ? periodStart : LocalDate.now();
        LocalDate end = timePeriod.calculateEndDate(start);
        System.out.println("Query-Zeitraum: " + start + " bis " + end);

        Sort sort = Sort.by("date").ascending().and(Sort.by("startTime").ascending());
        List<SurgicalCenterTimeSlot> timeSlots = new ArrayList<>();
        if (surgicalCenterId == null) {
            // If no specific surgical center, return time slots only from centers of current institution
            List<SurgicalCenter> institutionCenters = surgicalCenterRepository.findByInstitutionId(institutionId);
            System.out.println("Anzahl Behandlungsorte für Institution " + institutionId + ": " + institutionCenters.size());
            for (SurgicalCenter center : institutionCenters) {
                System.out.println("  - Behandlungsort: " + center.getId() + " (" + center.getName() + ")");
                List<SurgicalCenterTimeSlot> centerTimeSlots = timeSlotRepository
                        .findByDateBetweenAndSurgicalCenter(start, end, center, sort);
                System.out.println("    Gefundene Termine: " + centerTimeSlots.size());
                timeSlots.addAll(centerTimeSlots);
            }
        } else {
            SurgicalCenter surgicalCenter = surgicalCenterRepository.getReferenceById(surgicalCenterId);
            if (surgicalCenter != null) {
                System.out.println("Behandlungsort: " + surgicalCenter.getId() + " (" + surgicalCenter.getName() + ")");
                System.out.println("Behandlungsort Institution ID: " + (surgicalCenter.getInstitution() != null ? surgicalCenter.getInstitution().getId() : "NULL"));
                
                // Verify institution match for data isolation
                if (surgicalCenter.getInstitution() == null || 
                        !surgicalCenter.getInstitution().getId().equals(institutionId)) {
                    System.out.println("FEHLER: Behandlungsort gehört nicht zur aktuellen Institution!");
                    System.out.println("  Erwartet: " + institutionId);
                    System.out.println("  Gefunden: " + (surgicalCenter.getInstitution() != null ? surgicalCenter.getInstitution().getId() : "NULL"));
                    throw new IllegalStateException("Surgical center does not belong to current institution");
                }
                
                // Verwende neue Query mit direktem Institution-Filter und isAvailable-Filter
                System.out.println("Query: findAvailableTimeSlotsBySurgicalCenterAndInstitution(" + 
                        surgicalCenter.getId() + ", " + institutionId + ", " + start + ", " + end + ")");
                timeSlots = timeSlotRepository
                        .findAvailableTimeSlotsBySurgicalCenterAndInstitution(
                                surgicalCenter.getId(), institutionId, start, end);
                System.out.println("Gefundene verfügbare Termine: " + timeSlots.size());
                if (!timeSlots.isEmpty()) {
                    System.out.println("Erster Termin: " + timeSlots.get(0).getDate() + " " + timeSlots.get(0).getStartTime() + 
                            " (isAvailable: " + timeSlots.get(0).isAvailable() + ")");
                }
            } else {
                System.out.println("FEHLER: Behandlungsort mit ID " + surgicalCenterId + " nicht gefunden!");
            }
        }
        System.out.println("Gesamt gefundene Termine: " + timeSlots.size());
        System.out.println("=== ENDE DEBUG: SurgicalCenterService ===");
        return timeSlots;
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenter> getSurgicalCenters() {
        // Use the same method as findAll() for consistency
        return findAll();
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenter> getSurgicalCentersForInstitution(Long institutionId) {
        if (institutionId == null) {
            return List.of();
        }
        return surgicalCenterRepository.findByInstitutionId(institutionId);
    }

    @Transactional
    public SurgicalCenter saveTimeSlotsAndSurgicalCenter(List<SurgicalCenterTimeSlot> newTimeSlots,
            SurgicalCenter surgicalCenter) {
        Objects.requireNonNull(surgicalCenter);
        
        // Ensure institution is set before saving
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot save surgical center without institution context");
        }
        
        if (surgicalCenter.getInstitution() == null) {
            Institution institution = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
            surgicalCenter.setInstitution(institution);
        } else if (!surgicalCenter.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Surgical center institution does not match current institution context");
        }
        
        // Clear the availableTimeSlots to prevent cascading saves
        if (surgicalCenter.getAvailableTimeSlots() != null) {
            surgicalCenter.setAvailableTimeSlots(Collections.emptyList());
        }

        // Save the surgical center first without any time slots (use saveSurgicalCenter for consistency)
        SurgicalCenter savedEntity = saveSurgicalCenter(surgicalCenter);
        
        // Now save the time slots separately and update the surgical center's reference
        List<SurgicalCenterTimeSlot> savedTimeSlots = saveTimeSlotsForExistingSurgicalCenter(newTimeSlots,
                savedEntity);
        savedEntity.setAvailableTimeSlots(savedTimeSlots);
        
        return surgicalCenterRepository.getReferenceById(savedEntity.getId());
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenterTimeSlot> getTimeSlotsBySurgicalCenterIdWithTreatmentCount(Integer surgicalCenterId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access time slots without institution context");
        }
        
        // Verify surgical center belongs to current institution
        SurgicalCenter center = surgicalCenterRepository.findById(surgicalCenterId).orElse(null);
        if (center == null) {
            throw new RuntimeException("Surgical center not found: " + surgicalCenterId);
        }
        
        if (center.getInstitution() == null || !center.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Surgical center does not belong to current institution");
        }
        
        // Filter treatments by institution when counting patients
        return timeSlotRepository.findBySurgicalCenterIdWithTreatmentCount(surgicalCenterId, institutionId);
    }

    public List<SurgicalCenterTimeSlot> getNewTimeSlotsContainingNotApprovedTreatments(List<Long> timeSlotIds) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            // No institution context - return empty list to enforce data isolation
            return List.of();
        }
        
        // Get all time slots with not approved treatments for this institution
        // The query already filters by institution
        if (timeSlotIds == null || timeSlotIds.isEmpty()) {
            return timeSlotRepository.findAllContainingNotApprovedTreatments(institutionId);
        } else {
            return timeSlotRepository.findAllContainingNotApprovedTreatmentsAndNotInTimeSlotIdList(institutionId, timeSlotIds);
        }
    }

}
