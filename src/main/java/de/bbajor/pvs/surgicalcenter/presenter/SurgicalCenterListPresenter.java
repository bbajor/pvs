package de.bbajor.pvs.surgicalcenter.presenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterMapper;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class SurgicalCenterListPresenter {

    private final SurgicalCenterMapper modelToDtoMapper;
    private final SurgicalCenterService surgeryUnitService;

    public SurgicalCenterListPresenter(SurgicalCenterService surgicalCenterService,
            SurgicalCenterMapper modelToDtoMapper) {
        this.surgeryUnitService = surgicalCenterService;
        this.modelToDtoMapper = modelToDtoMapper;
    }

    public List<SurgicalCenterDto> getAll() {
        return surgeryUnitService.findAll();
    }

    public SurgicalCenterDto getById(Integer id) {

        Optional<SurgicalCenter> surgicalCenter = surgeryUnitService.findByIdWithDetails(id);

        if (surgicalCenter.isPresent()) {

            SurgicalCenterDto surgeryUnitDto = modelToDtoMapper.toDto(surgicalCenter.get());
            List<SurgicalCenterTimeSlotDto> availableTimeSlotDtos = new ArrayList<>();
            for (SurgicalCenterTimeSlot timeSlot : surgicalCenter.get().getAvailableTimeSlots()) {
                SurgicalCenterTimeSlotDto timeSlotDto = modelToDtoMapper.toDto(timeSlot);
                availableTimeSlotDtos.add(timeSlotDto);
            }
            surgeryUnitDto.setAvailableTimeSlots(availableTimeSlotDtos);

            return surgeryUnitDto;

        }
        return null;
    }

    public void save(SurgicalCenterDto surgeryUnitDto, List<TimeSlotConfig> timeSlotsToCreate) {

        List<SurgicalCenterTimeSlotDto> newTimeSlots = new ArrayList<>();
        for (TimeSlotConfig config : timeSlotsToCreate) {
            newTimeSlots.addAll(TimeSlotCreator.createTimeSlots(config));
        }

        if (surgeryUnitDto.getAvailableTimeSlots() != null) {
            newTimeSlots.removeAll(
                    TimeSlotCreator.getNewInvalidTimeSlots(surgeryUnitDto.getAvailableTimeSlots(), newTimeSlots));
        }

        surgeryUnitService.saveTimeSlotsAndSurgeryUnit(newTimeSlots, surgeryUnitDto);
    }

}
