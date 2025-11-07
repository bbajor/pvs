package de.bbajor.pvs.institution.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLink;

import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.mfa.ui.MfaSetupView;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * MFA Settings Tab for Super Admin Settings View.
 * Shows MFA status and provides link to MFA setup.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MfaSettingsTab extends VerticalLayout {

    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;

    private Paragraph mfaStatus;
    private Button mfaSetupButton;
    private RouterLink mfaSetupLink;

    @PostConstruct
    private void init() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setMaxWidth("800px");

        mfaStatus = new Paragraph();
        mfaSetupButton = new Button("MFA einrichten");
        mfaSetupLink = new RouterLink("", MfaSetupView.class);

        mfaSetupLink.add(mfaSetupButton);
        mfaSetupButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(mfaStatus, mfaSetupLink);

        updateMfaStatus();
    }

    private void updateMfaStatus() {
        currentUser.get().ifPresent(user -> {
            UserAccount userAccount = userAccountRepository.findByUsername(user.getPreferredUsername()).orElse(null);
            if (userAccount != null) {
                if (userAccount.isMfaEnabled()) {
                    mfaStatus.setText("Multi-Faktor-Authentifizierung: ✅ Aktiviert");
                    mfaSetupButton.setText("MFA-Einstellungen ändern");
                } else {
                    mfaStatus.setText("Multi-Faktor-Authentifizierung: ❌ Nicht aktiviert");
                    mfaSetupButton.setText("MFA einrichten");
                }
            }
        });
    }

    /**
     * Refreshes the MFA status display.
     * Call this after returning from MFA setup to update the status.
     */
    public void refresh() {
        updateMfaStatus();
    }
}

