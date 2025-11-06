package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

        add(new ViewToolbar("Hilfe & Dokumentation"));

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setWidthFull();
        content.setMaxWidth("1200px");
        content.addClassNames(LumoUtility.Margin.Horizontal.AUTO);

        // Willkommensbereich
        Section welcomeSection = createWelcomeSection();
        content.add(welcomeSection);

        // Funktionsübersicht
        Section functionsSection = createFunctionsSection();
        content.add(functionsSection);

        // Rollen-/Rechtesystem
        Section rolesSection = createRolesSection();
        content.add(rolesSection);

        add(content);
    }

    private Section createWelcomeSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        H2 welcomeHeader = new H2("Willkommen im PVS-Hilfezentrum");
        welcomeHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(welcomeHeader);

        Paragraph welcomeText = new Paragraph(
                "Hier finden Sie eine Übersicht über alle Funktionen des Systems sowie detaillierte Anleitungen für die einzelnen Bereiche. "
                + "Wählen Sie eine Funktion aus, um mehr zu erfahren.");
        welcomeText.addClassNames(LumoUtility.FontSize.LARGE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(welcomeText);

        return section;
    }

    private Section createFunctionsSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        H2 functionsHeader = new H2("Funktionsübersicht");
        functionsHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(functionsHeader);

        // Karten-Layout für Funktionen
        HorizontalLayout cardsRow1 = new HorizontalLayout();
        cardsRow1.setWidthFull();
        cardsRow1.setSpacing(true);
        cardsRow1.getStyle().set("flex-wrap", "wrap");

        cardsRow1.add(createFunctionCard("IVOM-Behandlungsplan", "Verwaltung von intravitrealen Behandlungsplänen",
                VaadinIcon.CALENDAR_USER, "help/ivom", "var(--lumo-primary-color)"));
        cardsRow1.add(createFunctionCard("Patientensuche", "Patientenverwaltung und Suche",
                VaadinIcon.MALE, "help/patient-search", "var(--lumo-success-color)"));
        cardsRow1.add(createFunctionCard("Terminkalender", "Terminverwaltung und Buchung",
                VaadinIcon.CALENDAR, "help/appointment-calendar", "var(--lumo-primary-color)"));
        cardsRow1.add(createFunctionCard("Aufgabenliste", "Zu überprüfende Behandlungen",
                VaadinIcon.CLIPBOARD_CHECK, "help/aufgabenliste", "var(--lumo-error-color)"));

        HorizontalLayout cardsRow2 = new HorizontalLayout();
        cardsRow2.setWidthFull();
        cardsRow2.setSpacing(true);
        cardsRow2.getStyle().set("flex-wrap", "wrap");

        cardsRow2.add(createFunctionCard("Medikamentendatenbank", "Verwaltung der Medikamentendatenbank",
                VaadinIcon.PILL, "help/ivom-drugs", "var(--lumo-success-color)"));
        cardsRow2.add(createFunctionCard("Augen-Termine", "Augenheilkundliche Patiententermine",
                VaadinIcon.EYE, "help/augen-termine", "var(--lumo-primary-color)"));
        cardsRow2.add(createFunctionCard("Operationszentren", "Verwaltung von Operationszentren",
                VaadinIcon.BUILDING, "help/surgicalcenter", "var(--lumo-contrast-50pct)"));
        cardsRow2.add(createFunctionCard("Einstellungen", "System- und Benutzereinstellungen",
                VaadinIcon.COG, "help/settings", "var(--lumo-contrast-50pct)"));

        section.add(cardsRow1, cardsRow2);

        return section;
    }

    private Div createFunctionCard(String title, String description, VaadinIcon icon, String route, String color) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL);
        card.getStyle().set("min-width", "250px").set("flex", "1 1 250px")
                .set("cursor", "pointer").set("transition", "all 0.2s");

        // Hover-Effekt
        card.addClickListener(e -> {
            com.vaadin.flow.component.UI.getCurrent().navigate(route);
        });
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
            card.getStyle().set("transform", "translateY(-2px)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().remove("box-shadow");
            card.getStyle().remove("transform");
        });

        // Icon
        Icon cardIcon = icon.create();
        cardIcon.setSize("48px");
        cardIcon.setColor(color);
        card.add(cardIcon);

        // Titel
        H3 cardTitle = new H3(title);
        cardTitle.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.FontSize.MEDIUM);
        card.add(cardTitle);

        // Beschreibung
        Paragraph cardDesc = new Paragraph(description);
        cardDesc.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.FontSize.SMALL,
                LumoUtility.TextColor.SECONDARY);
        card.add(cardDesc);

        // Link-Text
        Anchor link = new Anchor(route, "Mehr erfahren →");
        link.addClassNames(LumoUtility.Margin.Top.AUTO, LumoUtility.FontSize.SMALL);
        link.getStyle().set("color", color);
        card.add(link);

        return card;
    }

    private Section createRolesSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Top.LARGE, LumoUtility.Padding.LARGE,
                LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.MEDIUM);

        H2 rolesHeader = new H2("Rollen- und Rechtesystem");
        rolesHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(rolesHeader);

        Paragraph intro = new Paragraph(
                "Das System verwendet ein rollenbasiertes Berechtigungssystem. Je nach zugewiesener Rolle haben Benutzer unterschiedliche Zugriffsrechte auf verschiedene Funktionen der Anwendung.");
        intro.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        section.add(intro);

        // Verfügbare Rollen
        VerticalLayout rolesLayout = new VerticalLayout();
        rolesLayout.setSpacing(true);
        rolesLayout.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        H3 rolesSubHeader = new H3("Verfügbare Rollen");
        rolesSubHeader.addClassNames(LumoUtility.Margin.Top.NONE);
        rolesLayout.add(rolesSubHeader);

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

        H3 permissionsSubHeader = new H3("Berechtigungen nach Bereich");
        permissionsSubHeader.addClassNames(LumoUtility.Margin.Top.NONE);
        permissionsLayout.add(permissionsSubHeader);

        permissionsLayout.add(createPermissionCard(
                "Zu überprüfende Behandlungen",
                "ADMIN, DOCTOR, OWNER",
                "Nur Administratoren, Ärzte und der Eigentümer können die Aufgabenliste einsehen und Behandlungen genehmigen."));

        permissionsLayout.add(createPermissionCard(
                "IVOM-Verwaltung",
                "Alle (Lesezugriff), ADMIN, DOCTOR, TECH_USER (Buchung/Löschung)",
                "Alle Benutzer können Behandlungspläne einsehen. Termine buchen oder löschen können nur Administratoren, Ärzte und technische Benutzer."));

        permissionsLayout.add(createPermissionCard(
                "Medikamentendatenbank",
                "ADMIN, TECH_USER, OWNER",
                "Nur Administratoren, technische Benutzer und der Eigentümer können die Medikamentendatenbank verwalten."));

        permissionsLayout.add(createPermissionCard(
                "Operationszentren",
                "TECH_USER, ADMIN, OWNER",
                "Nur technische Benutzer, Administratoren und der Eigentümer können Operationszentren verwalten."));

        permissionsLayout.add(createPermissionCard(
                "Eigene Praxisdaten",
                "ADMIN, DOCTOR, TECH_USER, OWNER",
                "Administratoren, Ärzte, technische Benutzer und der Eigentümer können die Praxisdaten einsehen und bearbeiten."));

        permissionsLayout.add(createPermissionCard(
                "Benutzerverwaltung",
                "ADMIN, TECH_USER, OWNER",
                "Nur Administratoren, technische Benutzer und der Eigentümer können Benutzer erstellen, bearbeiten und löschen."));

        section.add(permissionsLayout);

        return section;
    }

    private Div createRoleInfo(String roleCode, String roleName, String description) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE);

        H3 roleHeader = new H3(roleName);
        roleHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        card.add(roleHeader);

        Paragraph codeParagraph = new Paragraph("Rolle: " + roleCode);
        codeParagraph.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.TextColor.PRIMARY);
        card.add(codeParagraph);

        Paragraph descParagraph = new Paragraph(description);
        descParagraph.addClassNames(LumoUtility.Margin.Top.NONE);
        card.add(descParagraph);

        return card;
    }

    private Div createPermissionCard(String area, String allowedRoles, String description) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE, LumoUtility.Margin.Bottom.MEDIUM);

        H3 areaHeader = new H3(area);
        areaHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        card.add(areaHeader);

        Paragraph rolesParagraph = new Paragraph("Zugriffsberechtigt: " + allowedRoles);
        rolesParagraph.addClassNames(LumoUtility.FontWeight.SEMIBOLD, LumoUtility.Margin.Bottom.SMALL,
                LumoUtility.TextColor.PRIMARY);
        card.add(rolesParagraph);

        Paragraph descParagraph = new Paragraph(description);
        descParagraph.addClassNames(LumoUtility.Margin.Top.NONE);
        card.add(descParagraph);

        return card;
    }
}

