package de.bbajor.pvs.ivom.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiseaseDto {

    private long id;
    private String disease;
}
