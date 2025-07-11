package de.bbajor.pvs.icd10.domain;


import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "icd_primary_key") // Updated table name
@Data
@ToString(exclude = {"icdEntriesPrimary1", "icdEntriesPrimary2"})
@EqualsAndHashCode(of = "keyNumber")
public class IcdPrimaryKey { // Renamed class

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "icd_primary_key_id") // Updated column name
    private Long id;

    @Column(name = "key_number", unique = true, nullable = false, length = 20)
    private String keyNumber; // Field 4 (potentially with cross)

    // Optional: Reverse relationships, if you want to navigate from the key to the entries
    @ManyToMany(mappedBy = "icdPrimaryKeys1") // Updated mappedBy name
    private Set<IcdEntry> icdEntriesPrimary1 = new HashSet<>();

    @ManyToMany(mappedBy = "icdPrimaryKeys2") // Updated mappedBy name
    private Set<IcdEntry> icdEntriesPrimary2 = new HashSet<>();

    public IcdPrimaryKey(String keyNumber) {
        this.keyNumber = keyNumber;
    }
}