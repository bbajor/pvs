package de.bbajor.pvs.base.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PatientAddress extends AbstractEntity<Integer>{

    private int id;

    @Override
    public Integer getId() {
        return id;
    }

}
