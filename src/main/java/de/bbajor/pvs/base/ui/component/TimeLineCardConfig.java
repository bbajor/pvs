package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;
import java.time.LocalTime;

import de.bbajor.pvs.base.util.SideOfEye;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class TimeLineCardConfig {

    private SideOfEye sideOfEye;
    private String additionalInfo;
    private LocalDate treatmentDate;
    private LocalTime startTime;
    private String locationInfo;
    private boolean isSelected;
    private boolean isFirst;

    public String getAdditionalInfo() {
        return additionalInfo == null ? "" : additionalInfo;
    }

    public TimeLineCardConfig(String additionalInfo, LocalDate emptyTimelineStartDate) {
        this.additionalInfo = additionalInfo;
        this.treatmentDate = emptyTimelineStartDate;
        this.isSelected = false;
        this.isFirst = true;
    }
}
