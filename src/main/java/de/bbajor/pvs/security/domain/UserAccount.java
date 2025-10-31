package de.bbajor.pvs.security.domain;

import java.util.HashSet;
import java.util.Set;

import de.bbajor.pvs.base.domain.BasicEntity;
import de.bbajor.pvs.tenant.model.Tenant;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Entity
@Accessors(chain = true)
@Table(name = "user_account", uniqueConstraints = { 
    @UniqueConstraint(columnNames = { "tenant_id", "username" })
})
public class UserAccount extends BasicEntity<Long> {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    private Set<String> roles = new HashSet<>();

    @Column(name = "user_id")
    private String userId;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "email")
    private String email;

    /**
     * The tenant this user belongs to.
     * Null for super-admin users who can manage all tenants.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
}
