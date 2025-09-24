package de.bbajor.pvs.ivomplan.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.base.misc.ModelToDtoMapper;
import de.bbajor.pvs.ivomplan.controller.TimeSlotConfig;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.model.SurgeryUnit;
import de.bbajor.pvs.ivomplan.model.SurgeryUnitTimeSlot;
import de.bbajor.pvs.ivomplan.repository.SurgeryUnitRepository;
import de.bbajor.pvs.ivomplan.repository.SurgeryUnitTimeSlotRepository;

@Service
public class SurgeryUnitService {

    private final SurgeryUnitTimeSlotRepository surgeryUnitTimeSlotRepository;
    private final SurgeryUnitRepository surgeryUnitRepository;
    private final ModelToDtoMapper modelToDtoMapper;

    public SurgeryUnitService(SurgeryUnitRepository surgeryUnitRepository, ModelToDtoMapper modelToDtoMapper,
            SurgeryUnitTimeSlotRepository surgeryUnitTimeSlotRepository) {
        this.surgeryUnitRepository = surgeryUnitRepository;
        this.modelToDtoMapper = modelToDtoMapper;
        this.surgeryUnitTimeSlotRepository = surgeryUnitTimeSlotRepository;
    }

    public List<SurgeryUnitDto> findAll() {
        List<SurgeryUnit> surgeryUnits = surgeryUnitRepository.findAll();
        return surgeryUnits.stream()
                .map(this::toDto)
                .toList();
    }

    public SurgeryUnitDto toDto(SurgeryUnit surgeryUnit) {
        return modelToDtoMapper.toDto(surgeryUnit);
    }

    public SurgeryUnitDto findByIdWithDetails(Integer id) {
        if (id == null || id <= 0) {
            return null;
        }
        Optional<SurgeryUnit> surgeryUnit = surgeryUnitRepository.findByIdWithDetails(id);
        if (surgeryUnit.isPresent()) {
            SurgeryUnitDto surgeryUnitDto = toDto(surgeryUnit.get());
            List<SurgeryUnitTimeSlotDto> availableTimeSlotDtos = new ArrayList<>();
            for (SurgeryUnitTimeSlot timeSlot : surgeryUnit.get().getAvailableTimeSlots()) {
                SurgeryUnitTimeSlotDto timeSlotDto = toDto(timeSlot);
                availableTimeSlotDtos.add(timeSlotDto);
            }
            surgeryUnitDto.setAvailableTimeSlots(availableTimeSlotDtos);
            return surgeryUnitDto;
        } else {
            return null;
        }
    }

    private SurgeryUnitTimeSlotDto toDto(SurgeryUnitTimeSlot timeSlot) {
        return modelToDtoMapper.toDto(timeSlot);
    }

    @Transactional
    public SurgeryUnit save(SurgeryUnitDto surgeryUnitDto) {
        SurgeryUnit entityToSave = modelToDtoMapper.toEntity(surgeryUnitDto);
        if (entityToSave.getId() != null && entityToSave.getId() == 0L) {
            entityToSave.setId(null);
        }
        if (entityToSave.getSurgeryUnitAddress() != null &&
                (entityToSave.getSurgeryUnitAddress().getId() == null
                        || entityToSave.getSurgeryUnitAddress().getId() == 0L)) {
            entityToSave.getSurgeryUnitAddress().setId(null);
        }
        return surgeryUnitRepository.save(entityToSave);
    }

    @Transactional
    private void saveTimeSlots(List<SurgeryUnitTimeSlotDto> newTimeSlots, SurgeryUnit surgeryUnit) {
        if (newTimeSlots == null || newTimeSlots.isEmpty() || surgeryUnit == null) {
            return;
        }

        List<SurgeryUnitTimeSlot> newTimeSlotsToSave = newTimeSlots.stream()
                .map(e -> {
                    SurgeryUnitTimeSlot entity = toEntity(e);
                    entity.setSurgeryUnit(surgeryUnit);
                    return entity;
                })
                .toList();
        surgeryUnitTimeSlotRepository.saveAll(newTimeSlotsToSave);
    }

