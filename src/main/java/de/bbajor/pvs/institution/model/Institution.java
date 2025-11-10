package de.bbajor.pvs.institution.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.location.model.Location;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing an institution (Einrichtung/Kunde).
 * <p>
 * Each institution represents a customer organization (e.g., medical center, clinic).
 * In the multi-database architecture, each institution has its own database.
 * An institution can have multiple locations where patients are treated.
 * </p>
 * <p>
 * This entity is stored in the central registry database.
 * </p>
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "institution", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "institution_code" })
})
public class Institution extends BasicEntity<Long> {

    /**
     * Unique institution code used for login.
     * Should be a pseudorandom generated identifier (e.g., "INST-2024-A7B3").
     */
    @NotBlank
    @Column(name = "institution_code", nullable = false, unique = true, length = 50)
    private String institutionCode;

    /**
     * Display name of the institution (e.g., "Augenzentrum Dr. Müller").
     */
    @NotBlank
    @Column(name = "institution_name", nullable = false, length = 200)
    private String institutionName;

    /**
     * Whether this institution is active.
     * Inactive institutions cannot log in.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Optional description or notes about this institution.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Street address of the institution's headquarters.
     */
    @Column(name = "street", length = 255)
    private String street;

    /**
     * House number of the institution's headquarters.
     */
    @Column(name = "house_number", length = 50)
    private String houseNumber;

    /**
     * Postal code of the institution's headquarters.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * City of the institution's headquarters.
     */
    @Column(name = "city", length = 255)
    private String city;

    /**
     * Country of the institution's headquarters.
     */
    @Column(name = "country", length = 255)
    private String country;

    /**
     * Contact phone number of the institution.
     */
    @Column(name = "phone", length = 50)
    private String phone;

    /**
     * Contact fax number of the institution.
     */
    @Column(name = "fax", length = 50)
    private String fax;

    /**
     * Contact email address of the institution.
     */
    @Column(name = "email", length = 255)
    private String email;

    /**
     * Legal form and company name (e.g., "GmbH", "MVZ", "Klinikum").
     * This is the official company name used for legal/administrative purposes.
     */
    @Column(name = "company_name", length = 255)
    private String companyName;

    /**
     * Tax ID or registration number (e.g., "Steuernummer", "Handelsregisternummer").
     */
    @Column(name = "tax_id", length = 100)
    private String taxId;

    /**
     * Database name for this institution's dedicated database.
     * Format: "pvs_inst_{institutionCode}"
     * Example: "pvs_inst_abc123"
     */
    @Column(name = "database_name", nullable = false, unique = true, length = 100)
    private String databaseName;

    /**
     * Docker container name for this institution's PostgreSQL database.
     * Format: "postgres-inst-{institutionCode}"
     * Example: "postgres-inst-abc123"
     */
    @Column(name = "container_name", nullable = false, unique = true, length = 100)
    private String containerName;

    /**
     * Database connection port (dynamically assigned).
     * Example: 5433, 5434, etc.
     */
    @Column(name = "database_port")
    private Integer databasePort;

    /**
     * Database connection password (encrypted/hashed).
     * Each institution has its own database password.
     */
    @Column(name = "database_password", length = 255)
    private String databasePassword;

    /**
     * Remote LLM configuration - each institution can have its own AI model.
     * If enabled, the institution uses its own remote LLM instead of the system-wide one.
     */
    @Column(name = "remote_llm_enabled")
    private Boolean remoteLlmEnabled = false;

    /**
     * Remote LLM API URL (e.g., "https://api.aleph-alpha.com/complete").
     */
    @Column(name = "remote_llm_api_url", length = 500)
    private String remoteLlmApiUrl;

    /**
     * Remote LLM API Key (encrypted/hashed).
     */
    @Column(name = "remote_llm_api_key", length = 500)
    private String remoteLlmApiKey;

    /**
     * Remote LLM monthly quota for this institution.
     */
    @Column(name = "remote_llm_monthly_quota")
    private Integer remoteLlmMonthlyQuota;
    
    /**
     * Website URL of the institution (for QR code generati?on).
     */
    @Column(name = "website_url", length = 500)
    private String websiteUrl;
    
    /**
     * Watermark image for PDF reports (stored as BLOB).
     */
    @Column(name = "watermark_image", columnDefinition = "BYTEA")
    private byte[] watermarkImage;

    /**
     * Layout customization: Primary brand color (hex format, e.g., "#1976d2").
     */
    @Column(name = "layout_primary_color", length = 7)
    private String layoutPrimaryColor;

    /**
     * Layout customization: Secondary brand color (hex format).
     */
    @Column(name = "layout_secondary_color", length = 7)
    private String layoutSecondaryColor;

    /**
     * Layout customization: Background color for main content areas (hex format).
     */
    @Column(name = "layout_background_color", length = 7)
    private String layoutBackgroundColor;

    /**
     * Layout customization: Primary text color (hex format).
     */
    @Column(name = "layout_text_color", length = 7)
    private String layoutTextColor;

    /**
     * Layout customization: Accent color for highlights and call-to-action elements (hex format).
     */
    @Column(name = "layout_accent_color", length = 7)
    private String layoutAccentColor;

    /**
     * Layout customization: Border radius for UI elements (e.g., "8px", "0.5rem").
     */
    @Column(name = "layout_border_radius", length = 10)
    private String layoutBorderRadius;

    /**
     * Layout customization: Font family for UI text (e.g., "Arial, sans-serif").
     */
    @Column(name = "layout_font_family", length = 100)
    private String layoutFontFamily;

    /**
     * Note: Locations are stored in the institution's own database,
     * not in the registry database where this Institution entity is stored.
     * Therefore, there is no @OneToMany relationship here - the relationship
     * is maintained only in the institution's database via Location.institutionId.
     * 
     * However, for development and testing, we keep a @OneToMany relationship
     * for easier access during migration.
     */
    @OneToMany(mappedBy = "institution", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Location> locations = new ArrayList<>();

    /**
     * Tenant specific settings; acts as anchor when moving to database-per-tenant.
     */
    @OneToOne(mappedBy = "institution", fetch = FetchType.LAZY, orphanRemoval = true)
    private InstitutionSettings settings;

    /**
     * Returns the complete address as a single string.
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
     * Returns the institution name with company name if available.
     */
    public String getFullName() {
        if (companyName != null && !companyName.isBlank()) {
            return institutionName + " " + companyName;
        }
        return institutionName;
    }

    public InstitutionSettings ensureSettings() {
        if (settings == null) {
            settings = InstitutionSettings.createDefault(this);
        }
        return settings;
    }

    public Institution setSettings(InstitutionSettings settings) {
        this.settings = settings;
        if (settings != null && settings.getInstitution() != this) {
            settings.setInstitution(this);
        }
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", institutionName, institutionCode);
    }
}

