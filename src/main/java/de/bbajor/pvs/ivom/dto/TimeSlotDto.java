package de.bbajor.pvs.ivom.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimeSlotDto {

    private Long id;
    private String timeSlot;
    private LocalDate date;
    private CooperatingAmbulanceDto cooperatingAmbulance;
    private boolean isAvailable;
    private List<IvomPlanDto> ivom;

    String getCooperatingAmbulance() {
        return cooperatingAmbulance != null ? cooperatingAmbulance.toString() : "Standort n.b.";
    }
}
