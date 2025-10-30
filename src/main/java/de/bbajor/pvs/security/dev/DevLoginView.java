package de.bbajor.pvs.security.dev;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.page.WebStorage;
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
    private final LoginForm login;
    private final UserAccountRepository userAccountRepository;

    DevLoginView(AuthenticationContext authenticationContext, UserAccountRepository userAccountRepository) {
        this.authenticationContext = authenticationContext;
        this.userAccountRepository = userAccountRepository;

        // Create the components
        login = new LoginForm();
        login.setAction(LOGIN_PATH);
        login.setForgotPasswordButtonVisible(false);

        var exampleUsers = new Div(new Div("Use the following details to login"));
        
        // Add predefined test users
        SampleUsers.ALL_USERS.forEach(user -> exampleUsers.add(createSampleUserCard(user)));
        
        // Add users from database
        List<UserAccount> dbUsers = userAccountRepository.findAll();
        dbUsers.forEach(user -> exampleUsers.add(createUserAccountCard(user)));

        // Configure the view
        setSizeFull();
        addClassNames("dev-login-view");

        exampleUsers.addClassNames("dev-users");

        var contentDiv = new Div(login, exampleUsers);
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

    private Component createSampleUserCard(DevUser user) {
        var card = new Div();
        card.addClassNames("dev-user-card");

        var fullName = new H3(user.getAppUser().getFullName());

        var credentials = new DescriptionList();
        credentials.add(new DescriptionList.Term("Username"), new DescriptionList.Description(user.getUsername()));
        credentials.add(new DescriptionList.Term("Password"),
                new DescriptionList.Description("•••")); // Passwort nicht im Klartext anzeigen

        // Make it easier to log in while still going through the normal authentication process.
        var loginButton = new Button(VaadinIcon.SIGN_IN.create(), event -> {
            login.getElement().executeJs("""
                    document.getElementById("vaadinLoginUsername").value = $0;
                    document.getElementById("vaadinLoginPassword").value = $1;
                    document.forms[0].submit();
                    """, user.getUsername(), SampleUsers.SAMPLE_PASSWORD);
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        loginButton.setTooltipText("Passwort: " + SampleUsers.SAMPLE_PASSWORD);

        card.add(new Div(fullName, credentials));
        card.add(loginButton);

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
        credentials.add(new DescriptionList.Term("Username"), new DescriptionList.Description(userAccount.getUsername()));
        credentials.add(new DescriptionList.Term("Password"), new DescriptionList.Description("•••")); // Passwort nicht im Klartext anzeigen

        // Try to extract password from hash (for login button only)
        final String passwordHint;
        if (userAccount.getPasswordHash() != null && userAccount.getPasswordHash().startsWith("{noop}")) {
            passwordHint = userAccount.getPasswordHash().substring("{noop}".length());
        } else {
            passwordHint = "123"; // Standard-Passwort für verschlüsselte Passwörter
        }

        // Make it easier to log in while still going through the normal authentication process.
        final String finalPassword = passwordHint;
        var loginButton = new Button(VaadinIcon.SIGN_IN.create(), event -> {
            login.getElement().executeJs("""
                    document.getElementById("vaadinLoginUsername").value = $0;
                    document.getElementById("vaadinLoginPassword").value = $1;
                    document.forms[0].submit();
                    """, userAccount.getUsername(), finalPassword);
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
        
        // Passwort im Tooltip anzeigen (wird beim Hovern sichtbar)
        if (userAccount.getPasswordHash() != null && userAccount.getPasswordHash().startsWith("{noop}")) {
            loginButton.setTooltipText("Passwort: " + finalPassword);
        } else {
            loginButton.setTooltipText("Passwort: 123 (Standard für verschlüsselte Passwörter)");
        }

        card.add(new Div(fullName, credentials));
        card.add(loginButton);

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
            login.setError(true);
        }
    }
}
