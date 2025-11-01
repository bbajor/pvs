package de.bbajor.pvs.tenant.model;

import de.bbajor.pvs.base.domain.BasicEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Entity representing a tenant (Praxis/MVZ/Klinik).
 * Each tenant has isolated data from other tenants.
 */
@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "tenant", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenant_code" })
})
public class Tenant extends BasicEntity<Long> {

    /**
     * Unique tenant code used for login.
     * Should be a pseudorandom generated identifier (e.g., "PRAX-2024-A7B3").
     */
    @NotBlank
    @Column(name = "tenant_code", nullable = false, unique = true, length = 50)
    private String tenantCode;

    /**
     * Display name of the tenant (e.g., "Augenarztpraxis Dr. Müller").
     */
    @NotBlank
    @Column(name = "tenant_name", nullable = false, length = 200)
    private String tenantName;

    /**
     * Whether this tenant is active.
     * Inactive tenants cannot log in.
     */
    @Column(name = "active", nullable = false)
    private boolean active = true;

    /**
     * Optional description or notes about this tenant.
     */
    @Column(name = "description", length = 1000)
    private String description;

    @Override
    public String toString() {
        return String.format("%s (%s)", tenantName, tenantCode);
    }
}
