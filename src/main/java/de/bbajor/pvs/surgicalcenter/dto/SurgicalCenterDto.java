package de.bbajor.pvs.surgicalcenter.dto;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class SurgicalCenterDto {

    
    private Integer id;
    private Long version;
    private String name;
    private String phone;
    private String email;
    private SurgicalCenterAddressDto surgicalCenterAddress;
    private String contact;
    private String phoneContact;
    private List<SurgicalCenterTimeSlotDto> availableTimeSlots;

    @Override
    public String toString() {
        return (name != null ? name.trim() : "Name n.b.") + " (Adresse: " +
                (surgicalCenterAddress != null ? surgicalCenterAddress.toString() : "-, ") + "Telefon: " +
                (phone != null ? phone.trim() : "-)");
    }
}
