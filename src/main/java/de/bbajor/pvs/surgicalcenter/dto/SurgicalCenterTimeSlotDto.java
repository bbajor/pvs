package de.bbajor.pvs.surgicalcenter.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SurgicalCenterTimeSlotDto {

    private Long id;
    private Long version;
    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private boolean isAvailable;
    private boolean isApproved;
    private SurgicalCenterDto surgicalCenter;
    private Long patientCount;

}
