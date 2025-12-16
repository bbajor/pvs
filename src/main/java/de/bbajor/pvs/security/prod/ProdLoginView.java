package de.bbajor.pvs.security.prod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.MfaAuthenticationProvider;
import org.springframework.core.env.Environment;

import java.util.Optional;

/**
 * Production login view with centered credentials input.
 * Clean, professional design without development conveniences.
 */
@PageTitle("Anmeldung")
@AnonymousAllowed
// No @Route annotation - the route is registered dynamically by ProdSecurityConfig.
class ProdLoginView extends Main implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(ProdLoginView.class);
    
    static final String LOGIN_PATH = "login";

    private final AuthenticationContext authenticationContext;
    private final UserAccountRepository userAccountRepository;
    private final AuthenticationManager authenticationManager;
    private final InstitutionRepository institutionRepository;
    private final Environment environment;
    private final TextField tenantCodeField;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final TextField mfaCodeField;
    private final Button loginButton;

    ProdLoginView(AuthenticationContext authenticationContext,
                  UserAccountRepository userAccountRepository,
                  AuthenticationManager authenticationManager,
                  InstitutionRepository institutionRepository,
                  Environment environment) {
        this.authenticationContext = authenticationContext;
        this.userAccountRepository = userAccountRepository;
        this.authenticationManager = authenticationManager;
        this.institutionRepository = institutionRepository;
        this.environment = environment;
        
        // Create login form fields
        tenantCodeField = new TextField("Institution/Tenant Code");
        tenantCodeField.setPlaceholder("z.B. PRAX-001");
        tenantCodeField.setRequired(true);
        tenantCodeField.setWidthFull();
        
        // Check if onpremise profile is active and institution code is configured
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isOnPremise = java.util.Arrays.asList(activeProfiles).contains("onpremise");
        String onPremiseInstitutionCode = environment.getProperty("app.onpremise.institution-code", "");
        
        if (isOnPremise && onPremiseInstitutionCode != null && !onPremiseInstitutionCode.trim().isEmpty()) {
            // OnPremise: Set institution code automatically and hide field
            tenantCodeField.setValue(onPremiseInstitutionCode);
            tenantCodeField.setVisible(false);
            tenantCodeField.setRequired(false);
            log.info("OnPremise mode: Institution code automatically set to: {}", onPremiseInstitutionCode);
        }

        usernameField = new TextField("Benutzername oder E-Mail");
        usernameField.setRequired(true);
        usernameField.setWidthFull();
        usernameField.setHelperText("Sie können sich mit Ihrem Benutzernamen oder Ihrer E-Mail-Adresse anmelden");

        passwordField = new PasswordField("Passwort");
        passwordField.setRequired(true);
        passwordField.setWidthFull();
        passwordField.setRevealButtonVisible(true);

        mfaCodeField = new TextField("MFA-Code");
        mfaCodeField.setPlaceholder("000000");
        mfaCodeField.setMaxLength(6);
        mfaCodeField.setPattern("[0-9]{6}");
        mfaCodeField.setHelperText("6-stelliger Code aus Ihrer Authenticator-App");
        mfaCodeField.setWidthFull();
        mfaCodeField.setVisible(false);

        loginButton = new Button("Anmelden");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(event -> performLogin());
        
        // Enter key support
        passwordField.addKeyPressListener(e -> {
            if (e.getKey().equals("Enter")) {
                performLogin();
            }
        });
        mfaCodeField.addKeyPressListener(e -> {
            if (e.getKey().equals("Enter")) {
                performLogin();
            }
        });

        // Create login form with title
        H2 loginTitle = new H2("Anmeldung");
        loginTitle.getStyle().set("text-align", "center");
        loginTitle.getStyle().set("margin-bottom", "var(--lumo-space-l)");
        
        var loginForm = new FormLayout();
        loginForm.add(tenantCodeField, usernameField, passwordField, mfaCodeField, loginButton);
        loginForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        loginForm.getStyle().set("max-width", "420px");
        loginForm.getStyle().set("width", "100%");
        
        var loginFormContainer = new Div(loginTitle, loginForm);
        loginFormContainer.getStyle().set("padding", "var(--lumo-space-xl)");
        loginFormContainer.getStyle().set("background", "var(--lumo-base-color)");
        loginFormContainer.getStyle().set("border-radius", "var(--lumo-border-radius-l)");
        loginFormContainer.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        loginFormContainer.getStyle().set("max-width", "420px");
        loginFormContainer.getStyle().set("width", "100%");

        // Configure the view - centered layout
        setSizeFull();
        addClassNames("prod-login-view");
        getStyle().set("display", "flex");
        getStyle().set("justify-content", "center");
        getStyle().set("align-items", "center");
        getStyle().set("min-height", "100vh");
        getStyle().set("background", "var(--lumo-contrast-5pct)");
        
        add(loginFormContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            log.debug("User is authenticated in ProdLoginView.beforeEnter(), redirecting to dashboard");
            event.forwardTo("/");
        }
        log.debug("User is not authenticated in ProdLoginView.beforeEnter()");
    }

    private void performLogin() {
        String tenantCode = tenantCodeField.getValue();
        String username = usernameField.getValue();
        String password = passwordField.getValue();
        String mfaCode = mfaCodeField.getValue();

        // Validate required fields
        if (username.isEmpty() || password.isEmpty()) {
            Notification.show("Bitte geben Sie Benutzername und Passwort ein", 3000, Notification.Position.TOP_CENTER);
            return;
        }

        // Check if institution code is empty and if user is SUPER_ADMIN
        boolean isEmptyInstitutionCode = tenantCode == null || tenantCode.trim().isEmpty();
        if (isEmptyInstitutionCode) {
            // Try to load user from database to check roles
            try {
                var userAccount = userAccountRepository.findByUsernameOrEmail(username).orElse(null);
                if (userAccount != null && userAccount.getRoles() != null && 
                    userAccount.getRoles().contains("SUPER_ADMIN")) {
                    tenantCode = ""; // Empty for SUPER_ADMIN
                } else {
                    Notification.show("Bitte geben Sie einen Institution/Tenant Code ein", 3000, Notification.Position.TOP_CENTER);
                    return;
                }
            } catch (Exception e) {
                log.warn("Could not check user roles: {}", e.getMessage());
                Notification.show("Bitte geben Sie einen Institution/Tenant Code ein", 3000, Notification.Position.TOP_CENTER);
                return;
            }
        }

        try {
            // Create authentication token with institution context
            // Constructor: (String institutionCode, Object principal, Object credentials)
            InstitutionAuthenticationToken authToken = new InstitutionAuthenticationToken(
                    tenantCode, username, password);
            
            // Set MFA code in details if provided
            if (mfaCode != null && !mfaCode.trim().isEmpty()) {
                authToken.setDetails(new MfaAuthenticationProvider.MfaAuthenticationDetails(mfaCode));
            }
            
            // Authenticate
            Authentication authentication = authenticationManager.authenticate(authToken);
            
            // Set security context
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            
            // Store in session
            VaadinServletRequest request = VaadinServletRequest.getCurrent();
            if (request != null) {
                request.getSession().setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
            }
            
            // Set institution context from authentication result
            if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
                if (institutionAuth.getInstitutionId() != null) {
                    InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                } else if (tenantCode != null && !tenantCode.trim().isEmpty()) {
                    // Fallback: Try to find institution by code and set ID
                    Optional<Institution> institutionOpt = institutionRepository.findByInstitutionCode(tenantCode);
                    if (institutionOpt.isPresent()) {
                        InstitutionContext.setInstitutionId(institutionOpt.get().getId());
                    }
                }
            }
            
            log.info("User {} successfully authenticated with institution code: {}", username, tenantCode);
            
            // Redirect to dashboard
            UI.getCurrent().navigate("/");
            
        } catch (BadCredentialsException e) {
            log.warn("Authentication failed for user: {}", username);
            Notification.show("Anmeldung fehlgeschlagen. Bitte überprüfen Sie Ihre Zugangsdaten.", 
                    5000, Notification.Position.TOP_CENTER);
        } catch (Exception e) {
            log.error("Unexpected error during login", e);
            Notification.show("Ein Fehler ist aufgetreten. Bitte versuchen Sie es erneut.", 
                    5000, Notification.Position.TOP_CENTER);
        }
    }
}

