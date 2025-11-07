package de.bbajor.pvs.institution.security;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.annotation.SpringComponent;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.institution.context.InstitutionContext;
import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Vaadin service initializer that sets InstitutionContext on every navigation.
 * <p>
 * Vaadin navigations happen client-side and don't trigger HTTP filters. This
 * listener ensures that InstitutionContext is set from the Authentication token
 * for every navigation.
 * </p>
 */
@SpringComponent
@RequiredArgsConstructor
public class VaadinInstitutionContextInitializer implements VaadinServiceInitListener, BeforeEnterListener {

    private static final Logger log = LoggerFactory.getLogger(VaadinInstitutionContextInitializer.class);

    private final UserAccountRepository userAccountRepository;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiInitEvent -> {
            uiInitEvent.getUI().addBeforeEnterListener(this);
            log.debug("VaadinInstitutionContextInitializer: Added BeforeEnterListener to UI");
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Set InstitutionContext from Authentication on every navigation
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            } else {
                log.warn("InstitutionAuthenticationToken has no institutionId - clearing InstitutionContext");
                InstitutionContext.clear();
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session and is now UsernamePasswordAuthenticationToken
            // Try to get institution from UserAccount via repository
            try {
                // Get username from adapter
                String username = adapter.getUsername();
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);

                if (userAccount != null) {
                    // Priority 1: Get institution ID (new model)
                    if (userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                        log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                                institutionId, userAccount.getInstitution().getInstitutionCode());
                    } else {
                        log.debug("UserAccount has no institution - clearing InstitutionContext");
                        InstitutionContext.clear();
                    }
                } else {
                    log.debug("UserAccount not found - clearing InstitutionContext");
                    InstitutionContext.clear();
                }
            } catch (Exception e) {
                log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
                InstitutionContext.clear();
            }
        } else if (authentication != null) {
            // Not a InstitutionAuthenticationToken and principal is not UserAccountUserDetailsAdapter
            log.debug("Authentication type: {}, Principal type: {} - clearing InstitutionContext",
                    authentication.getClass().getSimpleName(),
                    authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getSimpleName() : "null");
            InstitutionContext.clear();
        } else {
            // No authentication - clear context
            InstitutionContext.clear();
        }
    }
}

