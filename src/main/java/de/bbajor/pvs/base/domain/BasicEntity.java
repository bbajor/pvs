package de.bbajor.pvs.base.domain;

import org.springframework.data.util.ProxyUtils;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public class BasicEntity<ID> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private ID id;

    public @Nullable ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    public void setVersion(Long version) {
        this.version = version;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "%s{id=%s}".formatted(getClass().getSimpleName(), getId());
    }

    @Override
    public int hashCode() {
        // Hashcode should never change during the lifetime of an object. Because of
        // this we can't use getId() to calculate the hashcode. Unless you have sets
        // with lots of entities in them, returning the same hashcode should not be a
        // problem.
        return ProxyUtils.getUserClass(getClass()).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        }

        var thisUserClass = ProxyUtils.getUserClass(getClass());
        var otherUserClass = ProxyUtils.getUserClass(obj);
        if (thisUserClass != otherUserClass) {
            return false;
        }

        var id = getId();
        return id != null && id.equals(((BasicEntity<?>) obj).getId());
    }

}
