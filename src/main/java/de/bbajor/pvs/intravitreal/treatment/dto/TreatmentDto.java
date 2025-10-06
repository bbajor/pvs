package de.bbajor.pvs.intravitreal.treatment.dto;

import java.time.LocalDate;

import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterAddressDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TreatmentDto {

    private Long id;
    private Long version;
    private String sideOfEye;
    private TreatmentPlanDto treatmentPlan;
    private SurgicalCenterTimeSlotDto surgicalCenterTimeSlot;
    private LocalDate approvalDate;
    private String remarks;

    public LocalDate getDate() {
        return surgicalCenterTimeSlot == null ? null : surgicalCenterTimeSlot.getDate();
    }

    public String getSurgicalCenterString() {
        if (surgicalCenterTimeSlot == null || surgicalCenterTimeSlot.getSurgicalCenter() == null) {
            return "-";
        }
        SurgicalCenterDto surgicalCenterDto = surgicalCenterTimeSlot.getSurgicalCenter();
        SurgicalCenterAddressDto address = surgicalCenterDto.getSurgicalCenterAddress();
        return new StringBuilder(surgicalCenterDto.getName()).append(" - ").append(address.getStreet())
                .append(" ")
                .append(address.getHouseNo()).append(", ")
                .append(address.getPostalCode()).append(" ").append(address.getCity())
                .toString();
    }

}
