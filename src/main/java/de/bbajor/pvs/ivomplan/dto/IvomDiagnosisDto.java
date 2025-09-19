package de.bbajor.pvs.ivomplan.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IvomDiagnosisDto {

    private long id;
    private String name;
    private String icdCode;
    private String description;
}
