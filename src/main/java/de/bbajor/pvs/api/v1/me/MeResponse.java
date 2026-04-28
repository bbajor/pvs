package de.bbajor.pvs.api.v1.me;

public record MeResponse(
        String userId,
        String preferredUsername,
        String fullName,
        Long institutionId) {
}

