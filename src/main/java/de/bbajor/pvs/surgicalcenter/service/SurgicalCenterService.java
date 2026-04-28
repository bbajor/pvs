package de.bbajor.pvs.surgicalcenter.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
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
import jakarta.persistence.criteria.Predicate;

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
        Long institutionId = InstitutionContext.getRequiredInstitutionId();
        return surgicalCenterRepository.findByInstitutionId(institutionId);
    }
    
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Slice<SurgicalCenter> findAll(org.springframework.data.domain.Pageable pageable) {
        Long institutionId = InstitutionContext.getRequiredInstitutionId();
        return surgicalCenterRepository.findByInstitutionId(institutionId, pageable);
    }
    
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Slice<SurgicalCenter> findAllBy(String searchTerm, org.springframework.data.domain.Pageable pageable) {
        Long institutionId = InstitutionContext.getRequiredInstitutionId();
        
        // Wenn kein Suchbegriff, verwende findAll
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll(pageable);
        }
        
        // Erstelle Specification für Suche
        Specification<SurgicalCenter> spec = (root, query, cb) -> {
            Predicate institutionPredicate = cb.equal(root.get("institution").get("id"), institutionId);
            
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            
            // Suche in String-Feldern - nur wenn Feld nicht null ist
            Predicate namePredicate = cb.and(
                root.get("name").isNotNull(),
                cb.like(cb.lower(root.get("name")), searchPattern)
            );
            Predicate contactPredicate = cb.and(
                root.get("contact").isNotNull(),
                cb.like(cb.lower(root.get("contact")), searchPattern)
            );
            Predicate phonePredicate = cb.and(
                root.get("phone").isNotNull(),
                cb.like(cb.lower(root.get("phone")), searchPattern)
            );
            Predicate phoneContactPredicate = cb.and(
                root.get("phoneContact").isNotNull(),
                cb.like(cb.lower(root.get("phoneContact")), searchPattern)
            );
            Predicate emailPredicate = cb.and(
                root.get("email").isNotNull(),
                cb.like(cb.lower(root.get("email")), searchPattern)
            );
            
            // Suche in der Adresse - nur wenn Adresse und Feld nicht null sind
            jakarta.persistence.criteria.Path<?> addressPath = root.get("address");
            Predicate streetPredicate = cb.and(
                addressPath.isNotNull(),
                addressPath.get("street").isNotNull(),
                cb.like(cb.lower(addressPath.get("street")), searchPattern)
            );
            Predicate cityPredicate = cb.and(
                addressPath.isNotNull(),
                addressPath.get("city").isNotNull(),
                cb.like(cb.lower(addressPath.get("city")), searchPattern)
            );
            
            Predicate addressPredicate = cb.or(streetPredicate, cityPredicate);
            
            Predicate searchPredicate = cb.or(
                namePredicate,
                contactPredicate,
                phonePredicate,
                phoneContactPredicate,
                emailPredicate,
                addressPredicate
            );
            
            return cb.and(institutionPredicate, searchPredicate);
        };
        
        return surgicalCenterRepository.findAll(spec, pageable);
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
            .peek(e -> {
                e.setSurgicalCenter(savedSurgicalCenter);
                if (!e.isAvailable()) {
                    e.setAvailable(true);
                }
            })
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

        if (institutionId == null) {
            return Collections.emptyList();
        }

        if (periodStart == null || timePeriod == null) {
            return Collections.emptyList();
        }

        LocalDate start = periodStart.isAfter(LocalDate.now()) ? periodStart : LocalDate.now();
        LocalDate end = timePeriod.calculateEndDate(start);

        List<SurgicalCenterTimeSlot> timeSlots = new ArrayList<>();
        if (surgicalCenterId == null) {
            List<SurgicalCenter> institutionCenters = surgicalCenterRepository.findByInstitutionId(institutionId);
            for (SurgicalCenter center : institutionCenters) {
                timeSlots.addAll(timeSlotRepository.findAvailableByDateRangeAndSurgicalCenter(start, end, center));
            }
        } else {
            SurgicalCenter surgicalCenter = surgicalCenterRepository.findById(surgicalCenterId).orElse(null);
            if (surgicalCenter == null) {
                return Collections.emptyList();
            }
            if (surgicalCenter.getInstitution() == null
                    || !surgicalCenter.getInstitution().getId().equals(institutionId)) {
                throw new IllegalStateException("Surgical center does not belong to current institution");
            }
            timeSlots = timeSlotRepository.findAvailableTimeSlotsBySurgicalCenterAndInstitution(
                    surgicalCenter.getId(), institutionId, start, end);
        }
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

    /**
     * Liefert alle Zeitslots aller Behandlungsorte der aktuellen Institution in einem Zeitraum,
     * inklusive Patientenzahl pro Slot.
     */
    @Transactional(readOnly = true)
    public List<SurgicalCenterTimeSlot> getAllTimeSlotsForCurrentInstitutionWithTreatmentCount(LocalDate start,
            TimePeriod period) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot access time slots without institution context");
        }

        LocalDate effectiveStart = start != null && start.isAfter(LocalDate.now()) ? start : LocalDate.now();
        LocalDate end = period != null ? period.calculateEndDate(effectiveStart) : effectiveStart.plusMonths(6);

        List<SurgicalCenter> centers = surgicalCenterRepository.findByInstitutionId(institutionId);
        List<SurgicalCenterTimeSlot> result = new ArrayList<>();

        for (SurgicalCenter center : centers) {
            if (center == null || center.getId() == null) {
                continue;
            }
            List<SurgicalCenterTimeSlot> slotsForCenter =
                    timeSlotRepository.findBySurgicalCenterIdWithTreatmentCount(center.getId().intValue(),
                            institutionId);

            slotsForCenter.stream()
                    .filter(slot -> slot.getDate() != null
                            && !slot.getDate().isBefore(effectiveStart)
                            && !slot.getDate().isAfter(end)
                            && slot.isAvailable())
                    .forEach(result::add);
        }

        result.sort(
                java.util.Comparator
                        .comparing((SurgicalCenterTimeSlot slot) -> slot.getDate() != null ? slot.getDate()
                                : LocalDate.MAX)
                        .thenComparing(slot -> slot.getStartTime() != null ? slot.getStartTime() : java.time.LocalTime.MAX));

        return result;
    }

    /**
     * Deaktiviert einen Zeitslot (isAvailable = false), falls er zur aktuellen Institution gehört.
     */
    @Transactional
    public void deactivateTimeSlot(Long timeSlotId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot modify time slot without institution context");
        }

        SurgicalCenterTimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found: " + timeSlotId));

        SurgicalCenter center = slot.getSurgicalCenter();
        if (center == null || center.getInstitution() == null
                || !center.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Time slot does not belong to current institution");
        }

        slot.setAvailable(false);
        timeSlotRepository.save(slot);
    }

    /**
     * Löscht einen Zeitslot, falls er zur aktuellen Institution gehört.
     */
    @Transactional
    public void deleteTimeSlot(Long timeSlotId) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            throw new IllegalStateException("Cannot delete time slot without institution context");
        }

        SurgicalCenterTimeSlot slot = timeSlotRepository.findById(timeSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found: " + timeSlotId));

        SurgicalCenter center = slot.getSurgicalCenter();
        if (center == null || center.getInstitution() == null
                || !center.getInstitution().getId().equals(institutionId)) {
            throw new IllegalStateException("Time slot does not belong to current institution");
        }

        timeSlotRepository.delete(slot);
    }

    public List<SurgicalCenterTimeSlot> getNewTimeSlotsContainingNotApprovedTreatments(List<Long> timeSlotIds) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            // No institution context - return empty list to enforce data isolation
            return List.of();
        }
        
        // Nur vergangene Zeitslots (bis heute) mit ungeprüften Treatments
        LocalDate today = LocalDate.now();
        
        // Get all time slots with not approved treatments for this institution
        // The query already filters by institution and date <= today
        if (timeSlotIds == null || timeSlotIds.isEmpty()) {
            return timeSlotRepository.findAllContainingNotApprovedTreatments(institutionId, today);
        } else {
            return timeSlotRepository.findAllContainingNotApprovedTreatmentsAndNotInTimeSlotIdList(institutionId, today, timeSlotIds);
        }
    }

}
