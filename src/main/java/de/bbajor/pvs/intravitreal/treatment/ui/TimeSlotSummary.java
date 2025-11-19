package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;
import java.util.List;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class TimeSlotSummary {
    private final SurgicalCenterTimeSlot timeSlot;
    private final List<Treatment> treatments;
    
    @Setter
    private int number;
    
    public int getNumber() {
        return number;
    }
    
    public LocalDate getDate() {
        return timeSlot != null ? timeSlot.getDate() : null;
    }
    
    public String getCenterShort() {
        if (timeSlot != null && timeSlot.getSurgicalCenter() != null) {
            String name = timeSlot.getSurgicalCenter().getName();
            // Kürze den Namen auf max. 30 Zeichen
            if (name != null && name.length() > 30) {
                return name.substring(0, 27) + "...";
            }
            return name != null ? name : "-";
        }
        return "-";
    }
    
    public int getPatientCount() {
        return treatments != null ? treatments.size() : 0;
    }
    
    public String getTimeRange() {
        if (timeSlot != null && timeSlot.getStartTime() != null && timeSlot.getEndTime() != null) {
            LocalTime start = timeSlot.getStartTime();
            LocalTime end = timeSlot.getEndTime();
            Duration duration = Duration.between(start, end);
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            
            return String.format("%02d:%02d - %02d:%02d (%d:%02d h)", 
                start.getHour(), start.getMinute(),
                end.getHour(), end.getMinute(),
                hours, minutes);
        }
        return "-";
    }
}

