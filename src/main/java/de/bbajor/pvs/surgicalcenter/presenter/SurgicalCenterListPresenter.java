package de.bbajor.pvs.surgicalcenter.presenter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import de.bbajor.pvs.surgicalcenter.service.SurgicalCenterService;

@Component
public class SurgicalCenterListPresenter {

    private static final Logger LOG = LogManager.getLogger();

    @Autowired
    private SurgicalCenterService surgicalCenterService;

    public List<SurgicalCenterDto> getAll() {
        return surgicalCenterService.findAll();
    }

    public SurgicalCenterDto getById(Integer id) {
        // TODO only use one query here
        SurgicalCenterDto dto = surgicalCenterService.findByIdWithDetails(id);
        List<SurgicalCenterTimeSlotDto> timeSlotDtos = surgicalCenterService
                .getTimeSlotsBySurgicalCenterIdWithTreatmentCount(id);
        dto.setAvailableTimeSlots(timeSlotDtos);
        return dto;
    }

    public void save(SurgicalCenterDto surgicalCenterDto, List<TimeSlotConfig> timeSlotConfigList) {
        LOG.debug("Entering save-method for SurgicalCenter....");
        List<SurgicalCenterTimeSlotDto> newTimeSlots = new ArrayList<>();
        for (TimeSlotConfig config : timeSlotConfigList) {
            List<SurgicalCenterTimeSlotDto> timeSlotDtos = TimeSlotCreator.createTimeSlots(config);
            newTimeSlots.addAll(timeSlotDtos);
        }
        LOG.debug("Found " + newTimeSlots.size() + " new TimeSlots for SurgicalCenter....");

        if (surgicalCenterDto.getAvailableTimeSlots() != null) {
            Collection<SurgicalCenterTimeSlotDto> invalidSlots = TimeSlotCreator
                    .getNewInvalidTimeSlots(surgicalCenterDto.getAvailableTimeSlots(), newTimeSlots);
            newTimeSlots.removeAll(invalidSlots);
            LOG.debug("Found " + invalidSlots.size() + " invalid TimeSlots, that had to be removed before saving...");
        }

        surgicalCenterService.saveTimeSlotsAndSurgicalCenter(newTimeSlots, surgicalCenterDto);
    }

}
