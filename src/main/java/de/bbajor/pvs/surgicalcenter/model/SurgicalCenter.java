package de.bbajor.pvs.surgicalcenter.model;

import java.util.List;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
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
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private SurgicalCenterAddress surgicalCenterAddress;
    @OneToMany(mappedBy = "surgicalCenter", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<SurgicalCenterTimeSlot> availableTimeSlots;

}
