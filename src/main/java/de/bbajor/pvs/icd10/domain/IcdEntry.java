package com.example.icd10gm.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "icd_entry")
@Data
@ToString(exclude = {"icdPrimaryKeys1", "icdStarKeys", "icdAdditionalKeys", "icdPrimaryKeys2"}) // Updated exclude names
@EqualsAndHashCode(of = "id")
public class IcdEntry {

    @Id
    @Column(name = "id")
    private Integer id; // BfArM internal sequential number (Field 2)

    @Column(name = "coding_type", nullable = false)
    private Integer codingType; // Field 1

    @Column(name = "print_indicator", nullable = false)
    private Integer printIndicator; // Field 3

    @Column(name = "text_content", nullable = false, columnDefinition = "TEXT")
    private String textContent; // Field 8

    // Relationships to key numbers via join tables

    // Relationship to Primary Key 1 (Field 4)
    @ManyToMany
    @JoinTable(
        name = "icd_primary_key1",
        joinColumns = @JoinColumn(name = "icd_entry_id"),
        inverseJoinColumns = @JoinColumn(name = "icd_primary_key_id") // Updated inverseJoinColumn name
    )
    private Set<IcdPrimaryKey> icdPrimaryKeys1 = new HashSet<>(); // Updated field name

    // Relationship to Star Key (Field 5)
    @ManyToMany
    @JoinTable(
        name = "icd_star_key",
        joinColumns = @JoinColumn(name = "icd_entry_id"),
        inverseJoinColumns = @JoinColumn(name = "icd_star_key_id") // Updated inverseJoinColumn name
    )
    private Set<IcdStarKey> icdStarKeys = new HashSet<>(); // Updated field name

    // Relationship to Additional Key (Field 6)
    @ManyToMany
    @JoinTable(
        name = "icd_additional_key",
        joinColumns = @JoinColumn(name = "icd_entry_id"),
        inverseJoinColumns = @JoinColumn(name = "icd_additional_key_id") // Updated inverseJoinColumn name
    )
    private Set<IcdAdditionalKey> icdAdditionalKeys = new HashSet<>(); // Updated field name

    // Relationship to Primary Key 2 (Field 7)
    @ManyToMany
    @JoinTable(
        name = "icd_primary_key2",
        joinColumns = @JoinColumn(name = "icd_entry_id"),
        inverseJoinColumns = @JoinColumn(name = "icd_primary_key_id") // Updated inverseJoinColumn name
    )
    private Set<IcdPrimaryKey> icdPrimaryKeys2 = new HashSet<>(); // Updated field name
}