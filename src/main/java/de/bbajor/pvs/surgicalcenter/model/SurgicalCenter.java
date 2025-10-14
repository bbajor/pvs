package de.bbajor.pvs.surgicalcenter.model;

import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.patient.model.Address;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class SurgicalCenter extends BasicEntity<Integer> {

    private String name;
    private String description;
    private String phone;
    private String email;
    private String contact;
    private String phoneContact;
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "surgical_center_street")),
            @AttributeOverride(name = "houseNo", column = @Column(name = "surgical_center_house_no")),
            @AttributeOverride(name = "postlCode", column = @Column(name = "surgical_center_postal_code")),
            @AttributeOverride(name = "city", column = @Column(name = "surgical_center_city")),
            @AttributeOverride(name = "country", column = @Column(name = "surgical_center_country"))
    })
    private Address address;
    @OneToMany(mappedBy = "surgicalCenter", fetch = FetchType.EAGER)
    private List<SurgicalCenterTimeSlot> availableTimeSlots;

    @Override
    public String toString() {
        return name + (description != null && !description.isBlank() ? " (" + description + ")" : "") + ", "
                + (address != null ? address.toString() : "");
    }
}
