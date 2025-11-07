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
import de.bbajor.pvs.institution.ui.tabs.WhisperSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

/**
 * Super Admin Settings View.
 * Only accessible by SUPER_ADMIN.
 * Contains:
 * - Institution and Administrator management
 * - Whisper configuration (system-wide)
 */
@Route("admin/super-settings")
@PageTitle("System-Einstellungen")
@RolesAllowed({AppRoles.SUPER_ADMIN})
public class SuperAdminSettingsView extends Main {

    private final Tab institutionTab = new Tab("Institutionen & Administratoren");
    private final Tab whisperTab = new Tab("Whisper-Konfiguration");

    private final VerticalLayout content = new VerticalLayout();

    public SuperAdminSettingsView(
            InstitutionManagementTab institutionManagementTab,
            WhisperSettingsTab whisperSettingsTab) {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("System-Einstellungen"));

        Tabs tabs = new Tabs(institutionTab, whisperTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == institutionTab) {
                content.add(institutionManagementTab);
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

