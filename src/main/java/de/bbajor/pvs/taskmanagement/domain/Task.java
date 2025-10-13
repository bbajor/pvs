package de.bbajor.pvs.taskmanagement.domain;

import java.time.Instant;
import java.time.LocalDate;

import org.jspecify.annotations.Nullable;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "task")
public class Task extends BasicEntity<Long> {

    public static final int DESCRIPTION_MAX_LENGTH = 255;

    @Size(max = DESCRIPTION_MAX_LENGTH)
    private String description;

    private Instant creationDate;

    @Nullable
    private LocalDate dueDate;

    @OneToOne(fetch = FetchType.EAGER)
    private SurgicalCenterTimeSlot timeSlot;
}
