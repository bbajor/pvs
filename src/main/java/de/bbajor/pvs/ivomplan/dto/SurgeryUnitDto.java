package de.bbajor.pvs.ivomplan.dto;

import java.util.List;

import de.bbajor.pvs.base.dto.AddressDto;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SurgeryUnitDto {

    private int id;
    private String name;
    private String phone;
    private String email;
    private AddressDto address;
    private String contact;
    private String phoneContact;
    private List<TimeSlotDto> bookedTimeSlots;

    @Override
    public String toString() {
        return (name != null ? name.trim() : "Name n.b.") + " (Adresse: " +
                (address != null ? address.toString() : "-, ") + "Telefon: " +
                (phone != null ? phone.trim() : "-)");
    }
}
