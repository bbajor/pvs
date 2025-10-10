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

    @Transactional(readOnly = true)
    public List<SurgicalCenter> findAll() {
        return surgicalCenterRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SurgicalCenter findByIdWithDetails(Integer id) {
        return surgicalCenterRepository.findByIdWithDetails(id).orElseThrow();
    }

    @Transactional
    public SurgicalCenter saveSurgicalCenter(SurgicalCenter entityToSave) {
        if (entityToSave.getId() != null && entityToSave.getId() == 0L) {
            entityToSave.setId(null);
        }
        if (entityToSave.getSurgicalCenterAddress() != null &&
                (entityToSave.getSurgicalCenterAddress().getId() == null
                        || entityToSave.getSurgicalCenterAddress().getId() == 0L)) {
            entityToSave.getSurgicalCenterAddress().setId(null);
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
        newTimeSlots.forEach(e -> e.setSurgicalCenter(savedSurgicalCenter));
        return timeSlotRepository.saveAll(newTimeSlots);
    }

    @Transactional
    protected List<SurgicalCenterTimeSlot> findTimeSlotsBySurgicalCenterId(Integer id) {
        Optional<SurgicalCenter> surgicalCenter = surgicalCenterRepository.findById(id);
        if (!surgicalCenter.isPresent()) {
            return new ArrayList<>();
        }

        List<SurgicalCenterTimeSlot> timeSlots = timeSlotRepository
                .findBySurgicalCenterAndDateGreaterThanEqual(surgicalCenter.get(), LocalDate.now());
        return timeSlots;
    }

    @Transactional(readOnly = true)
    public Collection<SurgicalCenterTimeSlot> findAvailableTimeSlotsFilteredBy(LocalDate periodStart,
            TimePeriod timePeriod,
            Integer surgicalCenterId) {

        if (periodStart == null || timePeriod == null) {
            return Collections.emptyList();
        }

        LocalDate start = periodStart.isAfter(LocalDate.now()) ? periodStart : LocalDate.now();
        LocalDate end = timePeriod.calculateEndDate(start);

        Sort sort = Sort.by("date").ascending().and(Sort.by("startTime").ascending());
        List<SurgicalCenterTimeSlot> timeSlots = new ArrayList<>();
        if (surgicalCenterId == null) {
            timeSlots = timeSlotRepository.findByDateBetween(start, end,
                    sort);
        } else {
            SurgicalCenter surgicalCenter = surgicalCenterRepository.getReferenceById(surgicalCenterId);
            if (surgicalCenter != null) {
                timeSlots = timeSlotRepository
                        .findByDateBetweenAndSurgicalCenter(start, end, surgicalCenter,
                                sort);
            }
        }
        return timeSlots;
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenter> getSurgicalCenters() {
        return surgicalCenterRepository.findAll();
    }

    @Transactional
    public SurgicalCenter saveTimeSlotsAndSurgicalCenter(List<SurgicalCenterTimeSlot> newTimeSlots,
            SurgicalCenter surgicalCenter) {
        Objects.requireNonNull(surgicalCenter);

        SurgicalCenter savedEntity = surgicalCenterRepository.save(surgicalCenter);
        List<SurgicalCenterTimeSlot> savedTimeSlots = saveTimeSlotsForExistingSurgicalCenter(newTimeSlots,
                savedEntity);
        return surgicalCenterRepository.getReferenceById(savedEntity.getId());
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenterTimeSlot> getTimeSlotsBySurgicalCenterIdWithTreatmentCount(Integer surgicalCenterId) {
        return timeSlotRepository.findBySurgicalCenterIdWithTreatmentCount(surgicalCenterId);
    }

}
