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
    private SurgicalCenterTimeSlotRepository timeSlotRepository;
    @Autowired
    private SurgicalCenterRepository surgicalCenterRepository;
    @Autowired
    private SurgicalCenterMapper mapper;

    public List<SurgicalCenterDto> findAll() {
        return mapper.toSurgicalCenterDtoList(surgicalCenterRepository.findAll());
    }

    public SurgicalCenterDto toDto(SurgicalCenter surgeryUnit) {
        return mapper.toDto(surgeryUnit);
    }

    public Optional<SurgicalCenter> findByIdWithDetails(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return surgicalCenterRepository.findByIdWithDetails(id);
    }

    @Transactional
    public SurgicalCenter save(SurgicalCenterDto dto) {
        SurgicalCenter entityToSave = mapper.toEntity(dto);
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
    private void saveTimeSlots(List<SurgicalCenterTimeSlotDto> newTimeSlots, SurgicalCenter entityToSave) {
        if (newTimeSlots == null || newTimeSlots.isEmpty() || entityToSave == null) {
            return;
        }

        List<SurgicalCenterTimeSlot> newTimeSlotsToSave = newTimeSlots.stream()
                .map(e -> {
                    SurgicalCenterTimeSlot entity = mapper.toEntity(e);
                    entity.setSurgicalCenter(entityToSave);
                    return entity;
                })
                .toList();
        timeSlotRepository.saveAll(newTimeSlotsToSave);
    }

    public List<SurgicalCenterTimeSlot> findTimeSlotsBySurgicalCenterId(Integer id) {
        Optional<SurgicalCenter> surgicalCenter = surgicalCenterRepository.findById(id);
        if (!surgicalCenter.isPresent()) {
            return new ArrayList<>();
        }

        List<SurgicalCenterTimeSlot> timeSlots = timeSlotRepository
                .findBySurgicalCenterAndDateGreaterThanEqual(surgicalCenter.get(), LocalDate.now());
        return timeSlots;
    }

    @Transactional
    public void saveTimeSlotsAndSurgeryUnit(List<SurgicalCenterTimeSlotDto> newTimeSlots,
            SurgicalCenterDto surgicalCenterDto) {

        // TODO dtos aus der service-klasse auslagern
        if (surgicalCenterDto == null) {
            return;
        }

        SurgicalCenter entityToSave = mapper.toEntity(surgicalCenterDto);
        if (surgicalCenterDto.getSurgicalCenterAddress() != null) {
            entityToSave
                    .setSurgicalCenterAddress(mapper.toEntity(surgicalCenterDto.getSurgicalCenterAddress()));
        }

        SurgicalCenter savedEntity = surgicalCenterRepository.save(entityToSave);
        saveTimeSlots(newTimeSlots, savedEntity);
    }

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

}
