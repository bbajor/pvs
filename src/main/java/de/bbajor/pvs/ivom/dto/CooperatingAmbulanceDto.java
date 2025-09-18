package de.bbajor.pvs.ivom.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CooperatingAmbulanceDto {

    private int id;
    private String name;
    private String phone;
    private String email;
    private String address;

    @Override
    public String toString() {
        return (name != null ? name.trim() : "Name n.b.") + " (Adresse: " +
                (address != null ? address.trim() : "-, ") + "Telefon: " +
                (phone != null ? phone.trim() : "-)");
    }
}
