package de.bbajor.pvs.location.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.model.Institution;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;

/**
 * Entity representing a location (Standort/Praxis-Standort) of an institution.
 * <p>
 * An institution can have multiple locations where patients are treated.
 * For example:
 * - Medical centers (MVZ) can have multiple practice branches
 * - Clinics can have multiple sites/locations
 * </p>
 * <p>
 * This entity replaces the old "Practice" entity and is stored in the
 * institution's own database (not the registry database).
 * </p>
 * <p>
 * Note: The institution reference is stored as institution ID only (no FK
 * constraint), since institutions are stored in the registry database.
 * </p>
 */
@Getter
@Setter
@Entity
@Filter(name = InstitutionFilterConstants.FILTER_NAME, condition = InstitutionFilterConstants.FILTER_CONDITION)
@Table(name = "location")
public class Location extends BasicEntity<Long> {

    /**
     * Name of the location (e.g., "Hauptsitz", "Zweigstelle Nord", "Praxis Berlin-Mitte").
     */
    private String locationName;

    /**
     * Street address
     */
    private String street;

    /**
     * House number
     */
    private String houseNumber;

    /**
     * Postal code
     */
    private String postalCode;

    /**
     * City
     */
    private String city;

    /**
     * Country
     */
    private String country;
    
    /**
     * Location owner/manager information
     */
    private String ownerName;
    
    /**
     * Owner title (e.g., "Dr. med.")
     */
    private String ownerTitle;
    
    /**
     * German healthcare system identifiers
     */
    private String lanr; // Leistungserbringer-Abrechnungsnummer (Doctor ID)
    private String bsnr; // Betriebsstättennummer (Practice ID)
    
    /**
     * Contact information
     */
    private String phone;
    private String fax;
    private String email;
    
    /**
     * Additional information
     */
    private String additionalInfo;
    
    /**
     * Whether this location is active.
     * Inactive locations cannot be selected for new patients or appointments.
     */
    private boolean active = true;
    
    /**
     * The institution this location belongs to.
     * <p>
     * Note: This is stored as institution ID reference only (no FK constraint),
     * since institutions are stored in the registry database, not the
     * institution's own database.
     * </p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "institution_id", nullable = false)
    private Institution institution;
    
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
     * Returns the location owner with title
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

