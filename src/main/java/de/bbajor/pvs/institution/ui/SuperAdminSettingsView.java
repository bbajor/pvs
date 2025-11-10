package de.bbajor.pvs.institution.ui;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.institution.ui.tabs.InstitutionManagementTab;
import de.bbajor.pvs.institution.ui.tabs.KbvMasterDataTab;
import de.bbajor.pvs.institution.ui.tabs.MailSettingsTab;
import de.bbajor.pvs.institution.ui.tabs.MfaSettingsTab;
import de.bbajor.pvs.institution.ui.tabs.RecoveryEmailTab;
import de.bbajor.pvs.institution.ui.tabs.WhisperSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

/**
 * Super Admin Settings View.
 * Nur für SUPER_ADMIN sichtbar und umfasst zentrale Plattform-Konfigurationen.
 */
@Route("admin/super-settings")
@PageTitle("System-Einstellungen")
@RolesAllowed({ AppRoles.SUPER_ADMIN })
public class SuperAdminSettingsView extends Main {

    private final Tab institutionTab = new Tab("Institutionen & Administratoren");
    private final Tab mailTab = new Tab("Mail-Server");
    private final Tab mfaTab = new Tab("Multi-Faktor-Authentifizierung");
    private final Tab recoveryEmailTab = new Tab("Recovery-E-Mail & PGP");
    private final Tab whisperTab = new Tab("Whisper-Konfiguration");
    private final Tab kbvTab = new Tab("KBV-Stammdaten Import");

    private final VerticalLayout content = new VerticalLayout();

    public SuperAdminSettingsView(
            InstitutionManagementTab institutionManagementTab,
            MailSettingsTab mailSettingsTab,
            MfaSettingsTab mfaSettingsTab,
            RecoveryEmailTab recoveryEmailTabComponent,
            WhisperSettingsTab whisperSettingsTab,
            KbvMasterDataTab kbvMasterDataTab) {

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("System-Einstellungen"));

        Tabs tabs = new Tabs(institutionTab, mailTab, mfaTab, recoveryEmailTab, whisperTab, kbvTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == institutionTab) {
                content.add(institutionManagementTab);
            } else if (selected == mailTab) {
                content.add(mailSettingsTab);
            } else if (selected == mfaTab) {
                mfaSettingsTab.refresh();
                content.add(mfaSettingsTab);
            } else if (selected == recoveryEmailTab) {
                recoveryEmailTabComponent.refresh();
                content.add(recoveryEmailTabComponent);
            } else if (selected == whisperTab) {
                content.add(whisperSettingsTab);
            } else if (selected == kbvTab) {
                kbvMasterDataTab.refresh();
                content.add(kbvMasterDataTab);
            }
        });

        content.setSpacing(false);
        content.setPadding(false);
        content.setSizeFull();

        add(tabs, content);

        tabs.setSelectedTab(institutionTab);
        content.add(institutionManagementTab);
    }
}
