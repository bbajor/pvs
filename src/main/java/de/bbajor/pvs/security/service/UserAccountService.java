package de.bbajor.pvs.security.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing user accounts.
 * All methods filter by current institution to ensure data isolation.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    /**
     * Find all users with a specific role for the current institution.
     * Filters by InstitutionContext to ensure data isolation.
     * 
     * @param role the role to filter by
     * @return list of users with the specified role for the current institution, empty list if no institution context
     */
    public List<UserAccount> findUsersByRole(String role) {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return Collections.emptyList();
        }

        return userAccountRepository.findAll().stream()
            .filter(user -> user.getRoles().contains(role))
            .filter(user -> user.getInstitution() != null && user.getInstitution().getId().equals(institutionId))
            .collect(Collectors.toList());
    }

    /**
     * Find all users for the current institution.
     * Filters by InstitutionContext to ensure data isolation.
     * 
     * @return list of users for the current institution, empty list if no institution context
     */
    public List<UserAccount> findAll() {
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            return Collections.emptyList();
        }

        return userAccountRepository.findAll().stream()
            .filter(user -> user.getInstitution() != null && user.getInstitution().getId().equals(institutionId))
            .collect(Collectors.toList());
    }
}
