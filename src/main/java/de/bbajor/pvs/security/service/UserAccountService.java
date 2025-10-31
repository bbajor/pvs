package de.bbajor.pvs.security.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import lombok.RequiredArgsConstructor;

/**
 * Service for managing user accounts.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;

    /**
     * Find all users with a specific role.
     */
    public List<UserAccount> findUsersByRole(String role) {
        return userAccountRepository.findAll().stream()
            .filter(user -> user.getRoles().contains(role))
            .collect(Collectors.toList());
    }

    /**
     * Find all users.
     */
    public List<UserAccount> findAll() {
        return userAccountRepository.findAll();
    }
}
