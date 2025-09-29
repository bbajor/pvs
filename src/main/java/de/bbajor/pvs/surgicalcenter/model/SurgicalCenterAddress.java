package de.bbajor.pvs.surgicalcenter.model;

import java.util.Locale;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Accessors(chain = true)
public class SurgicalCenterAddress extends BasicEntity<Long> {

    private String street;
    private String houseNo;
    private Double postalCode;
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
}
