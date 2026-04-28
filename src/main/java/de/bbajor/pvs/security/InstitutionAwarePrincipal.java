package de.bbajor.pvs.security;

import java.util.Optional;

/**
 * Marker interface for principals that can provide an institution id.
 * <p>
 * For multi-tenant isolation, prefer getting the institution id from the authenticated principal
 * (JWT claim / mapped identity) instead of doing database lookups on every request.
 * </p>
 */
public interface InstitutionAwarePrincipal {

    Optional<Long> getInstitutionId();
}

