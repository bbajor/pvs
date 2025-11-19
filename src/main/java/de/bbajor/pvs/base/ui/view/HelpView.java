package de.bbajor.pvs.base.ui.view;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.PermitAll;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Route("help")
@PageTitle("Hilfe")
@Menu(order = 5, icon = "vaadin:question-circle", title = "Hilfe")
@PermitAll
public class HelpView extends Main {

    public HelpView() {
        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, "view-content", LumoUtility.Gap.MEDIUM);

        // Überschrift
        H1 title = new H1("Hilfe & Dokumentation");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        add(title);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setWidthFull();
        content.setMaxWidth("1200px");
        content.addClassNames(LumoUtility.Margin.Horizontal.AUTO);

        // Willkommensbereich
        Section welcomeSection = createWelcomeSection();
        content.add(welcomeSection);

        // Funktionsübersicht - rollenbasiert
        Section functionsSection = createFunctionsSection();
        content.add(functionsSection);

        // Rollen-/Rechtesystem - rollenbasiert
        Section rolesSection = createRolesSection();
        content.add(rolesSection);

        add(content);
    }
    
    /**
     * Prüft, ob der Benutzer eine bestimmte Rolle hat.
     */
    private boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
    
    /**
     * Prüft, ob der Benutzer Zugriff auf einen Bereich hat.
     */
    private boolean hasAccessToArea(String area) {
        // IVOM-Planer: Alle haben Lesezugriff
        if (area.contains("IVOM") || area.contains("ivom")) {
            return true;
        }
        // Zu überprüfende Behandlungen: ADMIN, DOCTOR, OWNER
        if (area.contains("überprüfende") || area.contains("Aufgabenliste")) {
            return hasRole(AppRoles.ADMIN) || hasRole(AppRoles.DOCTOR) || hasRole(AppRoles.OWNER);
        }
        // Medikamentendatenbank: ADMIN, TECH_USER, OWNER
        if (area.contains("Medikament")) {
            return hasRole(AppRoles.ADMIN) || hasRole(AppRoles.TECH_USER) || hasRole(AppRoles.OWNER);
        }
        // Operationszentren: TECH_USER, ADMIN, OWNER
        if (area.contains("Operationszentren") || area.contains("OP-Planer")) {
            return hasRole(AppRoles.TECH_USER) || hasRole(AppRoles.ADMIN) || hasRole(AppRoles.OWNER);
        }
        // Einstellungen: ADMIN, TECH_USER, OWNER
        if (area.contains("Einstellungen")) {
            return hasRole(AppRoles.ADMIN) || hasRole(AppRoles.TECH_USER) || hasRole(AppRoles.OWNER);
        }
        // Standard: Alle haben Zugriff
        return true;
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

        // Nur anzeigen, wenn Benutzer Zugriff hat
        if (hasAccessToArea("IVOM")) {
            cardsRow1.add(createFunctionCard("IVOM-Behandlungsplan", "Verwaltung von intravitrealen Behandlungsplänen",
                    VaadinIcon.CALENDAR_USER, "help/ivom", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("Patient")) {
            cardsRow1.add(createFunctionCard("Patientensuche", "Patientenverwaltung und Suche",
                    VaadinIcon.MALE, "help/patient-search", "var(--lumo-success-color)"));
        }
        if (hasAccessToArea("Termin")) {
            cardsRow1.add(createFunctionCard("Terminkalender", "Terminverwaltung und Buchung",
                    VaadinIcon.CALENDAR, "help/appointment-calendar", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("überprüfende")) {
            cardsRow1.add(createFunctionCard("Aufgabenliste", "Zu überprüfende Behandlungen",
                    VaadinIcon.CLIPBOARD_CHECK, "help/aufgabenliste", "var(--lumo-error-color)"));
        }

        HorizontalLayout cardsRow2 = new HorizontalLayout();
        cardsRow2.setWidthFull();
        cardsRow2.setSpacing(true);
        cardsRow2.getStyle().set("flex-wrap", "wrap");

        // Nur anzeigen, wenn Benutzer Zugriff hat
        if (hasAccessToArea("Medikament")) {
            cardsRow2.add(createFunctionCard("Medikamentendatenbank", "Verwaltung der Medikamentendatenbank",
                    VaadinIcon.PILL, "help/ivom-drugs", "var(--lumo-success-color)"));
        }
        if (hasAccessToArea("Augen")) {
            cardsRow2.add(createFunctionCard("Augen-Termine", "Augenheilkundliche Patiententermine",
                    VaadinIcon.EYE, "help/augen-termine", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("Operationszentren")) {
            cardsRow2.add(createFunctionCard("Operationszentren", "Verwaltung von Operationszentren",
                    VaadinIcon.BUILDING, "help/surgicalcenter", "var(--lumo-contrast-50pct)"));
        }
        if (hasAccessToArea("Einstellungen")) {
            cardsRow2.add(createFunctionCard("Einstellungen", "System- und Benutzereinstellungen",
                    VaadinIcon.COG, "help/settings", "var(--lumo-contrast-50pct)"));
        }

        section.add(cardsRow1, cardsRow2);

        return section;
    }

    private Div createFunctionCard(String title, String description, VaadinIcon icon, String route, String color) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Gap.SMALL, LumoUtility.Margin.Bottom.MEDIUM);
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

        // Berechtigungsmatrix
        H3 matrixHeader = new H3("Berechtigungsmatrix");
        matrixHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        section.add(matrixHeader);

        Div matrixContainer = createPermissionMatrix();
        matrixContainer.addClassNames(LumoUtility.Margin.Bottom.LARGE);
        section.add(matrixContainer);

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
                "IVOM-Planer",
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

    /**
     * Erstellt eine Berechtigungsmatrix als Div-basierte Tabelle mit CSS Grid.
     * Zeigt für jeden Bereich an, welche Rollen welche Rechte haben.
     */
    private Div createPermissionMatrix() {
        Div container = new Div();
        container.addClassNames(LumoUtility.Overflow.AUTO, LumoUtility.Margin.Bottom.MEDIUM);
        container.getStyle().set("max-width", "100%");

        // Matrix-Daten: Bereich -> Rollen mit Rechten
        // Format: "✓" = Vollzugriff, "L" = Lesezugriff, "-" = Kein Zugriff
        String[][] matrixData = {
            // Bereich, OWNER, ADMIN, DOCTOR, TECH_USER, MEDICAL_STAFF, USER
            {"Zu überprüfende Behandlungen", "✓", "✓", "✓", "-", "-", "-"},
            {"IVOM-Planer (Lesen)", "✓", "✓", "✓", "✓", "✓", "✓"},
            {"IVOM-Planer (Schreiben)", "✓", "✓", "✓", "✓", "-", "-"},
            {"Medikamentendatenbank", "✓", "✓", "-", "✓", "-", "-"},
            {"Operationszentren", "✓", "✓", "-", "✓", "-", "-"},
            {"Eigene Praxisdaten", "✓", "✓", "✓", "✓", "-", "-"},
            {"Benutzerverwaltung", "✓", "✓", "-", "✓", "-", "-"}
        };

        String[] roleNames = {"Bereich", "Eigentümer", "Admin", "Arzt", "Techn. User", "Med. Personal", "Benutzer"};

        // CSS Grid Container
        Div gridContainer = new Div();
        gridContainer.getStyle().set("display", "grid");
        gridContainer.getStyle().set("grid-template-columns", "200px repeat(6, 1fr)");
        gridContainer.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM);
        gridContainer.getStyle().set("border-collapse", "collapse");

        // Header-Zeile
        for (int col = 0; col < roleNames.length; col++) {
            Div headerCell = new Div();
            headerCell.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);
            if (col == 0) {
                headerCell.addClassNames(LumoUtility.Background.CONTRAST_10);
            } else {
                headerCell.addClassNames(LumoUtility.Background.CONTRAST_10);
                headerCell.getStyle().set("text-align", "center");
            }
            headerCell.getStyle().set("border-right", "1px solid var(--lumo-contrast-20pct)");
            if (col == roleNames.length - 1) {
                headerCell.getStyle().remove("border-right");
            }
            headerCell.setText(roleNames[col]);
            gridContainer.add(headerCell);
        }

        // Daten-Zeilen
        for (int rowIdx = 0; rowIdx < matrixData.length; rowIdx++) {
            // Bereich-Spalte
            Div areaCell = new Div();
            areaCell.addClassNames(LumoUtility.Padding.MEDIUM, LumoUtility.FontWeight.SEMIBOLD);
            if (rowIdx % 2 == 0) {
                areaCell.addClassNames(LumoUtility.Background.BASE);
            } else {
                areaCell.addClassNames(LumoUtility.Background.CONTRAST_5);
            }
            areaCell.getStyle().set("border-right", "1px solid var(--lumo-contrast-20pct)");
            areaCell.setText(matrixData[rowIdx][0]);
            gridContainer.add(areaCell);

            // Rollen-Spalten
            for (int colIdx = 1; colIdx < matrixData[rowIdx].length; colIdx++) {
                Div cell = new Div();
                cell.addClassNames(LumoUtility.Padding.MEDIUM);
                cell.getStyle().set("text-align", "center");
                if (rowIdx % 2 == 0) {
                    cell.addClassNames(LumoUtility.Background.BASE);
                } else {
                    cell.addClassNames(LumoUtility.Background.CONTRAST_5);
                }
                if (colIdx < matrixData[rowIdx].length - 1) {
                    cell.getStyle().set("border-right", "1px solid var(--lumo-contrast-20pct)");
                }

                String permission = matrixData[rowIdx][colIdx];
                Span permissionSpan = new Span();
                
                if ("✓".equals(permission)) {
                    permissionSpan.addClassNames(LumoUtility.TextColor.SUCCESS, LumoUtility.FontWeight.BOLD);
                    permissionSpan.setText("✓");
                } else if ("L".equals(permission)) {
                    permissionSpan.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD);
                    permissionSpan.setText("L");
                } else {
                    permissionSpan.addClassNames(LumoUtility.TextColor.SECONDARY);
                    permissionSpan.setText("—");
                }
                
                cell.add(permissionSpan);
                gridContainer.add(cell);
            }
        }

        container.add(gridContainer);

        // Legende
        Div legend = new Div();
        legend.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.Padding.SMALL, 
                LumoUtility.Background.CONTRAST_5, LumoUtility.BorderRadius.SMALL);
        
        Paragraph legendText = new Paragraph();
        legendText.addClassNames(LumoUtility.Margin.NONE, LumoUtility.FontSize.SMALL);
        legendText.add(new Span("Legende: "));
        
        Span fullAccess = new Span("✓ = Vollzugriff");
        fullAccess.addClassNames(LumoUtility.TextColor.SUCCESS, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Right.MEDIUM);
        legendText.add(fullAccess);
        
        Span readAccess = new Span("L = Lesezugriff");
        readAccess.addClassNames(LumoUtility.TextColor.PRIMARY, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Right.MEDIUM);
        legendText.add(readAccess);
        
        Span noAccess = new Span("— = Kein Zugriff");
        noAccess.addClassNames(LumoUtility.TextColor.SECONDARY);
        legendText.add(noAccess);
        
        legend.add(legendText);
        container.add(legend);

        return container;
    }
}

