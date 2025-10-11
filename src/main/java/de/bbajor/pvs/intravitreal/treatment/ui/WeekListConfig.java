package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.util.List;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class WeekListConfig {
    private List<Treatment> treatmentsOfWeek;
    private LocalDate startDateOfWeek;
    private LocalDate endDateOfWeek;
}
