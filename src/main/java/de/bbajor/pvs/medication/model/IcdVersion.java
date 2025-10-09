package de.bbajor.pvs.medication.model;

import java.sql.Date;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class IcdVersion extends BasicEntity<Integer> {

    private String description;
    private Date validFrom;
    private Date validTo;
}
