package de.bbajor.pvs.institution.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.institution.ui.tabs.FeatureFlagsTab;
import de.bbajor.pvs.institution.ui.tabs.InstitutionManagementTab;
import de.bbajor.pvs.institution.ui.tabs.MailSettingsTab;
import de.bbajor.pvs.institution.ui.tabs.MfaSettingsTab;
import de.bbajor.pvs.institution.ui.tabs.RecoveryEmailTab;
import de.bbajor.pvs.institution.ui.tabs.WhisperSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

/**
 * Super Admin Settings View.
 * Only accessible by SUPER_ADMIN.
 * Contains:
 * - Institution and Administrator management
 * - Mail server configuration (SMTP)
 * - MFA configuration
 * - Whisper configuration (system-wide)
 */
@Route("admin/super-settings")
@PageTitle("System-Einstellungen")
@RolesAllowed({AppRoles.SUPER_ADMIN})
public class SuperAdminSettingsView extends Main {

    private final Tab institutionTab = new Tab("Institutionen & Administratoren");
    private final Tab featureFlagsTab = new Tab("Feature-Flags");
    private final Tab mailTab = new Tab("Mail-Server");
    private final Tab mfaTab = new Tab("Multi-Faktor-Authentifizierung");
    private final Tab recoveryEmailTab = new Tab("Recovery-E-Mail & PGP");
    private final Tab whisperTab = new Tab("Whisper-Konfiguration");

    private final VerticalLayout content = new VerticalLayout();

    public SuperAdminSettingsView(
            InstitutionManagementTab institutionManagementTab,
            FeatureFlagsTab featureFlagsTabComponent,
            MailSettingsTab mailSettingsTab,
            MfaSettingsTab mfaSettingsTab,
            RecoveryEmailTab recoveryEmailTabComponent,
            WhisperSettingsTab whisperSettingsTab) {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, "view-content", LumoUtility.Gap.MEDIUM);

        // Überschrift
        H1 title = new H1("System-Einstellungen");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        add(title);

        Tabs tabs = new Tabs(institutionTab, featureFlagsTab, mailTab, mfaTab, recoveryEmailTab, whisperTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == institutionTab) {
                content.add(institutionManagementTab);
            } else if (selected == featureFlagsTab) {
                content.add(featureFlagsTabComponent);
            } else if (selected == mailTab) {
                content.add(mailSettingsTab);
            } else if (selected == mfaTab) {
                mfaSettingsTab.refresh(); // Refresh MFA status when tab is selected
                content.add(mfaSettingsTab);
            } else if (selected == recoveryEmailTab) {
                recoveryEmailTabComponent.refresh(); // Refresh to check SMTP status
                content.add(recoveryEmailTabComponent);
            } else if (selected == whisperTab) {
                content.add(whisperSettingsTab);
            }
        });

        content.setSpacing(false);
        content.setPadding(false);
        content.setSizeFull();

        add(tabs, content);

        // Show first tab by default
        tabs.setSelectedTab(institutionTab);
        content.add(institutionManagementTab);
    }

}

