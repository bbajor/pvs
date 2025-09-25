package de.bbajor.pvs.surgicalcenter.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.dto.TimePeriod;
import de.bbajor.pvs.base.util.ModelToDtoMapper;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterTimeSlotRepository;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.repository.SurgicalCenterRepository;

@Service
public class SurgicalCenterService {

    private final SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository;
    private final SurgicalCenterRepository surgicalCenterRepository;
    private final ModelToDtoMapper modelToDtoMapper;

    public SurgicalCenterService(SurgicalCenterRepository surgicalCenterRepository, ModelToDtoMapper modelToDtoMapper,
            SurgicalCenterTimeSlotRepository surgicalCenterTimeSlotRepository) {
        this.surgicalCenterRepository = surgicalCenterRepository;
        this.modelToDtoMapper = modelToDtoMapper;
        this.surgicalCenterTimeSlotRepository = surgicalCenterTimeSlotRepository;
    }

    public List<SurgicalCenter> findAll() {
        return surgicalCenterRepository.findAll();
    }

    public SurgicalCenterDto toDto(SurgicalCenter surgeryUnit) {
        return modelToDtoMapper.toDto(surgeryUnit);
    }

    public Optional<SurgicalCenter> findByIdWithDetails(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        return surgicalCenterRepository.findByIdWithDetails(id);
    }

    @Transactional
    public SurgicalCenter save(SurgicalCenterDto surgeryUnitDto) {
        SurgicalCenter entityToSave = modelToDtoMapper.toEntity(surgeryUnitDto);
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
    private void saveTimeSlots(List<SurgicalCenterTimeSlotDto> newTimeSlots, SurgicalCenter surgeryUnit) {
        if (newTimeSlots == null || newTimeSlots.isEmpty() || surgeryUnit == null) {
            return;
        }

        List<SurgicalCenterTimeSlot> newTimeSlotsToSave = newTimeSlots.stream()
                .map(e -> {
                    SurgicalCenterTimeSlot entity = toEntity(e);
                    entity.setSurgicalCenter(surgeryUnit);
                    return entity;
                })
                .toList();
        surgicalCenterTimeSlotRepository.saveAll(newTimeSlotsToSave);
    }

    private SurgicalCenterTimeSlot toEntity(SurgicalCenterTimeSlotDto dto) {
        return modelToDtoMapper.toEntity(dto);
    }

    public List<SurgicalCenterTimeSlot> findSurgeryUnitTimeSlots(Integer id) {
        // TODO schöner per Query
        Optional<SurgicalCenter> surgeryUnit = surgicalCenterRepository.findById(id);
        List<SurgicalCenterTimeSlot> timeSlots = surgicalCenterTimeSlotRepository
                .findBySurgicalCenter(surgeryUnit.get());
        return timeSlots.stream().filter(
                element -> element.getDate().isAfter(LocalDate.now()) || element.getDate().isEqual(LocalDate.now()))
                .toList();
    }

    @Transactional
    public void saveTimeSlotsAndSurgeryUnit(List<SurgicalCenterTimeSlotDto> newTimeSlots,
            SurgicalCenterDto surgicalCenterDto) {

        // TODO dtos aus der service-klasse auslagern
        if (surgicalCenterDto == null) {
            return;
        }

        SurgicalCenter entityToSave = modelToDtoMapper.toEntity(surgicalCenterDto);
        if (surgicalCenterDto.getSurgicalCenterAddress() != null) {
            entityToSave
                    .setSurgicalCenterAddress(modelToDtoMapper.toEntity(surgicalCenterDto.getSurgicalCenterAddress()));
        }

        SurgicalCenter savedEntity = surgicalCenterRepository.save(entityToSave);
        saveTimeSlots(newTimeSlots, savedEntity);
    }

    public Collection<SurgicalCenterTimeSlot> findAvailableTimeSlotsFilteredBy(LocalDate periodStart,
            TimePeriod timePeriod,
            Integer surgicalCenterId) {

        if (periodStart == null || timePeriod == null) {
            return Collections.emptyList();
        }

        LocalDate start = periodStart.isAfter(LocalDate.now()) ? periodStart : LocalDate.now();
        LocalDate end = timePeriod.calculateEndDate(start);

        List<SurgicalCenterTimeSlot> availableTimeSlots = new ArrayList<>();
        Sort sort = Sort.by("date").ascending().and(Sort.by("startTime").ascending());
        if (surgicalCenterId == null) {
            availableTimeSlots.addAll(surgicalCenterTimeSlotRepository.findByDateBetween(start, end, sort));
        } else {
            SurgicalCenter surgicalCenter = surgicalCenterRepository.getReferenceById(surgicalCenterId);
            if (surgicalCenter != null) {
                availableTimeSlots.addAll(
                        surgicalCenterTimeSlotRepository.findByDateBetweenAndSurgicalCenter(start, end, surgicalCenter,
                                sort));
            }
        }

        return availableTimeSlots;
    }

    public Optional<SurgicalCenterTimeSlot> findSurgicalCenterTimeSlotById(Long id) {
        return surgicalCenterTimeSlotRepository.findById(id);
    }

}
