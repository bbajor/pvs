package de.bbajor.pvs.settings.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.institution.ui.tabs.MfaSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.settings.ui.tabs.AuditLogsTab;
import de.bbajor.pvs.settings.ui.tabs.InstitutionGeneralTab;
import de.bbajor.pvs.settings.ui.tabs.InsuranceSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.IvomPlannerTab;
import de.bbajor.pvs.settings.ui.tabs.LocationManagementTab;
import de.bbajor.pvs.settings.ui.tabs.MedicationSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.SchedulerManagementTab;
import de.bbajor.pvs.settings.ui.tabs.UserSettingsTab;
import jakarta.annotation.security.RolesAllowed;

@Route("settings")
@PageTitle("Einstellungen")
@Menu(order = 6, icon = "vaadin:cog", title = "Einstellungen")
@RolesAllowed({ AppRoles.ADMIN, AppRoles.TECH_USER, AppRoles.OWNER, AppRoles.INSTITUTION_ADMIN })
public class SettingsView extends Main {

    private final Tab generalTab = new Tab("Allgemein");
    private final Tab locationTab = new Tab("Standorte");
    private final Tab schedulerTab = new Tab("Terminplaner");
    private final Tab userTab = new Tab("Benutzerverwaltung");
    private final Tab medicationTab = new Tab("Medikamentendatenbank");
    private final Tab insuranceTab = new Tab("Versicherungen");
    private final Tab mfaTab = new Tab("Multi-Faktor-Authentifizierung");
    private final Tab ivomPlannerTab = new Tab("IVOM-Planer");
    private final Tab auditLogsTab = new Tab("Audit-Logs");
    
    private final IvomPlannerTab ivomPlannerTabComponent;

    private final VerticalLayout content = new VerticalLayout();
    
    private final AuditLogsTab auditLogsTabComponent;

    public SettingsView(InstitutionGeneralTab institutionGeneralTab,
            LocationManagementTab locationManagementTab,
            SchedulerManagementTab schedulerManagementTab,
            UserSettingsTab userSettingsTab,
            MedicationSettingsTab medicationSettingsTab,
            InsuranceSettingsTab insuranceSettingsTab,
            MfaSettingsTab mfaSettingsTab,
            IvomPlannerTab ivomPlannerTabComponent,
            AuditLogsTab auditLogsTabComponent) {
        this.auditLogsTabComponent = auditLogsTabComponent;
        this.ivomPlannerTabComponent = ivomPlannerTabComponent;
        // Überschrift
        H1 title = new H1("Einstellungen");
        add(title);

        Tabs tabs = new Tabs(generalTab, locationTab, schedulerTab, userTab, medicationTab, insuranceTab, mfaTab, ivomPlannerTab, auditLogsTab);
        tabs.setWidthFull();
        tabs.getStyle().set("flex-shrink", "0");
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == generalTab) {
                content.add(institutionGeneralTab);
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
            } else if (selected == ivomPlannerTab) {
                content.add(ivomPlannerTabComponent);
            } else if (selected == auditLogsTab) {
                auditLogsTabComponent.refresh(); // Refresh audit logs when tab is selected
                content.add(auditLogsTabComponent);
            }
        });

        // Container als Flexbox konfigurieren
        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("min-height", "0");

        content.setSizeFull();
        content.getStyle().set("flex-grow", "1");
        content.getStyle().set("min-height", "0");

        add(tabs, content);

        // Show first tab by default
        tabs.setSelectedTab(generalTab);
        content.add(institutionGeneralTab);
    }

}