    private SurgeryUnitTimeSlot toEntity(SurgeryUnitTimeSlotDto dto) {
        return modelToDtoMapper.toEntity(dto);
    }

    public List<SurgeryUnitTimeSlot> findSurgeryUnitTimeSlots(Integer id) {
        // TODO schöner per Query
        Optional<SurgeryUnit> surgeryUnit = surgeryUnitRepository.findById(id);
        List<SurgeryUnitTimeSlot> timeSlots = surgeryUnitTimeSlotRepository.findBySurgeryUnit(surgeryUnit.get());
        return timeSlots.stream().filter(
                element -> element.getDate().isAfter(LocalDate.now()) || element.getDate().isEqual(LocalDate.now()))
                .toList();
    }

    @Transactional
    public void saveTimeSlotsAndSurgeryUnit(List<SurgeryUnitTimeSlotDto> newTimeSlots, SurgeryUnitDto surgeryUnitDto) {
        if (surgeryUnitDto == null || surgeryUnitDto.getId() == null) {
            SurgeryUnit savedEntity = surgeryUnitRepository.save(modelToDtoMapper.toEntity(surgeryUnitDto));
            saveTimeSlots(newTimeSlots, savedEntity);
        }
    }

    public Collection<SurgeryUnitTimeSlotDto> findTimeSlotsFilteredBy(TimeSlotConfig currentConfig,
            SurgeryUnitDto surgeryUnitDto) {
        // TODO hier muss eine Filterung nur nach dem Startdatum und dem Zeitraum
        // stattfinden. Die TimeSlotConfig hat hier nichts zu suchen!!!
        if (currentConfig == null || currentConfig.getPeriodStart() == null || currentConfig.getTimePeriod() == null) {
            return Collections.emptyList();
        }
        LocalDate start = currentConfig.getPeriodStart().isAfter(LocalDate.now()) ? currentConfig.getPeriodStart()
                : LocalDate.now();
        LocalDate end = currentConfig.getTimePeriod().calculateEndDate(start);
        List<SurgeryUnitTimeSlot> availableTimeSlots = new ArrayList<>();
        Sort sort = Sort.by("date").ascending().and(Sort.by("startTime").ascending());
        if (surgeryUnitDto == null) {
            availableTimeSlots.addAll(surgeryUnitTimeSlotRepository.findByDateBetween(start, end, sort));
        } else {
            SurgeryUnit surgeryUnit = surgeryUnitRepository.getReferenceById(surgeryUnitDto.getId());
            if (surgeryUnit != null) {
                availableTimeSlots.addAll(
                        surgeryUnitTimeSlotRepository.findByDateBetweenAndSurgeryUnit(start, end, surgeryUnit, sort));
            }
        }

        List<SurgeryUnitTimeSlot> fullyFiltered = new ArrayList<>();
        LocalDate startDate = currentConfig.getPeriodStart();
        LocalDate endDate = currentConfig.getTimePeriod().calculateEndDate(startDate);

        int repeatEveryWeeks = currentConfig.getTimeSlotRepetition().getRepeatEveryWeeks();

        for (SurgeryUnitTimeSlot slot : availableTimeSlots) {
            LocalDate slotDate = slot.getDate();

            // nur Slots innerhalb des Zeitraums beachten
            if (!slotDate.isBefore(startDate) && !slotDate.isAfter(endDate)) {

                // Abstands-Berechnung in Wochen (inkl. Jahrwechsel)
                long weeksBetween = ChronoUnit.WEEKS.between(startDate, slotDate);

                // nur Slots im Wiederholungsrhythmus aufnehmen
                if (weeksBetween % repeatEveryWeeks == 0) {
                    fullyFiltered.add(slot);
                }
            }
        }

        List<SurgeryUnitTimeSlotDto> filteredDtos = new ArrayList<>();
        for (SurgeryUnitTimeSlot surgeryUnitTimeSlot : fullyFiltered) {
            SurgeryUnitTimeSlotDto dto = toDto(surgeryUnitTimeSlot);
            filteredDtos.add(dto);
        }

        return filteredDtos;
    }

}
