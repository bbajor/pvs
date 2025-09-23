package de.bbajor.pvs.ivomplan.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IvomDiagnosisDto {

    private Long id;
    private Long version;
    private String name;
    private String icdCode;
    private String description;
}
