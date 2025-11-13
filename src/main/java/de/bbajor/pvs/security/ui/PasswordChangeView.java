package de.bbajor.pvs.security.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * View for forced password change.
 * 
 * <p>
 * This view is displayed when a user is required to change their password (e.g., after first login
 * with an initial password). The user cannot proceed until they change their password.
 * </p>
 */
@Route("password-change")
@PageTitle("Passwort ändern")
@PermitAll
public class PasswordChangeView extends VerticalLayout implements BeforeEnterObserver {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationContext authenticationContext;

    private final H2 title = new H2("Passwort ändern erforderlich");
    private final Paragraph instructions = new Paragraph(
            "Sie müssen Ihr Passwort ändern, bevor Sie fortfahren können.");
    private final PasswordField currentPasswordField = new PasswordField("Aktuelles Passwort");
    private final PasswordField newPasswordField = new PasswordField("Neues Passwort");
    private final PasswordField confirmPasswordField = new PasswordField("Neues Passwort bestätigen");
    private final Button changeButton = new Button("Passwort ändern");

    public PasswordChangeView(
            CurrentUser currentUser,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationContext authenticationContext) {
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationContext = authenticationContext;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("500px");
        setAlignItems(Alignment.CENTER);

        newPasswordField.setHelperText("Mindestens 8 Zeichen");
        confirmPasswordField.setHelperText("Geben Sie das neue Passwort erneut ein");

        changeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changeButton.addClickListener(e -> changePassword());

        add(title, instructions, currentPasswordField, newPasswordField, confirmPasswordField, changeButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (!currentUser.get().isPresent()) {
            event.forwardTo("/");
            return;
        }

        // Check if password change is actually required
        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount == null || !userAccount.isPasswordChangeRequired()) {
                // Password change not required, redirect to main view
                event.forwardTo("/");
            }
        });
    }

    private void changePassword() {
        String currentPassword = currentPasswordField.getValue();
        String newPassword = newPasswordField.getValue();
        String confirmPassword = confirmPasswordField.getValue();

        // Validation
        if (currentPassword == null || currentPassword.isEmpty()) {
            Notification.show("Bitte geben Sie Ihr aktuelles Passwort ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (newPassword == null || newPassword.length() < 8) {
            Notification.show("Das neue Passwort muss mindestens 8 Zeichen lang sein", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Notification.show("Die Passwörter stimmen nicht überein", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Get user account
        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount == null) {
                Notification.show("Benutzerkonto nicht gefunden", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Verify current password
            if (!passwordEncoder.matches(currentPassword, userAccount.getPasswordHash())) {
                Notification.show("Aktuelles Passwort ist falsch", 3000, Notification.Position.MIDDLE);
                currentPasswordField.clear();
                return;
            }

            // Update password
            String encodedPassword = passwordEncoder.encode(newPassword);
            userAccount.setPasswordHash(encodedPassword);
            userAccount.setPasswordChangeRequired(false);
            userAccount.setInitialPasswordSet(true);
            userAccountRepository.save(userAccount);

            Notification.show("Passwort erfolgreich geändert", 3000, Notification.Position.MIDDLE);

            // Redirect to login to re-authenticate with new password
            getUI().ifPresent(ui -> {
                ui.access(() -> {
                    // Logout and redirect to login
                    authenticationContext.logout();
                    ui.getPage().setLocation("/");
                });
            });
        });
    }
}
