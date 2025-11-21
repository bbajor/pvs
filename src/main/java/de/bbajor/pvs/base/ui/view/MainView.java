package de.bbajor.pvs.base.ui.view;

import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import jakarta.annotation.security.PermitAll;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

/**
 * Dashboard view that displays all available menu entries as clickable cards.
 * This view shows up when a user navigates to the root ('/') of the application.
 */
@Route(value = "", layout = MainLayout.class)
@PermitAll // When security is enabled, allow all authenticated users
public final class MainView extends Main implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final HttpSession httpSession;

    MainView(CurrentUser currentUser, UserAccountRepository userAccountRepository, 
            HttpSession httpSession) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.httpSession = httpSession;
        
        // Padding ZUERST setzen, dann sizeFull() - wichtig für box-sizing: border-box
        getStyle().set("padding", "var(--lumo-space-l, 1.5rem)");
        getStyle().set("box-sizing", "border-box");
        setSizeFull();
        addClassNames("view-content", Display.FLEX, FlexDirection.COLUMN);
        
        // Keine Überschriften - nur Kacheln
        add(createMenuCards());
    }

    private Div createMenuCards() {
        var container = new Div();
        container.setWidthFull();
        container.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
            .set("gap", "var(--lumo-space-l, 1.5rem)")
            .set("margin-bottom", "0");
        container.addClassNames(Width.FULL);
        
        // Check if user is SUPER_ADMIN
        boolean isSuperAdmin = isCurrentUserSuperAdmin();
        
        if (isSuperAdmin) {
            // SUPER_ADMIN sees system settings and medication database
            container.add(createMenuCard("System-Einstellungen", "admin/super-settings", "vaadin:cog"));
            container.add(createMenuCard("Medikamentendatenbank", "ivom-drugs", "vaadin:pill"));
        } else {
            // Regular users see all menu entries
            MenuConfiguration.getMenuEntries().forEach(entry -> {
                // Filter out entries that should not be visible to regular users
                // (e.g., admin/institutions should not be in regular menu)
                if (!entry.path().equals("admin/institutions")) {
                    container.add(createMenuCard(entry));
                }
            });
        }
        
        return container;
    }

    private Div createMenuCard(MenuEntry menuEntry) {
        return createMenuCard(menuEntry.title(), menuEntry.path(), menuEntry.icon());
    }

    private Div createMenuCard(String title, String path, String iconName) {
        var card = new Div();
        card.addClassNames(
            Display.FLEX,
            FlexDirection.COLUMN,
            AlignItems.CENTER,
            JustifyContent.CENTER,
            "dashboard-menu-card"
        );
        
        // Größe wie im Analytics-Modul (min-height: 160px statt aspect-ratio)
        card.getStyle()
            .set("min-height", "160px")
            .set("padding", "var(--lumo-space-l, 1.5rem)")
            .set("border-radius", "var(--lumo-border-radius-l)")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("background", "var(--lumo-base-color)")
            .set("cursor", "pointer")
            .set("transition", "all 0.3s cubic-bezier(0.4, 0, 0.2, 1)")
            .set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.12)")
            .set("position", "relative")
            .set("overflow", "hidden");
        
        // Subtiler Hintergrund-Gradient (Vaadin Business App Style)
        card.getStyle().set("background", 
            "linear-gradient(135deg, var(--lumo-base-color) 0%, var(--lumo-contrast-5pct) 100%)");
        
        card.addClickListener(e -> UI.getCurrent().navigate(path));
        
        // Icon Container mit mehr Raum - OHNE Standard-Hintergrund (nur beim Hover)
        Div iconContainer = null;
        if (iconName != null && !iconName.isEmpty()) {
            iconContainer = new Div();
            iconContainer.addClassName("icon-container");
            iconContainer.getStyle()
                .set("width", "80px")
                .set("height", "80px")
                .set("margin-bottom", "var(--lumo-space-l)")
                .set("background", "transparent") // Kein Hintergrund standardmäßig
                .set("display", "flex")
                .set("justify-content", "center")
                .set("align-items", "center")
                .set("margin-left", "auto")
                .set("margin-right", "auto");
            
            var icon = new Icon(iconName);
            icon.setSize("48px");
            String color = getIconColor(path);
            icon.setColor(color);
            iconContainer.add(icon);
            card.add(iconContainer);
        }
        
        // Titel mit besserer Typografie
        var titleElement = new H2(title);
        titleElement.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD, Margin.NONE);
        titleElement.getStyle()
            .set("text-align", "center")
            .set("color", "var(--lumo-body-text-color)")
            .set("line-height", "1.4");
        card.add(titleElement);
        
        // Hover effect - Vaadin Business App Style (kombiniert für Card und Icon)
        final Div finalIconContainer = iconContainer;
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                .set("transform", "translateY(-4px) scale(1.02)")
                .set("box-shadow", "0 8px 24px rgba(0, 0, 0, 0.12), 0 4px 8px rgba(0, 0, 0, 0.08)")
                .set("border-color", "var(--lumo-primary-color-50pct)");
            if (finalIconContainer != null) {
                finalIconContainer.getStyle()
                    .set("background", "var(--lumo-primary-color-20pct)")
                    .set("transform", "scale(1.1)");
            }
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                .set("transform", "translateY(0) scale(1)")
                .set("box-shadow", "0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.12)")
                .set("border-color", "var(--lumo-contrast-20pct)");
            if (finalIconContainer != null) {
                finalIconContainer.getStyle()
                    .set("background", "transparent") // Zurück zu transparent, nicht blau
                    .set("transform", "scale(1)");
            }
        });
        
        return card;
    }
    
    private String getIconColor(String path) {
        // Farbige Icons für verschiedene Bereiche
        if (path.contains("ivom")) {
            return "var(--lumo-primary-color)";
        } else if (path.contains("patient")) {
            return "var(--lumo-success-color)";
        } else if (path.contains("surgicalcenter")) {
            return "var(--lumo-contrast-50pct)";
        } else if (path.contains("settings")) {
            return "var(--lumo-contrast-50pct)";
        } else if (path.contains("help")) {
            return "var(--lumo-primary-color)";
        }
        return "var(--lumo-primary-color)";
    }

    /**
     * Checks if the current user has SUPER_ADMIN role.
     */
    private boolean isCurrentUserSuperAdmin() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities() != null) {
                return auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
            }
        } catch (Exception e) {
            // If we can't determine the user's role, assume not super admin
        }
        return false;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Check if user is authenticated
        if (!currentUser.get().isPresent()) {
            return; // Let Spring Security handle unauthenticated access
        }

        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount == null) {
                return;
            }

            // Check if MFA is required
            if (userAccount.isMfaEnabled() && userAccount.getMfaSecret() != null) {
                if (MfaAuthenticationFilter.isMfaRequired(httpSession)) {
                    event.forwardTo("/mfa-verify");
                    return;
                }
            }

            // Check if password change is required
            if (userAccount.isPasswordChangeRequired()) {
                event.forwardTo("/password-change");
                return;
            }
        });
    }

    /**
     * Navigates to the main view.
     */
    public static void showMainView() {
        UI.getCurrent().navigate(MainView.class);
    }
}
