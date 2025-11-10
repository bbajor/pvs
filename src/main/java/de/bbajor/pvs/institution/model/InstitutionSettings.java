package de.bbajor.pvs.institution.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Locale;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

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

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "smtp_config_json")
    private String smtpConfigJson;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public static InstitutionSettings createDefault(Institution institution) {
        return new InstitutionSettings()
                .setInstitution(institution)
                .setDisplayName(institution.getInstitutionName())
                .setLegalName(institution.getCompanyName())
                .setContactEmail(institution.getEmail())
                .setContactPhone(institution.getPhone());
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
