package de.bbajor.pvs.surgicalcenter.model;

import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@NoArgsConstructor
public class SurgicalCenterTimeSlot extends BasicEntity<Long> {

    private String description;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    @ManyToOne
    private SurgicalCenter surgicalCenter;
    private boolean isAvailable;
    private boolean isApproved;

    @Transient
    private int patientCount;
    
    public SurgicalCenterTimeSlot(
            Long id, 
            Long version, 
            String description,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            SurgicalCenter surgicalCenter,
            boolean isAvailable,
            boolean isApproved,
            long patientCount) {
        super();
        this.setId(id);
        this.setVersion(version);
        this.description = description;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.surgicalCenter = surgicalCenter;
        this.isAvailable = isAvailable;
        this.isApproved = isApproved;
        this.patientCount = (int) patientCount;
    }

}
