package de.bbajor.pvs.base.ui.view;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationFilter;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
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
@Route
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
        
        addClassName(Padding.MEDIUM);
        add(new ViewToolbar("Dashboard"));
        add(createDashboardContent());
    }

    private Div createDashboardContent() {
        var content = new Div();
        content.addClassNames(Display.FLEX, FlexDirection.COLUMN, Gap.MEDIUM);
        
        var title = new H2("Verfügbare Bereiche");
        title.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD, Margin.NONE);
        content.add(title);
        
        var cardsContainer = createMenuCards();
        content.add(cardsContainer);
        
        return content;
    }

    private FlexLayout createMenuCards() {
        var container = new FlexLayout();
        container.addClassNames(
            Display.FLEX, 
            FlexWrap.WRAP, 
            Gap.MEDIUM,
            Width.FULL
        );
        container.setFlexDirection(FlexLayout.FlexDirection.ROW);
        container.setAlignItems(FlexComponent.Alignment.STRETCH);
        
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
            Padding.LARGE,
            BorderRadius.MEDIUM,
            Border.ALL,
            Background.BASE,
            Display.FLEX,
            FlexDirection.COLUMN,
            Gap.SMALL,
            AlignItems.CENTER,
            "dashboard-menu-card"
        );
        card.getStyle()
            .set("min-width", "200px")
            .set("flex", "1 1 250px")
            .set("max-width", "350px")
            .set("cursor", "pointer")
            .set("transition", "transform 0.2s, box-shadow 0.2s")
            .set("box-shadow", "var(--lumo-box-shadow-s)");
        
        card.addClickListener(e -> UI.getCurrent().navigate(path));
        
        if (iconName != null && !iconName.isEmpty()) {
            var icon = new Icon(iconName);
            icon.addClassNames(IconSize.LARGE, TextColor.PRIMARY);
            card.add(icon);
        }
        
        var titleElement = new H2(title);
        titleElement.addClassNames(FontSize.LARGE, FontWeight.SEMIBOLD, Margin.NONE);
        titleElement.getStyle().set("text-align", "center");
        card.add(titleElement);
        
        // Hover effect
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                .set("transform", "translateY(-2px)")
                .set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                .set("transform", "translateY(0)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");
        });
        
        return card;
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
