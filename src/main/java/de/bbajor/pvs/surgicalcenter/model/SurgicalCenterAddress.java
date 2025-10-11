package de.bbajor.pvs.surgicalcenter.model;

import java.util.Locale;

import de.bbajor.pvs.patient.model.Address;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
public class SurgicalCenterAddress extends Address {

    private String street;
    private String houseNo;
    private Integer postalCode;
    private String city;

    private String country;

    @Transient
    private Locale locale;

    @PostLoad
    void loadLocale() {
        if (country != null) {
            this.locale = Locale.of("", country);
        }
    }

    @PrePersist
    @PreUpdate
    void saveLocale() {
        if (locale != null) {
            this.country = locale.getCountry();
        }
    }

    @Override
    public String toString() {
        return street + " " + houseNo + ", " + postalCode + " " + city
                + (locale != null ? " (" + locale.getDisplayCountry() + ")" : "");
    }
}
