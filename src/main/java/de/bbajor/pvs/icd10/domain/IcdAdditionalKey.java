package de.bbajor.pvs.icd10.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "icd_additional_key") // Updated table name
@Data
@ToString(exclude = "icdEntries")
@EqualsAndHashCode(of = "keyNumber")
public class IcdAdditionalKey { // Renamed class

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "icd_additional_key_id") // Updated column name
    private Long id;

    @Column(name = "key_number", unique = true, nullable = false, length = 20)
    private String keyNumber; // Field 6 (with exclamation mark)

    @ManyToMany(mappedBy = "icdAdditionalKeys") // Updated mappedBy name
    private Set<IcdEntry> icdEntries = new HashSet<>();

    public IcdAdditionalKey(String keyNumber) {
        this.keyNumber = keyNumber;
    }
}