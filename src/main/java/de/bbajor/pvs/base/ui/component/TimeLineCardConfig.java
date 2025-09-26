package de.bbajor.pvs.base.ui.component;

import java.time.LocalDate;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TimeLineCardConfig {

    private LocalDate treatmenDate;
    private boolean isSelected;
    private String description;

}
