package de.bbajor.pvs.security.dev;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.PostConstruct;

/**
 * Login view for development.
 */
@PageTitle("Login")
@AnonymousAllowed
// No @Route annotation - the route is registered dynamically by DevSecurityConfig.
class DevLoginView extends Main implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(DevLoginView.class);
    
    static final String LOGIN_PATH = "dev-login";
    private static final String CALLOUT_HIDDEN_KEY = "walking-skeleton-dev-login-callout-hidden";

    private final AuthenticationContext authenticationContext;
    private final UserAccountRepository userAccountRepository;
    private final org.springframework.beans.factory.ObjectProvider<AuthenticationManager> authenticationManagerProvider;
    private final TextField tenantCodeField;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;
    private Div exampleUsersDiv;

    DevLoginView(AuthenticationContext authenticationContext, UserAccountRepository userAccountRepository,
                 org.springframework.beans.factory.ObjectProvider<AuthenticationManager> authenticationManagerProvider) {
        this.authenticationContext = authenticationContext;
        this.userAccountRepository = userAccountRepository;
        // Use ObjectProvider to avoid eager initialization issues
        this.authenticationManagerProvider = authenticationManagerProvider;

        // Create custom login form with institution/tenant code field
        // During migration, supports both Institution Code and Tenant Code (legacy)
        tenantCodeField = new TextField("Institution/Tenant Code");
        tenantCodeField.setPlaceholder("z.B. DEV-TEST oder PRAX-001");
        tenantCodeField.setRequired(true);
        tenantCodeField.setWidthFull();

        usernameField = new TextField("Benutzername");
        usernameField.setRequired(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField("Passwort");
        passwordField.setRequired(true);
        passwordField.setWidthFull();
        passwordField.setRevealButtonVisible(true); // Show/Hide password button with eye icon

        loginButton = new Button("Anmelden");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(event -> performLogin());

        // Create login form with title
        var loginTitle = new H2("Anmeldung");
        loginTitle.addClassNames("dev-login-title");
        
        var loginForm = new FormLayout();
        loginForm.add(tenantCodeField, usernameField, passwordField, loginButton);
        loginForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        loginForm.getStyle().set("max-width", "420px");
        loginForm.getStyle().set("width", "100%");

        // Create decorative medical icon/image
        var medicalIcon = createMedicalIcon();
        medicalIcon.addClassNames("dev-login-medical-icon");
        
        var loginFormContainer = new Div(loginTitle, loginForm);
        loginFormContainer.addClassNames("dev-login-form-container");
        
        // Create wrapper with icon and form side by side
        var loginSection = new Div(medicalIcon, loginFormContainer);
        loginSection.addClassNames("dev-login-section");

        var exampleUsersHeader = new Div();
        var headerIcon = VaadinIcon.USERS.create();
        headerIcon.setSize("16px");
        var headerText = new Span("Testbenutzer");
        exampleUsersHeader.add(headerIcon, headerText);
        exampleUsersHeader.addClassNames("dev-users-header");
        
        exampleUsersDiv = new Div();
        exampleUsersDiv.addClassNames("dev-users");

        // Configure the view
        setSizeFull();
        addClassNames("dev-login-view");
        getStyle().set("display", "flex");
        getStyle().set("justify-content", "center");
        getStyle().set("align-items", "center");
        getStyle().set("min-height", "100vh");

        // Wrap user list in scrollable container
        var usersScrollContainer = new Div(exampleUsersHeader, exampleUsersDiv);
        usersScrollContainer.addClassNames("dev-users-scroll-container");
        usersScrollContainer.getStyle().set("display", "flex");
        usersScrollContainer.getStyle().set("flex-direction", "column");
        usersScrollContainer.getStyle().set("width", "500px");
        usersScrollContainer.getStyle().set("min-width", "500px");
        usersScrollContainer.getStyle().set("flex-shrink", "0");

        // Create fixed login form wrapper
        var loginFormWrapper = new Div(loginSection);
        loginFormWrapper.addClassNames("dev-login-form-wrapper");
        loginFormWrapper.getStyle().set("display", "flex");
        loginFormWrapper.getStyle().set("align-items", "center");
        loginFormWrapper.getStyle().set("justify-content", "center");
        loginFormWrapper.getStyle().set("min-width", "650px");
        loginFormWrapper.getStyle().set("flex-shrink", "0");

        var contentDiv = new Div(loginFormWrapper, usersScrollContainer);
        contentDiv.addClassNames("dev-content-div");
        contentDiv.getStyle().set("display", "flex");
        contentDiv.getStyle().set("flex-direction", "row");
        contentDiv.getStyle().set("max-width", "1200px");
        contentDiv.getStyle().set("max-height", "90vh");
        contentDiv.getStyle().set("width", "100%");
        add(contentDiv);

        var devModeMenuDiv = new Div("You can also use the Dev Mode Menu here to impersonate any user");
        devModeMenuDiv.addClassNames("dev-mode-speech-bubble");
        // Hide the callout when clicked
        devModeMenuDiv.addClickListener(event -> {
            WebStorage.setItem(WebStorage.Storage.LOCAL_STORAGE, CALLOUT_HIDDEN_KEY, "1");
            devModeMenuDiv.setVisible(false);
        });
        devModeMenuDiv.setVisible(false);
        add(devModeMenuDiv);

        // Don't show the callout if already hidden once
        WebStorage.getItem(WebStorage.Storage.LOCAL_STORAGE, CALLOUT_HIDDEN_KEY,
                value -> devModeMenuDiv.setVisible(value == null));
    }

    @PostConstruct
    private void loadDatabaseUsers() {
        // Load relevant users for dev login: Superadmin + one user per institution
        try {
            List<UserAccount> allUsers = userAccountRepository.findAll();
            
            // Filter: Superadmin + one user per institution
            List<UserAccount> usersToShow = new ArrayList<>();
            
            // Add superadmin (if exists)
            allUsers.stream()
                    .filter(user -> user.getRoles() != null && user.getRoles().contains(AppRoles.SUPER_ADMIN))
                    .findFirst()
                    .ifPresent(usersToShow::add);
            
            // Group users by institution and add one user per institution
            Map<Long, UserAccount> usersByInstitution = new HashMap<>();
            for (UserAccount user : allUsers) {
                if (user.getInstitution() != null) {
                    Long institutionId = user.getInstitution().getId();
                    // Prefer ADMIN user, otherwise take first user for this institution
                    if (!usersByInstitution.containsKey(institutionId)) {
                        usersByInstitution.put(institutionId, user);
                    } else {
                        // Replace with ADMIN user if current is not ADMIN
                        UserAccount current = usersByInstitution.get(institutionId);
                        if (user.getRoles() != null && user.getRoles().contains(AppRoles.ADMIN) 
                                && (current.getRoles() == null || !current.getRoles().contains(AppRoles.ADMIN))) {
                            usersByInstitution.put(institutionId, user);
                        }
                    }
                }
            }
            usersToShow.addAll(usersByInstitution.values());
            
            // Add all users to the view
            usersToShow.forEach(user -> exampleUsersDiv.add(createUserAccountCard(user)));
        } catch (Exception e) {
            log.warn("Could not load users from database: {}", e.getMessage());
            // Continue without database users - login will still work
        }
    }

    private void performLogin() {
        String tenantCode = tenantCodeField.getValue();
        String username = usernameField.getValue();
        String password = passwordField.getValue();

        // Validate required fields
        if (username.isEmpty() || password.isEmpty()) {
            Notification.show("Bitte geben Sie Benutzername und Passwort ein", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        // Check if institution code is empty and if user is SUPER_ADMIN or INSTITUTION_ADMIN
        boolean isEmptyInstitutionCode = tenantCode == null || tenantCode.trim().isEmpty();
        if (isEmptyInstitutionCode) {
            // Try to load user from database to check roles
            Optional<UserAccount> userOpt = userAccountRepository.findByUsername(username);
            if (userOpt.isPresent()) {
                UserAccount user = userOpt.get();
                boolean hasSuperAdminRole = user.getRoles() != null && 
                        (user.getRoles().contains(AppRoles.SUPER_ADMIN) || user.getRoles().contains(AppRoles.INSTITUTION_ADMIN));
                
                if (!hasSuperAdminRole) {
                    Notification.show("Institution-Code ist erforderlich. Nur SUPER_ADMIN und INSTITUTION_ADMIN können ohne Institution-Code einloggen.", 
                            5000, Notification.Position.TOP_CENTER);
                    tenantCodeField.setInvalid(true);
                    return;
                }
                // User has SUPER_ADMIN or INSTITUTION_ADMIN role, allow login without institution code
                log.debug("User {} has {} role, allowing login without institution code", username,
                        user.getRoles().contains(AppRoles.SUPER_ADMIN) ? AppRoles.SUPER_ADMIN : AppRoles.INSTITUTION_ADMIN);
            } else {
                // User not found yet, but we'll let authentication provider handle it
                // It will check if user has SUPER_ADMIN or INSTITUTION_ADMIN role
                log.debug("User {} not found in database yet, letting authentication provider handle validation", username);
            }
        }

        try {
            // Get AuthenticationManager lazily to avoid initialization issues
            AuthenticationManager authManager = authenticationManagerProvider.getObject();
            
            // Authenticate directly in Vaadin thread - this preserves SecurityContext
            // Use empty string if institution code is empty (for SUPER_ADMIN/INSTITUTION_ADMIN)
            String institutionCodeForAuth = isEmptyInstitutionCode ? "" : tenantCode;
            InstitutionAuthenticationToken authRequest = new InstitutionAuthenticationToken(institutionCodeForAuth, username, password);
            Authentication authResult = authManager.authenticate(authRequest);
            
            // Set authentication in SecurityContext
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authResult);
            SecurityContextHolder.setContext(securityContext);
            
            // CRITICAL: Save SecurityContext to HTTP Session
            // Vaadin navigations are client-side and don't trigger HTTP filters,
            // so we must explicitly save the SecurityContext to the session
            VaadinServletRequest vaadinRequest = VaadinServletRequest.getCurrent();
            VaadinServletResponse vaadinResponse = VaadinServletResponse.getCurrent();
            if (vaadinRequest != null && vaadinResponse != null) {
                HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
                securityContextRepository.saveContext(securityContext, vaadinRequest, vaadinResponse);
                log.debug("SecurityContext saved to HTTP session");
            } else {
                log.warn("Could not get VaadinServletRequest/VaadinServletResponse - SecurityContext may not be persisted to session");
            }
            
            // Set InstitutionContext from authentication token - required for institution-aware queries
            if (authResult instanceof InstitutionAuthenticationToken institutionAuth && institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext set to: {} (institution code: {})", institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
            
            log.debug("Login successful for user: {} (institution/tenant: {})", username, tenantCode);
            
            // Navigate directly using Vaadin's router - SecurityContext is now available
            // SUPER_ADMIN and INSTITUTION_ADMIN without institution should go to institution management
            if (authResult instanceof InstitutionAuthenticationToken institutionAuth && institutionAuth.getInstitutionId() == null) {
                // User logged in without institution (SUPER_ADMIN or INSTITUTION_ADMIN)
                UI.getCurrent().navigate("admin/institutions");
            } else {
                // Regular user with institution - go to patient search
                UI.getCurrent().navigate("patient-search");
            }
            
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {} (institution/tenant: {})", username, tenantCode);
            Notification notification = Notification.show(
                    "Login fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.", 
                    5000, 
                    Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Mark fields as invalid
            tenantCodeField.setInvalid(true);
            usernameField.setInvalid(true);
            passwordField.setInvalid(true);
            passwordField.setErrorMessage("Ungültige Anmeldedaten.");
        } catch (Exception e) {
            log.error("Error during login", e);
            Notification notification = Notification.show(
                    "Ein Fehler ist aufgetreten: " + e.getMessage(), 
                    5000, 
                    Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private Component createUserAccountCard(UserAccount userAccount) {
        var card = new Div();
        card.addClassNames("dev-user-card");

        String displayName = userAccount.getFullName() != null && !userAccount.getFullName().isEmpty()
                ? userAccount.getFullName()
                : userAccount.getUsername();
        
        // User icon and name
        var userIcon = VaadinIcon.USER.create();
        userIcon.setSize("20px");
        userIcon.getStyle().set("color", "var(--lumo-primary-color)");
        var fullName = new H3(displayName);
        fullName.getStyle().set("margin", "0");
        fullName.getStyle().set("font-size", "var(--lumo-font-size-l)");
        
        var nameContainer = new Div(userIcon, fullName);
        nameContainer.addClassNames("dev-user-name-container");

        // Credentials
        var credentials = new DescriptionList();
        String institutionCodeDisplay = userAccount.getInstitution() != null 
                ? userAccount.getInstitution().getInstitutionCode() 
                : (userAccount.getPreferredLocation() != null 
                    ? userAccount.getPreferredLocation().getLocationName() 
                    : "Unbekannt");
        String locationCodeDisplay = userAccount.getPreferredLocation() != null 
                ? userAccount.getPreferredLocation().getLocationName() 
                : "Unbekannt";
        
        var institutionIcon = VaadinIcon.BUILDING.create();
        institutionIcon.setSize("14px");
        credentials.add(new DescriptionList.Term(new Span(institutionIcon, new Span(" Institution"))), 
                new DescriptionList.Description(institutionCodeDisplay));
        
        var locationIcon = VaadinIcon.MAP_MARKER.create();
        locationIcon.setSize("14px");
        credentials.add(new DescriptionList.Term(new Span(locationIcon, new Span(" Standort"))), 
                new DescriptionList.Description(locationCodeDisplay));
        
        var usernameIcon = VaadinIcon.USER_CARD.create();
        usernameIcon.setSize("14px");
        credentials.add(new DescriptionList.Term(new Span(usernameIcon, new Span(" Benutzername"))), 
                new DescriptionList.Description(userAccount.getUsername()));

        // Try to extract password from hash (for login button only)
        final String passwordHint;
        if (userAccount.getPasswordHash() != null && userAccount.getPasswordHash().startsWith("{noop}")) {
            passwordHint = userAccount.getPasswordHash().substring("{noop}".length());
        } else {
            passwordHint = "123";
        }

        final String finalPassword = passwordHint;
        var quickLoginButton = new Button("Anmelden", VaadinIcon.SIGN_IN.create(), event -> {
            // CRITICAL: Use institution code, not location name
            // If institution is null, use "SUPER_ADMIN" or empty string (user must have SUPER_ADMIN role)
            String institutionCodeToUse = institutionCodeDisplay;
            if ("Unbekannt".equals(institutionCodeToUse) || institutionCodeToUse == null || institutionCodeToUse.isEmpty()) {
                // For users without institution, check if they have SUPER_ADMIN role
                if (userAccount.getRoles() != null && userAccount.getRoles().contains("SUPER_ADMIN")) {
                    institutionCodeToUse = ""; // Empty for SUPER_ADMIN
                } else {
                    // For other users without institution, show error
                    Notification.show("User hat keine Institution zugeordnet. Bitte Institution-Code manuell eingeben.", 
                        5000, Notification.Position.TOP_CENTER);
                    return;
                }
            }
            tenantCodeField.setValue(institutionCodeToUse);
            usernameField.setValue(userAccount.getUsername());
            passwordField.setValue(finalPassword);
            performLogin();
        });
        quickLoginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        quickLoginButton.setWidthFull();

        var cardContent = new Div(nameContainer, credentials);
        cardContent.addClassNames("dev-user-card-content");
        var cardActions = new Div(quickLoginButton);
        cardActions.addClassNames("dev-user-card-actions");
        
        card.add(cardContent, cardActions);

        return card;
    }

    private Component createMedicalIcon() {
        // Create a large decorative medical icon using SVG
        var iconContainer = new Div();
        iconContainer.addClassNames("dev-medical-icon-container");
        
        // Use healthicons SVG for medical/healthcare theme
        try {
            var healthIcon = new Icon("my-icons-icons", "healthicons--ambulatory-clinic");
            healthIcon.setSize("200px");
            healthIcon.getStyle().set("color", "var(--lumo-primary-color)");
            healthIcon.getStyle().set("opacity", "0.2");
            healthIcon.addClassNames("dev-login-medical-icon");
            iconContainer.add(healthIcon);
        } catch (Exception e) {
            // Fallback to VaadinIcon with medical theme
            var fallbackIcon = VaadinIcon.HEART.create();
            fallbackIcon.setSize("200px");
            fallbackIcon.getStyle().set("color", "var(--lumo-primary-color)");
            fallbackIcon.getStyle().set("opacity", "0.2");
            fallbackIcon.addClassNames("dev-login-medical-icon");
            iconContainer.add(fallbackIcon);
        }
        
        return iconContainer;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean authenticated = authenticationContext.isAuthenticated();
        String location = event.getLocation().getPath();
        log.debug("DevLoginView.beforeEnter() called - location: {}, authenticated: {}", location, authenticated);
        
        if (authenticated) {
            // Redirect to patient search if the user is already logged in
            log.debug("User is authenticated in DevLoginView.beforeEnter(), redirecting to patient-search");
            event.forwardTo("patient-search");
            return;
        }

        log.debug("User is not authenticated in DevLoginView.beforeEnter()");

        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            // Show error notification for login failure
            var errorParams = event.getLocation().getQueryParameters().getParameters().get("error");
            String errorMessage = "Login fehlgeschlagen. Bitte überprüfen Sie Ihre Eingaben.";
            
            if (errorParams != null && !errorParams.isEmpty()) {
                String errorParam = errorParams.get(0);
                if (errorParam != null && !errorParam.isEmpty()) {
                    errorMessage = "Login fehlgeschlagen: " + errorParam;
                }
            }
            
            // Show notification
            Notification notification = Notification.show(errorMessage, 5000, Notification.Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            
            // Mark fields as invalid
            tenantCodeField.setInvalid(true);
            usernameField.setInvalid(true);
            passwordField.setInvalid(true);
            passwordField.setErrorMessage("Bitte überprüfen Sie Ihre Eingaben.");
        }
    }
}
