package de.bbajor.pvs.base.ui.view;

import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.AppRoles;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
public final class MainLayout extends AppLayout {

    private final CurrentUser currentUser;
    private final AuthenticationContext authenticationContext;

    MainLayout(CurrentUser currentUser, AuthenticationContext authenticationContext) {
        this.currentUser = currentUser;
        this.authenticationContext = authenticationContext;
        setPrimarySection(Section.DRAWER);
        addToDrawer(createHeader(), new Scroller(createSideNav()));
        // Only add user menu if user is authenticated (to avoid CurrentUser.require() exception)
        if (authenticationContext.isAuthenticated()) {
            addToDrawer(createUserMenu());
        }
    }

    private Div createHeader() {
        // TODO Replace with real application logo and name
        var appLogo = VaadinIcon.CALENDAR.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span("Praxis Tool-Suite");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }

    private SideNav createSideNav() {
        var nav = new SideNav();
        nav.addClassNames(Margin.Horizontal.MEDIUM);
        
        // Check if user is SUPER_ADMIN
        boolean isSuperAdmin = isCurrentUserSuperAdmin();
        
        if (isSuperAdmin) {
            // SUPER_ADMIN only sees their own settings menu
            nav.addItem(new SideNavItem("System-Einstellungen", "admin/super-settings", 
                    new Icon("vaadin:cog")));
        } else {
            // Regular users see all menu entries
            MenuConfiguration.getMenuEntries().forEach(entry -> {
                // Filter out entries that should not be visible to regular users
                // (e.g., admin/institutions should not be in regular menu)
                if (!entry.path().equals("admin/institutions")) {
                    nav.addItem(createSideNavItem(entry));
                }
            });
        }
        
        return nav;
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

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        if (menuEntry.icon() != null) {
            return new SideNavItem(menuEntry.title(), menuEntry.path(), new Icon(menuEntry.icon()));
        } else {
            return new SideNavItem(menuEntry.title(), menuEntry.path());
        }
    }

    private Component createUserMenu() {
        // Only call this if user is authenticated (checked in constructor)
        // Use get() with orElseThrow as fallback
        var user = currentUser.get().orElseThrow(() -> 
            new IllegalStateException("User menu should only be created for authenticated users"));

        var avatar = new Avatar(user.getFullName(), user.getPictureUrl());
        avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        avatar.addClassNames(Margin.Right.SMALL);
        avatar.setColorIndex(5);

        var userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassNames(Margin.MEDIUM);

        var userMenuItem = userMenu.addItem(avatar);
        userMenuItem.add(user.getFullName());
        if (user.getProfileUrl() != null) {
            userMenuItem.getSubMenu().addItem("Profil anzeigen",
                    event -> UI.getCurrent().getPage().open(user.getProfileUrl()));
        }
        // TODO Add additional items to the user menu if needed
        userMenuItem.getSubMenu().addItem("Ausloggen", event -> authenticationContext.logout());

        return userMenu;
    }

}
