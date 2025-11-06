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
import de.bbajor.pvs.settings.ui.tabs.AiSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.KbvMasterDataTab;
import de.bbajor.pvs.settings.ui.tabs.MedicationSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.PracticeSettingsTab;
import de.bbajor.pvs.settings.ui.tabs.UserSettingsTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

@Route("settings")
@PageTitle("Einstellungen")
@Menu(order = 100, icon = "vaadin:cog", title = "Einstellungen")
@RolesAllowed({ AppRoles.ADMIN, AppRoles.TECH_USER, AppRoles.OWNER })
public class SettingsView extends Main {

    private final Tab aiTab = new Tab("KI-Module");
    private final Tab userTab = new Tab("Benutzerverwaltung");
    private final Tab practiceTab = new Tab("Praxisverwaltung");
    private final Tab medicationTab = new Tab("Medikamentendatenbank");
    private final Tab kbvTab = new Tab("KBV-Stammdaten");

    private final VerticalLayout content = new VerticalLayout();

    public SettingsView(AiSettingsTab aiSettingsTab, UserSettingsTab userSettingsTab,
            PracticeSettingsTab practiceSettingsTab, MedicationSettingsTab medicationSettingsTab,
            KbvMasterDataTab kbvMasterDataTab) {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Einstellungen"));

        Tabs tabs = new Tabs(aiTab, userTab, practiceTab, medicationTab, kbvTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == aiTab) {
                content.add(aiSettingsTab);
            } else if (selected == userTab) {
                content.add(userSettingsTab);
            } else if (selected == practiceTab) {
                content.add(practiceSettingsTab);
            } else if (selected == medicationTab) {
                content.add(medicationSettingsTab);
            } else if (selected == kbvTab) {
                content.add(kbvMasterDataTab);
            }
        });

        content.setSpacing(false);
        content.setPadding(false);
        content.setSizeFull();

        add(tabs, content);

        // Show first tab by default
        tabs.setSelectedTab(aiTab);
        content.add(aiSettingsTab);
    }

}

