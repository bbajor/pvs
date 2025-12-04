package de.bbajor.pvs.security.pin.ui;

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
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.pin.service.PinResetService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

/**
 * View for resetting PIN using a reset token from recovery email.
 */
@Route("pin-reset")
@PageTitle("PIN zurücksetzen")
@AnonymousAllowed
public class PinResetView extends VerticalLayout implements BeforeEnterObserver {

    private final UserAccountRepository userAccountRepository;
    private final PinResetService pinResetService;
    private final PasswordEncoder passwordEncoder;

    private final H2 title = new H2("PIN zurücksetzen");
    private final Paragraph instructions = new Paragraph(
            "Bitte geben Sie eine neue 6-stellige PIN ein.");
    private final PasswordField pinField = new PasswordField("Neue PIN (6-stellig)");
    private final PasswordField pinConfirmField = new PasswordField("PIN bestätigen");
    private final Button resetButton = new Button("PIN zurücksetzen");

    private UserAccount userAccount;
    private String resetToken;

    public PinResetView(UserAccountRepository userAccountRepository, 
                       PinResetService pinResetService,
                       PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.pinResetService = pinResetService;
        this.passwordEncoder = passwordEncoder;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("500px");
        setAlignItems(Alignment.CENTER);

        pinField.setPlaceholder("000000");
        pinField.setMaxLength(6);
        pinField.setPattern("[0-9]{6}");
        pinField.setHelperText("6-stellige PIN");
        pinField.setWidthFull();

        pinConfirmField.setPlaceholder("000000");
        pinConfirmField.setMaxLength(6);
        pinConfirmField.setPattern("[0-9]{6}");
        pinConfirmField.setHelperText("PIN zur Bestätigung wiederholen");
        pinConfirmField.setWidthFull();

        resetButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        resetButton.addClickListener(e -> resetPin());

        add(title, instructions, pinField, pinConfirmField, resetButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters params = event.getLocation().getQueryParameters();
        Optional<String> tokenParam = params.getSingleParameter("token");
        
        if (tokenParam.isEmpty()) {
            Notification.show("Ungültiger oder fehlender Reset-Token", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("/"));
            return;
        }

        resetToken = tokenParam.get();
        Optional<UserAccount> userOpt = pinResetService.validateAndUseResetToken(resetToken);
        
        if (userOpt.isEmpty()) {
            Notification.show("Ungültiger oder abgelaufener Reset-Token", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("/"));
            return;
        }

        userAccount = userOpt.get();
        
        // Check if user has MFA enabled (required for PIN login)
        if (!userAccount.isMfaEnabled() || userAccount.getMfaSecret() == null) {
            Notification.show("MFA muss aktiviert sein, um PIN-Login zu nutzen", 3000, Notification.Position.MIDDLE);
            getUI().ifPresent(ui -> ui.navigate("/"));
            return;
        }
    }

    private void resetPin() {
        String pin = pinField.getValue();
        String pinConfirm = pinConfirmField.getValue();

        if (pin == null || pin.length() != 6) {
            Notification.show("Bitte geben Sie eine 6-stellige PIN ein", 3000, Notification.Position.MIDDLE);
            return;
        }

        if (!pin.equals(pinConfirm)) {
            Notification.show("Die PINs stimmen nicht überein", 3000, Notification.Position.MIDDLE);
            pinConfirmField.clear();
            return;
        }

        if (userAccount == null) {
            Notification.show("Benutzerkonto nicht gefunden", 3000, Notification.Position.MIDDLE);
            return;
        }

        // Hash and save new PIN
        String pinHash = passwordEncoder.encode(pin);
        userAccount.setPinHash(pinHash);
        userAccountRepository.save(userAccount);

        Notification.show("PIN erfolgreich zurückgesetzt!", 3000, Notification.Position.MIDDLE);
        
        // Redirect to login
        getUI().ifPresent(ui -> ui.navigate("/"));
    }
}

