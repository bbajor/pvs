package de.bbajor.pvs.settings.ui;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.settings.ui.tabs.InstitutionGeneralTab;
import de.bbajor.pvs.settings.ui.tabs.LocationManagementTab;
import de.bbajor.pvs.settings.ui.tabs.MedicationSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.UserSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

@Route("settings")
@PageTitle("Einstellungen")
@Menu(order = 100, icon = "vaadin:cog", title = "Einstellungen")
@RolesAllowed({ AppRoles.ADMIN, AppRoles.TECH_USER, AppRoles.OWNER })
public class SettingsView extends Main {

    private final Tab generalTab = new Tab("Allgemein");
    private final Tab locationTab = new Tab("Standorte");
    private final Tab userTab = new Tab("Benutzerverwaltung");
    private final Tab medicationTab = new Tab("Medikamentendatenbank");

    private final VerticalLayout content = new VerticalLayout();

    public SettingsView(InstitutionGeneralTab institutionGeneralTab, 
            LocationManagementTab locationManagementTab,
            UserSettingsTab userSettingsTab,
            MedicationSettingsTab medicationSettingsTab) {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Einstellungen"));

        Tabs tabs = new Tabs(generalTab, locationTab, userTab, medicationTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == generalTab) {
                content.add(institutionGeneralTab);
            } else if (selected == locationTab) {
                content.add(locationManagementTab);
            } else if (selected == userTab) {
                content.add(userSettingsTab);
            } else if (selected == medicationTab) {
                content.add(medicationSettingsTab);
            }
        });

        content.setSpacing(false);
        content.setPadding(false);
        content.setSizeFull();

        add(tabs, content);

        // Show first tab by default
        tabs.setSelectedTab(generalTab);
        content.add(institutionGeneralTab);
    }

}

