package de.bbajor.pvs.surgicalcenter.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
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
    public List<SurgicalCenterDto> findAll() {
        return mapper.toSurgicalCenterDtoList(surgicalCenterRepository.findAll());
    }

    @Transactional(readOnly = true)
    public SurgicalCenterDto findByIdWithDetails(Integer id) {
        SurgicalCenter surgicalCenter = surgicalCenterRepository.findByIdWithDetails(id).orElseThrow();
        SurgicalCenterDto surgicalCenterDto = mapper.toDto(surgicalCenter);
        List<SurgicalCenterTimeSlotDto> availableTimeSlotDtos = new ArrayList<>();
        for (SurgicalCenterTimeSlot timeSlot : surgicalCenter.getAvailableTimeSlots()) {
            SurgicalCenterTimeSlotDto timeSlotDto = mapper.toDto(timeSlot);
            availableTimeSlotDtos.add(timeSlotDto);
        }
        surgicalCenterDto.setAvailableTimeSlots(availableTimeSlotDtos);

        return surgicalCenterDto;
    }

    @Transactional
    public SurgicalCenter saveSurgicalCenter(SurgicalCenterDto dto) {
        SurgicalCenter entityToSave = mapper.toEntity(dto);
        return saveSurgicalCenter(entityToSave);
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
    private List<SurgicalCenterTimeSlotDto> saveTimeSlotsForExistingSurgicalCenter(
            List<SurgicalCenterTimeSlotDto> newTimeSlots,
            SurgicalCenter existingEntity) {
        if (newTimeSlots == null || newTimeSlots.isEmpty() || existingEntity == null) {
            return Collections.emptyList();
        }

        List<SurgicalCenterTimeSlot> newTimeSlotsToSave = newTimeSlots.stream()
                .map(e -> {
                    SurgicalCenterTimeSlot entity = mapper.toEntity(e);
                    entity.setSurgicalCenter(existingEntity);
                    return entity;
                })
                .toList();
        List<SurgicalCenterTimeSlot> savedTimeSlots = timeSlotRepository.saveAll(newTimeSlotsToSave);
        return mapper.toTimeSlotDtoList(savedTimeSlots);
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
    public Collection<SurgicalCenterTimeSlotDto> findAvailableTimeSlotsFilteredBy(LocalDate periodStart,
            TimePeriod timePeriod,
            Integer surgicalCenterId) {

        if (periodStart == null || timePeriod == null) {
            return Collections.emptyList();
        }

        LocalDate start = periodStart.isAfter(LocalDate.now()) ? periodStart : LocalDate.now();
        LocalDate end = timePeriod.calculateEndDate(start);

        List<SurgicalCenterTimeSlotDto> availableTimeSlots = new ArrayList<>();
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
        availableTimeSlots.addAll(mapper.toTimeSlotDtoList(timeSlots));
        return availableTimeSlots;
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenterDto> getSurgicalCenters() {
        List<SurgicalCenterDto> surgicalCenterDtos = new ArrayList<>();
        List<SurgicalCenter> surgicalCenters = surgicalCenterRepository.findAll();
        for (SurgicalCenter surgicalCenter : surgicalCenters) {
            SurgicalCenterDto surgicalCenterDto = mapper.toDto(surgicalCenter);
            surgicalCenterDto.setAvailableTimeSlots(mapper.toTimeSlotDtoList(timeSlotRepository
                    .findBySurgicalCenter(surgicalCenter)));
            surgicalCenterDtos.add(surgicalCenterDto);
        }
        return surgicalCenterDtos;
    }

    @Transactional
    public void saveTimeSlotsAndSurgicalCenter(List<SurgicalCenterTimeSlot> availableTimeSlots,
            SurgicalCenter surgicalCenter) {
        SurgicalCenter savedEntity = surgicalCenterRepository.save(surgicalCenter);
        SurgicalCenter saved = surgicalCenterRepository.getReferenceById(savedEntity.getId());
        for (SurgicalCenterTimeSlot surgicalCenterTimeSlot : availableTimeSlots) {
            surgicalCenterTimeSlot.setSurgicalCenter(saved);
        }
        timeSlotRepository.saveAll(availableTimeSlots);
    }

    @Transactional
    public SurgicalCenterDto saveTimeSlotsAndSurgicalCenter(List<SurgicalCenterTimeSlotDto> newTimeSlots,
            SurgicalCenterDto surgicalCenterDto) {

        if (surgicalCenterDto == null) {
            return null;
        }

        SurgicalCenter entityToSave = mapper.toEntity(surgicalCenterDto);
        if (surgicalCenterDto.getSurgicalCenterAddress() != null) {
            entityToSave
                    .setSurgicalCenterAddress(mapper.toEntity(surgicalCenterDto.getSurgicalCenterAddress()));
        }

        SurgicalCenter savedEntity = surgicalCenterRepository.save(entityToSave);
        List<SurgicalCenterTimeSlotDto> savedTimeSlots = saveTimeSlotsForExistingSurgicalCenter(newTimeSlots,
                savedEntity);
        SurgicalCenterDto savedSurgicalCenterDto = mapper.toDto(savedEntity);
        savedSurgicalCenterDto.setAvailableTimeSlots(savedTimeSlots);
        return savedSurgicalCenterDto;
    }

    @Transactional(readOnly = true)
    public List<SurgicalCenterTimeSlotDto> getTimeSlotsBySurgicalCenterIdWithTreatmentCount(Integer surgicalCenterId) {
        return timeSlotRepository.findBySurgicalCenterIdWithTreatmentCount(surgicalCenterId);
    }

}
