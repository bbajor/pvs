package de.bbajor.pvs.ivomplan.dto;

import java.util.List;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SurgeryUnitDto {

    private Integer id;
    private String name;
    private String phone;
    private String email;
    private SurgeryUnitAddressDto surgeryUnitAddress;
    private String contact;
    private String phoneContact;
    private List<SurgeryUnitTimeSlotDto> availableTimeSlots;

    @Override
    public String toString() {
        return (name != null ? name.trim() : "Name n.b.") + " (Adresse: " +
                (surgeryUnitAddress != null ? surgeryUnitAddress.toString() : "-, ") + "Telefon: " +
                (phone != null ? phone.trim() : "-)");
    }
}
