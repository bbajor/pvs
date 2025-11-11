package de.bbajor.pvs.institution.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.institution.persistence.InstitutionFilterConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Tenant-specific configuration that lives inside the tenant database.
 * <p>
 * For the transition period it is persisted alongside {@link Institution}
 * in the monolithic schema. Once each tenant runs on its own database the
 * settings table becomes the anchor for domain data instead of {@code institution}.
 * </p>
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@FilterDef(name = "institutionFilter", parameters = @ParamDef(name = "institutionId", type = Long.class))
@Filter(name = InstitutionFilterConstants.FILTER_NAME, condition = InstitutionFilterConstants.FILTER_CONDITION)
@Table(name = "institution_settings", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "institution_id" })
})
public class InstitutionSettings extends BasicEntity<Long> {

    private static final String DEFAULT_TIMEZONE = ZoneId.of("Europe/Berlin").getId();
    private static final String DEFAULT_LOCALE = Locale.GERMANY.toLanguageTag();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "institution_id", nullable = false, unique = true)
    private Institution institution;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "legal_name", length = 255)
    private String legalName;

    @Column(name = "description")
    private String description;

    @Column(name = "street", length = 255)
    private String street;

    @Column(name = "house_number", length = 50)
    private String houseNumber;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "country", length = 255)
    private String country;

    @Column(name = "timezone", nullable = false, length = 100)
    private String timezone = DEFAULT_TIMEZONE;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale = DEFAULT_LOCALE;

    @Column(name = "demo_mode", nullable = false)
    private boolean demoMode = false;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "contact_fax", length = 50)
    private String contactFax;

    @Column(name = "tax_id", length = 100)
    private String taxId;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "smtp_config_json")
    private String smtpConfigJson;

    @Column(name = "notes")
    private String notes;

    @Column(name = "remote_llm_enabled", nullable = false)
    private boolean remoteLlmEnabled = false;

    @Column(name = "remote_llm_api_url", length = 500)
    private String remoteLlmApiUrl;

    @Column(name = "remote_llm_api_key", length = 500)
    private String remoteLlmApiKey;

    @Column(name = "remote_llm_monthly_quota")
    private Integer remoteLlmMonthlyQuota;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Lob
    @Column(name = "watermark_image")
    private byte[] watermarkImage;

    @Column(name = "layout_primary_color", length = 7)
    private String layoutPrimaryColor;

    @Column(name = "layout_secondary_color", length = 7)
    private String layoutSecondaryColor;

    @Column(name = "layout_background_color", length = 7)
    private String layoutBackgroundColor;

    @Column(name = "layout_text_color", length = 7)
    private String layoutTextColor;

    @Column(name = "layout_accent_color", length = 7)
    private String layoutAccentColor;

    @Column(name = "layout_border_radius", length = 10)
    private String layoutBorderRadius;

    @Column(name = "layout_font_family", length = 100)
    private String layoutFontFamily;

    @Column(name = "kbv_last_import_quarter", length = 20)
    private String kbvLastImportQuarter;

    @Column(name = "kbv_last_import_version", length = 50)
    private String kbvLastImportVersion;

    @Column(name = "kbv_last_imported_at")
    private OffsetDateTime kbvLastImportedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static InstitutionSettings createDefault(Institution institution) {
        return new InstitutionSettings()
                .setInstitution(institution)
                .setDisplayName(institution.getInstitutionName());
    }

    public void setWatermarkImage(byte[] watermarkImage) {
        this.watermarkImage = watermarkImage != null ? watermarkImage.clone() : null;
    }

    public byte[] getWatermarkImage() {
        return watermarkImage != null ? watermarkImage.clone() : null;
    }

    @PrePersist
    void onPersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("UTC"));
        createdAt = now;
        updatedAt = now;
        if (timezone == null || timezone.isBlank()) {
            timezone = DEFAULT_TIMEZONE;
        }
        if (locale == null || locale.isBlank()) {
            locale = DEFAULT_LOCALE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneId.of("UTC"));
    }
}
