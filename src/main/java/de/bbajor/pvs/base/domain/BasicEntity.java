package de.bbajor.pvs.base.domain;

import java.util.Objects;

import org.hibernate.proxy.HibernateProxy;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

@MappedSuperclass
public abstract class BasicEntity<ID> {

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
    public final boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null)
            return false;

        // Hibernate-Proxys sauber auflösen
        Class<?> thisClass = this instanceof HibernateProxy
                ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                : getClass();

        Class<?> otherClass = o instanceof HibernateProxy
                ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();

        if (!thisClass.equals(otherClass))
            return false;

        BasicEntity<ID> that = (BasicEntity<ID>) o;

        // equals nur auf Basis der ID, wenn vorhanden
        return id != null && id.equals(that.id);
    }

    @Override
    public final int hashCode() {
        // Verwende Klassenhash, um StackOverflow zu vermeiden
        // (weil id bei transienten Entities noch null ist)
        return Objects.hashCode(
                this instanceof HibernateProxy
                        ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
                        : getClass());
    }
}
