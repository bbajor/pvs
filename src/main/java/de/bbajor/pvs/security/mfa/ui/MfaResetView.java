package de.bbajor.pvs.security.mfa.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.security.mfa.service.MfaResetService;
import jakarta.annotation.security.PermitAll;

import java.util.Optional;

/**
 * View for resetting MFA via email link.
 */
@Route("mfa-reset")
@PageTitle("MFA zurücksetzen")
@PermitAll
public class MfaResetView extends VerticalLayout implements BeforeEnterObserver {

    private final MfaResetService mfaResetService;
    private final AuthenticationContext authenticationContext;

    private final H2 title = new H2("MFA zurücksetzen");
    private final Paragraph statusParagraph = new Paragraph();
    private final Button continueButton = new Button("Weiter zur MFA-Einrichtung");

    public MfaResetView(MfaResetService mfaResetService, AuthenticationContext authenticationContext) {
        this.mfaResetService = mfaResetService;
        this.authenticationContext = authenticationContext;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("600px");
        setAlignItems(Alignment.CENTER);

        continueButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        continueButton.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate("/mfa-setup"));
        });

        add(title, statusParagraph, continueButton);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        QueryParameters queryParams = event.getLocation().getQueryParameters();
        Optional<String> tokenParam = queryParams.getParameters().get("token").stream().findFirst();

        if (tokenParam.isEmpty()) {
            statusParagraph.setText("Ungültiger Reset-Link. Kein Token gefunden.");
            continueButton.setVisible(false);
            return;
        }

        String token = tokenParam.get();
        Optional<de.bbajor.pvs.security.domain.UserAccount> userOpt = mfaResetService.validateAndUseResetToken(token);

        if (userOpt.isPresent()) {
            statusParagraph.setText("MFA wurde erfolgreich zurückgesetzt. Bitte richten Sie MFA neu ein.");
            continueButton.setVisible(true);
            
            // Log the user in if not already authenticated
            if (!authenticationContext.isAuthenticated()) {
                // Note: In a real scenario, you might want to authenticate the user here
                // For now, we'll just show the message and let them navigate to setup
            }
        } else {
            statusParagraph.setText("Ungültiger oder abgelaufener Reset-Link. Bitte fordern Sie einen neuen Reset-Link an.");
            continueButton.setVisible(false);
        }
    }
}

