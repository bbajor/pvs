package de.bbajor.pvs.ivomplan.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.ivomplan.dto.SurgeryUnitDto;
import de.bbajor.pvs.ivomplan.dto.SurgeryUnitTimeSlotDto;
import de.bbajor.pvs.ivomplan.service.SurgeryUnitService;

@Component
public class SurgeryUnitListPresenter {

    private final SurgeryUnitService surgeryUnitService;

    public SurgeryUnitListPresenter(SurgeryUnitService surgeryUnitService) {
        this.surgeryUnitService = surgeryUnitService;
    }

    public List<SurgeryUnitDto> getAll() {
        return surgeryUnitService.findAll();
    }

    public SurgeryUnitDto getById(Integer id) {
        return surgeryUnitService.findByIdWithDetails(id);
    }

    public void save(SurgeryUnitDto surgeryUnitDto, List<TimeSlotConfig> timeSlotsToCreate) {
        List<SurgeryUnitTimeSlotDto> newTimeSlots = new ArrayList<>();
        for (TimeSlotConfig config : timeSlotsToCreate) {
            newTimeSlots.addAll(TimeSlotCreator.createTimeSlots(config));
        }
        if (surgeryUnitDto.getAvailableTimeSlots() != null) {
            newTimeSlots.removeAll(TimeSlotCreator.getNewInvalidTimeSlots(surgeryUnitDto.getAvailableTimeSlots(), newTimeSlots));
        }

        surgeryUnitService.saveTimeSlots(newTimeSlots, surgeryUnitDto);
        surgeryUnitService.save(surgeryUnitDto);
    }

}
