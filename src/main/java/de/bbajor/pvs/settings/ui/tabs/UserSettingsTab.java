package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.location.model.Location;
import de.bbajor.pvs.location.service.LocationService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Tab for managing users of the current institution.
 * Allows creating, editing, activating/deactivating users and assigning them to locations.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class UserSettingsTab extends VerticalLayout {

    private final UserAccountRepository userAccountRepository;
    private final LocationService locationService;
    private final InstitutionRepository institutionRepository;

    private Grid<UserAccount> userGrid;
    private Button createButton;
    
    private List<UserAccount> allUsers;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // Ensure InstitutionContext is set (important for Institutionsadmins)
        ensureInstitutionContext();

        // Check if InstitutionContext is set
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("Benutzerverwaltung");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        H3 title = new H3("Benutzerverwaltung");

        // Button section above grid
        createButton = new Button("Erstellen", e -> openUserDialog(null));
        createButton.setIcon(VaadinIcon.PLUS.create());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttonSection = new HorizontalLayout(createButton);
        buttonSection.setSpacing(true);
        buttonSection.setPadding(true);

        // Initialize grid
        userGrid = new Grid<>(UserAccount.class, false);
        userGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        
        // Combined renderer for username, name, email
        userGrid.addColumn(new ComponentRenderer<>(ua -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            
            // Username
            String username = ua.getUsername() != null ? ua.getUsername() : "-";
            Span usernameSpan = new Span(username);
            usernameSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            layout.add(usernameSpan);
            
            // Name
            String name = ua.getFullName() != null ? ua.getFullName() : "-";
            Span nameSpan = new Span(name);
            nameSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            layout.add(nameSpan);
            
            // Email
            String email = ua.getEmail() != null ? ua.getEmail() : "-";
            Span emailSpan = new Span(email);
            emailSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
            layout.add(emailSpan);
            
            return layout;
        })).setHeader("Benutzer").setSortable(true).setAutoWidth(true);
        
        // Role renderer with readable names
        userGrid.addColumn(new ComponentRenderer<>(ua -> {
            if (ua.getRoles() == null || ua.getRoles().isEmpty()) {
                return new Span("-");
            }
            String role = ua.getRoles().iterator().next(); // Only one role per user
            String roleLabel = translateRole(role);
            Span span = new Span(roleLabel);
            return span;
        })).setHeader("Rolle").setAutoWidth(true);
        
        // Location renderer
        userGrid.addColumn(new ComponentRenderer<>(ua -> {
            if (ua.getPreferredLocation() == null) {
                return new Span("-");
            }
            Location loc = ua.getPreferredLocation();
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            
            String name = loc.getLocationName() != null ? loc.getLocationName() : "-";
            Span nameSpan = new Span(name);
            nameSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            layout.add(nameSpan);
            
            String address = loc.getFullAddress() != null ? loc.getFullAddress() : "";
            if (!address.isEmpty()) {
                Span addressSpan = new Span(address);
                addressSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                layout.add(addressSpan);
            }
            
            return layout;
        })).setHeader("Standort").setAutoWidth(true);
        
        // Actions column with Edit and Deactivate/Activate buttons
        userGrid.addComponentColumn(ua -> {
            HorizontalLayout buttonLayout = new HorizontalLayout();
            buttonLayout.setSpacing(true);
            
            Button editButton = new Button("Bearbeiten", e -> openUserDialog(ua));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
            
            Button toggleButton = new Button(
                ua.isEnabled() ? "Deaktivieren" : "Aktivieren",
                e -> toggleUserStatus(ua)
            );
            toggleButton.addThemeVariants(
                ua.isEnabled() ? ButtonVariant.LUMO_ERROR : ButtonVariant.LUMO_SUCCESS,
                ButtonVariant.LUMO_SMALL
            );
            
            buttonLayout.add(editButton, toggleButton);
            return buttonLayout;
        }).setHeader("Aktionen").setAutoWidth(true);
        
        userGrid.setSizeFull();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        add(title, buttonSection, userGrid);
        expand(userGrid);
        
        refreshUsers();
    }

    private void openUserDialog(UserAccount userAccount) {
        UserDialog dialog = new UserDialog(
            userAccountRepository,
            locationService,
            institutionRepository,
            userAccount
        );
        dialog.setOnSaveCallback(this::refreshUsers);
        dialog.open();
    }

    private void toggleUserStatus(UserAccount userAccount) {
        if (userAccount == null || userAccount.getId() == null) {
            return;
        }
        
        try {
            userAccount.setEnabled(!userAccount.isEnabled());
            userAccountRepository.save(userAccount);
            refreshUsers();
            
            String message = userAccount.isEnabled() ? "Benutzer wurde aktiviert" : "Benutzer wurde deaktiviert";
            Notification.show(message, 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception e) {
            log.error("Error toggling user status: {}", e.getMessage(), e);
            Notification.show("Fehler beim Ändern des Status: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private String translateRole(String role) {
        return switch (role) {
            case AppRoles.ADMIN -> "Administrator";
            case AppRoles.OWNER -> "Praxisinhaber";
            case AppRoles.DOCTOR -> "Arzt";
            case AppRoles.MEDICAL_STAFF -> "Medizinisches Personal (MFA)";
            case AppRoles.TECH_USER -> "Technischer Benutzer";
            case AppRoles.USER -> "Benutzer";
            default -> role;
        };
    }

    private void refreshUsers() {
        // Ensure InstitutionContext is set before loading users
        ensureInstitutionContext();
        
        // Load users for current institution with preferredLocation eagerly fetched
        // This prevents LazyInitializationException when Grid renders the location column
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            allUsers = userAccountRepository.findAllByInstitutionIdWithPreferredLocation(institutionId);
            userGrid.setItems(allUsers);
        } else {
            userGrid.setItems(List.of());
        }
    }
    
    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set, especially for Institutionsadmins.
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, userAccount.getInstitution().getInstitutionCode());
                } else {
                    log.warn("UserAccount has no institution - cannot set InstitutionContext");
                }
            } catch (Exception e) {
                log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
            }
        } else {
            log.debug("Authentication type: {}, Principal type: {} - cannot set InstitutionContext",
                    authentication != null ? authentication.getClass().getSimpleName() : "null",
                    authentication != null && authentication.getPrincipal() != null 
                        ? authentication.getPrincipal().getClass().getSimpleName() : "null");
        }
    }
}

