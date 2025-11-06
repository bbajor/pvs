package de.bbajor.pvs.institution.ui;

import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.institution.ui.tabs.KbvMasterDataTab;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

@Route("super-admin")
@PageTitle("Super-Admin Einstellungen")
@Menu(order = 50, icon = "vaadin:shield", title = "Super-Admin")
@RolesAllowed({ AppRoles.ADMIN, AppRoles.OWNER })
public class SuperAdminSettingsView extends Main {

    private final Tab kbvTab = new Tab("KBV-Stammdaten Import");
    private final VerticalLayout content = new VerticalLayout();

    public SuperAdminSettingsView(KbvMasterDataTab kbvMasterDataTab) {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Super-Admin Einstellungen"));

        Tabs tabs = new Tabs(kbvTab);
        tabs.setWidthFull();
        tabs.addSelectedChangeListener(event -> {
            content.removeAll();
            Tab selected = event.getSelectedTab();
            if (selected == kbvTab) {
                content.add(kbvMasterDataTab);
            }
        });

        content.setSpacing(false);
        content.setPadding(false);
        content.setSizeFull();

        add(tabs, content);

        // Show first tab by default
        tabs.setSelectedTab(kbvTab);
        content.add(kbvMasterDataTab);
    }
}
