package com.example.icd10gm.model;

import jakarta.persistence.*;
import lombok.*; // Lombok Import
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "primaerschluessel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Generiert einen Konstruktor mit allen Feldern (id und schluesselnummer)
@ToString(exclude = {"icdEintraegePrimaer1", "icdEintraegePrimaer2"})
@EqualsAndHashCode(of = "schluesselnummer") // Generiert equals/hashCode basierend auf der schluesselnummer
public class Primaerschluessel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "primaerschluessel_id")
    private Long id;

    @Column(name = "schluesselnummer", unique = true, nullable = false, length = 20)
    private String schluesselnummer;

    @ManyToMany(mappedBy = "primaerschluessel1")
    private Set<IcdEntry> icdEintraegePrimaer1 = new HashSet<>();

    @ManyToMany(mappedBy = "primaerschluessel2")
    private Set<IcdEntry> icdEintraegePrimaer2 = new HashSet<>();

    // Spezieller Konstruktor für die Erzeugung, wenn nur die Schlüsselnummer bekannt ist (ohne ID)
    public Primaerschluessel(String schluesselnummer) {
        this.schluesselnummer = schluesselnummer;
    }
}