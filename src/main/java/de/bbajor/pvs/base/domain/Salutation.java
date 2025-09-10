package de.bbajor.pvs.base.domain;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
public class Salutation extends BasicEntity<Integer> {
    
    private String salutation;

    
}