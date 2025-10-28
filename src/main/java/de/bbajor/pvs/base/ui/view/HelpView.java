package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.PermitAll;

@Route("help")
@PageTitle("Hilfe")
@Menu(order = 98, icon = "vaadin:question-circle", title = "Hilfe")
@PermitAll
public class HelpView extends Main {

    public HelpView() {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.MEDIUM);

        add(new ViewToolbar("Hilfe"));

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setWidthFull();

        // Rollen-/Rechtesystem
        Section rolesSection = createRolesSection();
        content.add(rolesSection);

        add(content);
    }

    private Section createRolesSection() {
        Section section = new Section();
        section.add(new H2("Rollen- und Rechtesystem"));

        Paragraph intro = new Paragraph(
                "Das System verwendet ein rollenbasiertes Berechtigungssystem. Je nach zugewiesener Rolle haben Benutzer unterschiedliche Zugriffsrechte auf verschiedene Funktionen der Anwendung.");
        intro.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        section.add(intro);

        // Verfügbare Rollen
        VerticalLayout rolesLayout = new VerticalLayout();
        rolesLayout.setSpacing(true);
        rolesLayout.add(new H3("Verfügbare Rollen"));

        rolesLayout.add(createRoleInfo(AppRoles.OWNER, "Eigentümer",
                "Vollzugriff auf alle Funktionen des Systems. Kann alle administrativen Aufgaben durchführen."));
        rolesLayout.add(createRoleInfo(AppRoles.ADMIN, "Administrator",
                "Verwaltet Benutzer und Systemeinstellungen. Kann Behandlungen genehmigen und alle Bereiche verwalten."));
        rolesLayout.add(createRoleInfo(AppRoles.DOCTOR, "Arzt",
                "Kann Behandlungspläne erstellen, Termine buchen und Behandlungen genehmigen. Hat Zugriff auf alle medizinischen Daten."));
        rolesLayout.add(createRoleInfo(AppRoles.TECH_USER, "Technischer Benutzer",
                "Kann technische Einstellungen verwalten, Operationszentren verwalten und Medikamentendatenbank pflegen."));
        rolesLayout.add(createRoleInfo(AppRoles.MEDICAL_STAFF, "Medizinisches Personal",
                "Hat Lesezugriff auf Patienten- und Behandlungsdaten. Kann keine Termine buchen oder Behandlungen genehmigen."));
        rolesLayout.add(createRoleInfo(AppRoles.USER, "Benutzer",
                "Basis-Rolle mit eingeschränkten Zugriffsrechten. Oft in Kombination mit anderen Rollen verwendet."));

        section.add(rolesLayout);

        // Berechtigungen nach Bereich
        VerticalLayout permissionsLayout = new VerticalLayout();
        permissionsLayout.setSpacing(true);
        permissionsLayout.addClassNames(LumoUtility.Margin.Top.LARGE);
        permissionsLayout.add(new H3("Berechtigungen nach Bereich"));

        permissionsLayout.add(createPermissionTable(
                "Zu überprüfende Behandlungen",
                "ADMIN, DOCTOR, OWNER",
                "Nur Administratoren, Ärzte und der Eigentümer können die Aufgabenliste einsehen und Behandlungen genehmigen."));

        permissionsLayout.add(createPermissionTable(
                "IVOM-Verwaltung",
                "Alle (Lesezugriff), ADMIN, DOCTOR, TECH_USER (Buchung/Löschung)",
                "Alle Benutzer können Behandlungspläne einsehen. Termine buchen oder löschen können nur Administratoren, Ärzte und technische Benutzer."));

        permissionsLayout.add(createPermissionTable(
                "Medikamentendatenbank",
                "ADMIN, TECH_USER, OWNER",
                "Nur Administratoren, technische Benutzer und der Eigentümer können die Medikamentendatenbank verwalten."));

        permissionsLayout.add(createPermissionTable(
                "Operationszentren",
                "TECH_USER, ADMIN, OWNER",
                "Nur technische Benutzer, Administratoren und der Eigentümer können Operationszentren verwalten."));

        permissionsLayout.add(createPermissionTable(
                "Eigene Praxisdaten",
                "ADMIN, DOCTOR, TECH_USER, OWNER",
                "Administratoren, Ärzte, technische Benutzer und der Eigentümer können die Praxisdaten einsehen und bearbeiten."));

        permissionsLayout.add(createPermissionTable(
                "Benutzerverwaltung",
                "ADMIN, TECH_USER, OWNER",
                "Nur Administratoren, technische Benutzer und der Eigentümer können Benutzer erstellen, bearbeiten und löschen."));

        section.add(permissionsLayout);

        return section;
    }

    private VerticalLayout createRoleInfo(String roleCode, String roleName, String description) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(false);

        H3 roleHeader = new H3(roleName);
        roleHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        layout.add(roleHeader);

        Paragraph codeParagraph = new Paragraph("Rolle: " + roleCode);
        codeParagraph.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Bottom.SMALL);
        layout.add(codeParagraph);

        Paragraph descParagraph = new Paragraph(description);
        descParagraph.addClassNames(LumoUtility.Margin.Top.NONE);
        layout.add(descParagraph);

        return layout;
    }

    private VerticalLayout createPermissionTable(String area, String allowedRoles, String description) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(false);
        layout.setPadding(true);
        layout.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM);

        H3 areaHeader = new H3(area);
        areaHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        layout.add(areaHeader);

        Paragraph rolesParagraph = new Paragraph("Zugriffsberechtigt: " + allowedRoles);
        rolesParagraph.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Bottom.SMALL);
        layout.add(rolesParagraph);

        Paragraph descParagraph = new Paragraph(description);
        descParagraph.addClassNames(LumoUtility.Margin.Top.NONE);
        layout.add(descParagraph);

        return layout;
    }
}

