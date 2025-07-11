package de.bbajor.pvs.icd10.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "icd_star_key") // Updated table name
@Data
@ToString(exclude = "icdEntries")
@EqualsAndHashCode(of = "keyNumber")
public class IcdStarKey { // Renamed class

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "icd_star_key_id") // Updated column name
    private Long id;

    @Column(name = "key_number", unique = true, nullable = false, length = 20)
    private String keyNumber; // Field 5 (with star)

    @ManyToMany(mappedBy = "icdStarKeys") // Updated mappedBy name
    private Set<IcdEntry> icdEntries = new HashSet<>();

    public IcdStarKey(String keyNumber) {
        this.keyNumber = keyNumber;
    }
}