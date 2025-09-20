package de.bbajor.pvs.ivomplan.model;

import java.time.LocalDate;
import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.ivomdrug.model.IvomDrug;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class Treatment extends BasicEntity<Long> {

    private String name;
    private LocalDate creationDate;
    private String description;
    private String dosage;
    private String frequency;
    private String sideOfEye;
    @ManyToOne
    private IvomDrug drug;
    private String additionalInformation;
    @OneToMany
    private List<TimeSlot> timeSlots;


}
