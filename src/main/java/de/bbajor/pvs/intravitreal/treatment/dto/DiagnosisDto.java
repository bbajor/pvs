package de.bbajor.pvs.intravitreal.treatment.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class DiagnosisDto {

    private Long id;
    private Long version;
    private String name;
    private String icdCode;
    private String description;

    @Override
    public String toString() {
        return (name != null ? name.trim() : "Name n.b.") + " (ICD: " + (icdCode != null ? icdCode.trim() : "-") + ")";
    }
}
