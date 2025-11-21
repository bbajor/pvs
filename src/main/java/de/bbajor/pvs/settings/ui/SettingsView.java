package de.bbajor.pvs.settings.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.institution.ui.tabs.MfaSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.InstitutionGeneralTab;
import de.bbajor.pvs.settings.ui.tabs.InsuranceSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.LayoutSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.LocationManagementTab;
import de.bbajor.pvs.settings.ui.tabs.MedicationSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.SchedulerManagementTab;
import de.bbajor.pvs.settings.ui.tabs.UserSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

@Route("settings")
@PageTitle("Einstellungen")
@Menu(order = 6, icon = "vaadin:cog", title = "Einstellungen")
@RolesAllowed({ AppRoles.ADMIN, AppRoles.TECH_USER, AppRoles.OWNER })
public class SettingsView extends Main {

    private final Tab generalTab = new Tab("Allgemein");
    private final Tab layoutTab = new Tab("Layout");
    private final Tab locationTab = new Tab("Standorte");
    private final Tab schedulerTab = new Tab("Terminplaner");
    private final Tab userTab = new Tab("Benutzerverwaltung");
    private final Tab medicationTab = new Tab("Medikamentendatenbank");
    private final Tab insuranceTab = new Tab("Versicherungen");
    private final Tab mfaTab = new Tab("Multi-Faktor-Authentifizierung");

    private final VerticalLayout content = new VerticalLayout();

    public SettingsView(InstitutionGeneralTab institutionGeneralTab,
            LayoutSettingsTab layoutSettingsTab,
            LocationManagementTab locationManagementTab,
            SchedulerManagementTab schedulerManagementTab,
            UserSettingsTab userSettingsTab,
            MedicationSettingsTab medicationSettingsTab,
            InsuranceSettingsTab insuranceSettingsTab,
            MfaSettingsTab mfaSettingsTab) {
        // Padding ZUERST setzen, dann sizeFull() - wichtig für box-sizing: border-box
        getStyle().set("padding", "var(--lumo-space-l, 1.5rem)");
        getStyle().set("box-sizing", "border-box");
        getStyle().set("overflow", "hidden"); // Verhindert Scrolling auf Main-Ebene
        setSizeFull();
        addClassNames(LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, "view-content", LumoUtility.Gap.MEDIUM);

        // Überschrift
        H1 title = new H1("Einstellungen");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        title.getStyle().set("flex-shrink", "0");
        add(title);

        Tabs tabs = new Tabs(generalTab, layoutTab, locationTab, schedulerTab, userTab, medicationTab, insuranceTab, mfaTab);
        tabs.setWidthFull();
        tabs.getStyle().set("flex-shrink", "0");
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == generalTab) {
                content.add(institutionGeneralTab);
            } else if (selected == layoutTab) {
                content.add(layoutSettingsTab);
            } else if (selected == locationTab) {
                content.add(locationManagementTab);
            } else if (selected == schedulerTab) {
                content.add(schedulerManagementTab);
            } else if (selected == userTab) {
                content.add(userSettingsTab);
            } else if (selected == medicationTab) {
                content.add(medicationSettingsTab);
            } else if (selected == insuranceTab) {
                content.add(insuranceSettingsTab);
            } else if (selected == mfaTab) {
                mfaSettingsTab.refresh(); // Refresh MFA status when tab is selected
                content.add(mfaSettingsTab);
            }
        });

        content.setSpacing(false);
        content.setPadding(false);
        content.setSizeFull();
        content.getStyle().set("flex-grow", "1");
        content.getStyle().set("min-height", "0");
        content.getStyle().set("overflow", "auto");

        add(tabs, content);

        // Show first tab by default
        tabs.setSelectedTab(generalTab);
        content.add(institutionGeneralTab);
    }

}

