package de.bbajor.pvs.ivomplan.model;

import java.util.List;

import de.bbajor.pvs.base.domain.Address;
import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class SurgeryUnit extends BasicEntity<Integer> {

    private String name;
    private String phone;
    private String email;
    private String contact;
    private String phoneContact;
    @ManyToOne
    private Address address;
    @OneToMany(mappedBy = "surgeryUnit")
    private List<TimeSlot> visitingHours;

}
