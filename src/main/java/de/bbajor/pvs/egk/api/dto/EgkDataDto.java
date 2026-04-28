package de.bbajor.pvs.egk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO für eGK-Daten vom Client-Agent.
 * Enthält sowohl persönliche Versichertendaten als auch Versicherungsdaten.
 */
@Data
public class EgkDataDto {

    // Persönliche Versichertendaten
    @NotBlank(message = "Versicherten-ID ist erforderlich")
    @JsonProperty("versichertenId")
    private String versichertenId;

    @NotBlank(message = "Vorname ist erforderlich")
    @JsonProperty("vorname")
    private String vorname;

    @NotBlank(message = "Nachname ist erforderlich")
    @JsonProperty("nachname")
    private String nachname;

    @JsonProperty("geburtsdatum")
    private LocalDate geburtsdatum;

    @JsonProperty("geschlecht")
    private String geschlecht;

    // Adresse
    @Valid
    @JsonProperty("adresse")
    private AdresseDto adresse;

    // Versicherungsdaten
    @JsonProperty("versicherungsschutz")
    private VersicherungsschutzDto versicherungsschutz;

    @Data
    public static class AdresseDto {
        @JsonProperty("strasse")
        private String strasse;

        @JsonProperty("hausnummer")
        private String hausnummer;

        @JsonProperty("postleitzahl")
        private String postleitzahl;

        @JsonProperty("ort")
        private String ort;

        @JsonProperty("land")
        private String land;
    }

    @Data
    public static class VersicherungsschutzDto {
        @JsonProperty("beginn")
        private LocalDate beginn;

        @JsonProperty("kostentraeger")
        private KostentraegerDto kostentraeger;

        @JsonProperty("versichertenart")
        private String versichertenart;

        @JsonProperty("wop")
        private String wop;
    }

    @Data
    public static class KostentraegerDto {
        @JsonProperty("kostentraegerkennung")
        private String kostentraegerkennung;

        @JsonProperty("name")
        private String name;

        @JsonProperty("laendercode")
        private String laendercode;

        @JsonProperty("abrechnenderKostentraeger")
        private KostentraegerDto abrechnenderKostentraeger;
    }
}
