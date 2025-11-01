package de.bbajor.pvs.security.dev;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;

import java.util.List;

/**
 * Login view for development.
 */
@PageTitle("Login")
@AnonymousAllowed
// No @Route annotation - the route is registered dynamically by DevSecurityConfig.
class DevLoginView extends Main implements BeforeEnterObserver {

    static final String LOGIN_PATH = "dev-login";
    private static final String CALLOUT_HIDDEN_KEY = "walking-skeleton-dev-login-callout-hidden";

    private final AuthenticationContext authenticationContext;
    private final UserAccountRepository userAccountRepository;
    private final TextField tenantCodeField;
    private final TextField usernameField;
    private final PasswordField passwordField;
    private final Button loginButton;

    DevLoginView(AuthenticationContext authenticationContext, UserAccountRepository userAccountRepository) {
        this.authenticationContext = authenticationContext;
        this.userAccountRepository = userAccountRepository;

        // Create custom login form with tenant code field
        tenantCodeField = new TextField("Tenant Code");
        tenantCodeField.setPlaceholder("z.B. PRAX-A1B2C3D4");
        tenantCodeField.setRequired(true);
        tenantCodeField.setWidthFull();

        usernameField = new TextField("Benutzername");
        usernameField.setRequired(true);
        usernameField.setWidthFull();

        passwordField = new PasswordField("Passwort");
        passwordField.setRequired(true);
        passwordField.setWidthFull();

        loginButton = new Button("Anmelden");
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();
        loginButton.addClickListener(event -> performLogin());

        var loginForm = new FormLayout();
        loginForm.add(tenantCodeField, usernameField, passwordField, loginButton);
        loginForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1)
        );
        loginForm.getStyle().set("max-width", "400px");

        var exampleUsers = new Div(new Div("Dev-Benutzer für Tests (klick auf Button zum Login)"));
        
        // Add predefined test users
        SampleUsers.ALL_USERS.forEach(user -> exampleUsers.add(createSampleUserCard(user)));
        
        // Add users from database
        List<UserAccount> dbUsers = userAccountRepository.findAll();
        dbUsers.forEach(user -> exampleUsers.add(createUserAccountCard(user)));

        // Configure the view
        setSizeFull();
        addClassNames("dev-login-view");

        exampleUsers.addClassNames("dev-users");

        var contentDiv = new Div(loginForm, exampleUsers);
        contentDiv.addClassNames("dev-content-div");
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

    private void performLogin() {
        String tenantCode = tenantCodeField.getValue();
        String username = usernameField.getValue();
        String password = passwordField.getValue();

        if (tenantCode.isEmpty() || username.isEmpty() || password.isEmpty()) {
            return;
        }

        // Redirect to login endpoint with tenant code
        getUI().ifPresent(ui -> {
            ui.getPage().executeJs("""
                    const form = document.createElement('form');
                    form.method = 'POST';
                    form.action = $0;
                    
                    const tenantInput = document.createElement('input');
                    tenantInput.type = 'hidden';
                    tenantInput.name = 'tenantCode';
                    tenantInput.value = $1;
                    form.appendChild(tenantInput);
                    
                    const usernameInput = document.createElement('input');
                    usernameInput.type = 'hidden';
                    usernameInput.name = 'username';
                    usernameInput.value = $2;
                    form.appendChild(usernameInput);
                    
                    const passwordInput = document.createElement('input');
                    passwordInput.type = 'hidden';
                    passwordInput.name = 'password';
                    passwordInput.value = $3;
                    form.appendChild(passwordInput);
                    
                    document.body.appendChild(form);
                    form.submit();
                    """, LOGIN_PATH, tenantCode, username, password);
        });
    }

    private Component createSampleUserCard(DevUser user) {
        var card = new Div();
        card.addClassNames("dev-user-card");

        var fullName = new H3(user.getAppUser().getFullName());

        var credentials = new DescriptionList();
        credentials.add(new DescriptionList.Term("Tenant"), new DescriptionList.Description("DEV-TEST"));
        credentials.add(new DescriptionList.Term("Username"), new DescriptionList.Description(user.getUsername()));
        credentials.add(new DescriptionList.Term("Password"),
                new DescriptionList.Description("•••"));

        var quickLoginButton = new Button(VaadinIcon.SIGN_IN.create(), event -> {
            tenantCodeField.setValue("DEV-TEST");
            usernameField.setValue(user.getUsername());
            passwordField.setValue(SampleUsers.SAMPLE_PASSWORD);
            performLogin();
        });
        quickLoginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        quickLoginButton.setTooltipText("Schnell-Login");

        card.add(new Div(fullName, credentials));
        card.add(quickLoginButton);

        return card;
    }

    private Component createUserAccountCard(UserAccount userAccount) {
        var card = new Div();
        card.addClassNames("dev-user-card");

        String displayName = userAccount.getFullName() != null && !userAccount.getFullName().isEmpty()
                ? userAccount.getFullName()
                : userAccount.getUsername();
        var fullName = new H3(displayName);

        var credentials = new DescriptionList();
        String tenantCodeDisplay = userAccount.getTenant() != null 
                ? userAccount.getTenant().getTenantCode() 
                : "DEV-TEST";
        credentials.add(new DescriptionList.Term("Tenant"), new DescriptionList.Description(tenantCodeDisplay));
        credentials.add(new DescriptionList.Term("Username"), new DescriptionList.Description(userAccount.getUsername()));
        credentials.add(new DescriptionList.Term("Password"), new DescriptionList.Description("•••"));

        // Try to extract password from hash (for login button only)
        final String passwordHint;
        if (userAccount.getPasswordHash() != null && userAccount.getPasswordHash().startsWith("{noop}")) {
            passwordHint = userAccount.getPasswordHash().substring("{noop}".length());
        } else {
            passwordHint = "123";
        }

        final String finalPassword = passwordHint;
        var quickLoginButton = new Button(VaadinIcon.SIGN_IN.create(), event -> {
            tenantCodeField.setValue(tenantCodeDisplay);
            usernameField.setValue(userAccount.getUsername());
            passwordField.setValue(finalPassword);
            performLogin();
        });
        quickLoginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        quickLoginButton.setTooltipText("Schnell-Login");

        card.add(new Div(fullName, credentials));
        card.add(quickLoginButton);

        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            // Redirect to the main view if the user is already logged in. This makes impersonation easier to work with.
            event.forwardTo("");
            return;
        }

        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            // Show error notification for login failure
            tenantCodeField.setInvalid(true);
            usernameField.setInvalid(true);
            passwordField.setInvalid(true);
            passwordField.setErrorMessage("Login fehlgeschlagen. Bitte \u00fcberpr\u00fcfen Sie Ihre Eingaben.");
        }
    }
}
