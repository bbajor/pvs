package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TimeLineCardConfig {

    private Treatment treatment;
    private LocalDate firstDate;
    private boolean first; // Lombok generiert setFirst() und isFirst()
    private Integer treatmentCount;
    private Integer mostCommonInterval; // in Wochen

    public String getAdditionalInfo() {
        return treatment != null && treatment.getAdditionalInfo() != null ? treatment.getAdditionalInfo() : "";
    }

    public boolean isApproved() {
        return treatment != null && treatment.getApprovalDate() != null;
    }

    public LocalDate getTreatmentDate() {
        return treatment != null && treatment.getSurgicalCenterTimeSlot() != null
                ? treatment.getSurgicalCenterTimeSlot().getDate()
                : firstDate;
    }
}
