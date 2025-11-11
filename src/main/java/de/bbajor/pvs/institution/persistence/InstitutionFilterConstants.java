package de.bbajor.pvs.institution.persistence;

/**
 * Shared constants for Hibernate tenant filters.
 */
public final class InstitutionFilterConstants {

    private InstitutionFilterConstants() {
    }

    public static final String FILTER_NAME = "institutionFilter";
    public static final String PARAM_NAME = "institutionId";
    public static final String FILTER_CONDITION = "institution_id = :" + PARAM_NAME;
}
