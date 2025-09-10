package de.bbajor.pvs.base.domain;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

@MappedSuperclass
public class BasicEntity<ID> extends AbstractEntity<ID> {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private ID id;

    @Override
    public @Nullable ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
    
}
