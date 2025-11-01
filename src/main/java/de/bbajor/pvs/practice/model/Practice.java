package de.bbajor.pvs.practice.model;

import de.bbajor.pvs.tenant.model.Tenant;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import de.bbajor.pvs.base.domain.BasicEntity;

/**
 * Entity representing the practice data.
 * In multi-tenant mode, each tenant has its own practice data.
 */
@Getter
@Setter
@Entity
@Table(name = "practice")
public class Practice extends BasicEntity<Long> {

    private String practiceName;
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private String country;
    
    // Practice owner information
    private String ownerName;
    private String ownerTitle; // e.g., "Dr. med."
    
    // German healthcare system identifiers
    private String lanr; // Leistungserbringer-Abrechnungsnummer (Doctor ID)
    private String bsnr; // Betriebsstättennummer (Practice ID)
    
    // Contact information
    private String phone;
    private String fax;
    private String email;
    
    // Additional information
    private String additionalInfo;
    
    /**
     * The tenant this practice belongs to.
     * In multi-tenant mode, each tenant has its own practice data.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
    
    /**
     * Returns the complete address as a single string
     */
    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        if (street != null && !street.isBlank()) {
            address.append(street);
            if (houseNumber != null && !houseNumber.isBlank()) {
                address.append(" ").append(houseNumber);
            }
        }
        if (postalCode != null && !postalCode.isBlank()) {
            if (address.length() > 0) address.append(", ");
            address.append(postalCode);
        }
        if (city != null && !city.isBlank()) {
            if (address.length() > 0) address.append(" ");
            address.append(city);
        }
        if (country != null && !country.isBlank()) {
            if (address.length() > 0) address.append(", ");
            address.append(country);
        }
        return address.toString();
    }
    
    /**
     * Returns the practice owner with title
     */
    public String getOwnerWithTitle() {
        if (ownerTitle != null && !ownerTitle.isBlank() && ownerName != null && !ownerName.isBlank()) {
            return ownerTitle + " " + ownerName;
        } else if (ownerName != null && !ownerName.isBlank()) {
            return ownerName;
        }
        return "";
    }
}


