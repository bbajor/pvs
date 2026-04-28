package de.bbajor.pvs.institution.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.single-practice")
public record SinglePracticeProperties(
        boolean enabled,
        boolean autoProvisionInstitution,
        String defaultInstitutionCode,
        String defaultInstitutionName,
        String defaultInstitutionDescription) {

    public SinglePracticeProperties {
        if (defaultInstitutionCode == null || defaultInstitutionCode.isBlank()) {
            defaultInstitutionCode = "PRAX-001";
        }
        if (defaultInstitutionName == null || defaultInstitutionName.isBlank()) {
            defaultInstitutionName = "Praxis";
        }
        if (defaultInstitutionDescription == null) {
            defaultInstitutionDescription = "Single-practice default institution";
        }
    }
}

