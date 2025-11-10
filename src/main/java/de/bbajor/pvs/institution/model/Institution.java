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

    public String getFullAddress() {
        StringBuilder address = new StringBuilder();
        String street = getStreet();
        String houseNumber = getHouseNumber();
        if (street != null && !street.isBlank()) {
            address.append(street);
            if (houseNumber != null && !houseNumber.isBlank()) {
                address.append(" ").append(houseNumber);
            }
        }
        String postalCode = getPostalCode();
        if (postalCode != null && !postalCode.isBlank()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(postalCode);
        }
        String city = getCity();
        if (city != null && !city.isBlank()) {
            if (address.length() > 0) {
                address.append(" ");
            }
            address.append(city);
        }
        String country = getCountry();
        if (country != null && !country.isBlank()) {
            if (address.length() > 0) {
                address.append(", ");
            }
            address.append(country);
        }
        return address.toString();
    }

    public String getFullName() {
        String companyName = getCompanyName();
        if (companyName != null && !companyName.isBlank()) {
            return institutionName + " " + companyName;
        }
        return institutionName;
    }

    public Institution setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
        if (settings != null && institutionName != null && !institutionName.isBlank()) {
            settings.setDisplayName(institutionName);
        }
        return this;
    }

    public InstitutionSettings ensureSettings() {
        if (settings == null) {
            settings = InstitutionSettings.createDefault(this);
        }
        if ((settings.getDisplayName() == null || settings.getDisplayName().isBlank())
                && institutionName != null && !institutionName.isBlank()) {
            settings.setDisplayName(institutionName);
        }
        return settings;
    }

    public Institution setSettings(InstitutionSettings settings) {
        this.settings = settings;
        if (settings != null && settings.getInstitution() != this) {
            settings.setInstitution(this);
        }
        if (settings != null && institutionName != null && !institutionName.isBlank()
                && (settings.getDisplayName() == null || settings.getDisplayName().isBlank())) {
            settings.setDisplayName(institutionName);
        }
        return this;
    }

    public String getDescription() {
        return settings != null ? settings.getDescription() : null;
    }

    public Institution setDescription(String description) {
        ensureSettings().setDescription(description);
        return this;
    }

    public String getStreet() {
        return settings != null ? settings.getStreet() : null;
    }

    public Institution setStreet(String street) {
        ensureSettings().setStreet(street);
        return this;
    }

    public String getHouseNumber() {
        return settings != null ? settings.getHouseNumber() : null;
    }

    public Institution setHouseNumber(String houseNumber) {
        ensureSettings().setHouseNumber(houseNumber);
        return this;
    }

    public String getPostalCode() {
        return settings != null ? settings.getPostalCode() : null;
    }

    public Institution setPostalCode(String postalCode) {
        ensureSettings().setPostalCode(postalCode);
        return this;
    }

    public String getCity() {
        return settings != null ? settings.getCity() : null;
    }

    public Institution setCity(String city) {
        ensureSettings().setCity(city);
        return this;
    }

    public String getCountry() {
        return settings != null ? settings.getCountry() : null;
    }

    public Institution setCountry(String country) {
        ensureSettings().setCountry(country);
        return this;
    }

    public String getPhone() {
        return settings != null ? settings.getContactPhone() : null;
    }

    public Institution setPhone(String phone) {
        ensureSettings().setContactPhone(phone);
        return this;
    }

    public String getFax() {
        return settings != null ? settings.getContactFax() : null;
    }

    public Institution setFax(String fax) {
        ensureSettings().setContactFax(fax);
        return this;
    }

    public String getEmail() {
        return settings != null ? settings.getContactEmail() : null;
    }

    public Institution setEmail(String email) {
        ensureSettings().setContactEmail(email);
        return this;
    }

    public String getCompanyName() {
        return settings != null ? settings.getLegalName() : null;
    }

    public Institution setCompanyName(String companyName) {
        ensureSettings().setLegalName(companyName);
        return this;
    }

    public String getTaxId() {
        return settings != null ? settings.getTaxId() : null;
    }

    public Institution setTaxId(String taxId) {
        ensureSettings().setTaxId(taxId);
        return this;
    }

    public Boolean getRemoteLlmEnabled() {
        return settings != null ? settings.isRemoteLlmEnabled() : Boolean.FALSE;
    }

    public Institution setRemoteLlmEnabled(Boolean remoteLlmEnabled) {
        ensureSettings().setRemoteLlmEnabled(Boolean.TRUE.equals(remoteLlmEnabled));
        return this;
    }

    public String getRemoteLlmApiUrl() {
        return settings != null ? settings.getRemoteLlmApiUrl() : null;
    }

    public Institution setRemoteLlmApiUrl(String remoteLlmApiUrl) {
        ensureSettings().setRemoteLlmApiUrl(remoteLlmApiUrl);
        return this;
    }

    public String getRemoteLlmApiKey() {
        return settings != null ? settings.getRemoteLlmApiKey() : null;
    }

    public Institution setRemoteLlmApiKey(String remoteLlmApiKey) {
        ensureSettings().setRemoteLlmApiKey(remoteLlmApiKey);
        return this;
    }

    public Integer getRemoteLlmMonthlyQuota() {
        return settings != null ? settings.getRemoteLlmMonthlyQuota() : null;
    }

    public Institution setRemoteLlmMonthlyQuota(Integer remoteLlmMonthlyQuota) {
        ensureSettings().setRemoteLlmMonthlyQuota(remoteLlmMonthlyQuota);
        return this;
    }

    public String getWebsiteUrl() {
        return settings != null ? settings.getWebsiteUrl() : null;
    }

    public Institution setWebsiteUrl(String websiteUrl) {
        ensureSettings().setWebsiteUrl(websiteUrl);
        return this;
    }

    public byte[] getWatermarkImage() {
        return settings != null ? settings.getWatermarkImage() : null;
    }

    public Institution setWatermarkImage(byte[] watermarkImage) {
        ensureSettings().setWatermarkImage(watermarkImage);
        return this;
    }

    public String getLayoutPrimaryColor() {
        return settings != null ? settings.getLayoutPrimaryColor() : null;
    }

    public Institution setLayoutPrimaryColor(String layoutPrimaryColor) {
        ensureSettings().setLayoutPrimaryColor(layoutPrimaryColor);
        return this;
    }

    public String getLayoutSecondaryColor() {
        return settings != null ? settings.getLayoutSecondaryColor() : null;
    }

    public Institution setLayoutSecondaryColor(String layoutSecondaryColor) {
        ensureSettings().setLayoutSecondaryColor(layoutSecondaryColor);
        return this;
    }

    public String getLayoutBackgroundColor() {
        return settings != null ? settings.getLayoutBackgroundColor() : null;
    }

    public Institution setLayoutBackgroundColor(String layoutBackgroundColor) {
        ensureSettings().setLayoutBackgroundColor(layoutBackgroundColor);
        return this;
    }

    public String getLayoutTextColor() {
        return settings != null ? settings.getLayoutTextColor() : null;
    }

    public Institution setLayoutTextColor(String layoutTextColor) {
        ensureSettings().setLayoutTextColor(layoutTextColor);
        return this;
    }

    public String getLayoutAccentColor() {
        return settings != null ? settings.getLayoutAccentColor() : null;
    }

    public Institution setLayoutAccentColor(String layoutAccentColor) {
        ensureSettings().setLayoutAccentColor(layoutAccentColor);
        return this;
    }

    public String getLayoutBorderRadius() {
        return settings != null ? settings.getLayoutBorderRadius() : null;
    }

    public Institution setLayoutBorderRadius(String layoutBorderRadius) {
        ensureSettings().setLayoutBorderRadius(layoutBorderRadius);
        return this;
    }

    public String getLayoutFontFamily() {
        return settings != null ? settings.getLayoutFontFamily() : null;
    }

    public Institution setLayoutFontFamily(String layoutFontFamily) {
        ensureSettings().setLayoutFontFamily(layoutFontFamily);
        return this;
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", institutionName, institutionCode);
    }
}

