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
        // Padding ZUERST setzen, dann sizeFull() - wichtig für box-sizing: border-box
        getStyle().set("padding", "var(--lumo-space-l, 1.5rem)");
        getStyle().set("box-sizing", "border-box");
        getStyle().set("overflow", "hidden"); // Verhindert Scrolling auf Main-Ebene
        setSizeFull();
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                "view-content", LumoUtility.Gap.MEDIUM);

        // Überschrift
        H1 title = new H1("Hilfe & Dokumentation");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        title.getStyle().set("flex-shrink", "0");
        add(title);

        // Container für feste Inhalte (Willkommen + Funktionsübersicht-Header)
        VerticalLayout fixedContent = new VerticalLayout();
        fixedContent.setSpacing(true);
        fixedContent.setPadding(false);
        fixedContent.setWidthFull();
        fixedContent.setMaxWidth("1400px");
        fixedContent.addClassNames(LumoUtility.Margin.Horizontal.AUTO);
        fixedContent.getStyle().set("flex-shrink", "0");

        // Willkommensbereich
        Section welcomeSection = createWelcomeSection();
        fixedContent.add(welcomeSection);

        // Funktionsübersicht-Header (ohne Kacheln)
        H2 functionsHeader = new H2("Funktionsübersicht");
        functionsHeader.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.MEDIUM);
        fixedContent.add(functionsHeader);

        add(fixedContent);

        // Scrollbarer Container nur für die Kacheln
        Div scrollableContainer = new Div();
        scrollableContainer.setWidthFull();
        scrollableContainer.setMaxWidth("1400px");
        scrollableContainer.addClassNames(LumoUtility.Margin.Horizontal.AUTO);
        scrollableContainer.getStyle()
            .set("flex-grow", "1")
            .set("flex-shrink", "1")
            .set("min-height", "0")
            .set("overflow-y", "auto")
            .set("overflow-x", "hidden");

        // Kacheln-Container
        Div cardsContainer = createCardsContainer();
        scrollableContainer.add(cardsContainer);

        add(scrollableContainer);
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
        section.getStyle().set("flex-shrink", "0"); // Section soll nicht schrumpfen

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

    private Div createCardsContainer() {
        // Responsives Grid-Layout für Karten - wie in AnalyticsOverviewView
        Div cardsContainer = new Div();
        cardsContainer.setWidthFull();
        cardsContainer.getStyle()
            .set("display", "grid")
            .set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))")
            .set("gap", "var(--lumo-space-l, 1.5rem)")
            .set("margin-bottom", "var(--lumo-space-l, 1.5rem)");
        cardsContainer.addClassNames(LumoUtility.Width.FULL);

        // Nur anzeigen, wenn Benutzer Zugriff hat
        if (hasAccessToArea("IVOM")) {
            cardsContainer.add(createFunctionCard("IVOM-Behandlungsplan", "Verwaltung von intravitrealen Behandlungsplänen",
                    VaadinIcon.CALENDAR_USER, "help/ivom", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("Patient")) {
            cardsContainer.add(createFunctionCard("Patientensuche", "Patientenverwaltung und Suche",
                    VaadinIcon.MALE, "help/patient-search", "var(--lumo-primary-color-50pct)"));
        }
        if (hasAccessToArea("Termin")) {
            cardsContainer.add(createFunctionCard("Terminkalender", "Terminverwaltung und Buchung",
                    VaadinIcon.CALENDAR, "help/appointment-calendar", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("überprüfende")) {
            cardsContainer.add(createFunctionCard("Aufgabenliste", "Zu überprüfende Behandlungen",
                    VaadinIcon.CLIPBOARD_CHECK, "help/aufgabenliste", "var(--lumo-primary-color-50pct)"));
        }
        if (hasAccessToArea("Medikament")) {
            cardsContainer.add(createFunctionCard("Medikamentendatenbank", "Verwaltung der Medikamentendatenbank",
                    VaadinIcon.PILL, "help/ivom-drugs", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("Augen")) {
            cardsContainer.add(createFunctionCard("Augen-Termine", "Augenheilkundliche Patiententermine",
                    VaadinIcon.EYE, "help/augen-termine", "var(--lumo-primary-color-50pct)"));
        }
        if (hasAccessToArea("Operationszentren")) {
            cardsContainer.add(createFunctionCard("Operationszentren", "Verwaltung von Operationszentren",
                    VaadinIcon.BUILDING, "help/surgicalcenter", "var(--lumo-primary-color)"));
        }
        if (hasAccessToArea("Einstellungen")) {
            cardsContainer.add(createFunctionCard("Einstellungen", "System- und Benutzereinstellungen",
                    VaadinIcon.COG, "help/settings", "var(--lumo-primary-color-50pct)"));
        }
        
        // Rollen-/Rechtesystem Kachel
        cardsContainer.add(createFunctionCard("Rollen- und Rechtesystem", 
                "Übersicht über das rollenbasierte Berechtigungssystem, verfügbare Rollen und Berechtigungen nach Bereich",
                VaadinIcon.USER_STAR, "help/roles", "var(--lumo-primary-color)"));

        return cardsContainer;
    }

    private Div createFunctionCard(String title, String description, VaadinIcon icon, String route, String color) {
        Div card = new Div();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Background.BASE, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN);
        card.getStyle()
            .set("cursor", "pointer")
            .set("transition", "all 0.2s ease")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.08)")
            .set("min-height", "180px")
            .set("padding", "var(--lumo-space-l, 1.5rem)");

        // Hover-Effekt
        card.addClickListener(e -> {
            com.vaadin.flow.component.UI.getCurrent().navigate(route);
        });
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                .set("box-shadow", "0 8px 16px rgba(0, 0, 0, 0.12)")
                .set("transform", "translateY(-2px)")
                .set("border-color", "var(--lumo-primary-color-50pct)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.08)")
                .set("transform", "translateY(0)")
                .set("border-color", "var(--lumo-contrast-20pct)");
        });

        // Icon Container mit Abstand
        Div iconContainer = new Div();
        iconContainer.addClassNames(LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER);
        iconContainer.getStyle()
            .set("margin-bottom", "var(--lumo-space-s, 0.75rem)");
        
        Icon cardIcon = icon.create();
        cardIcon.setSize("36px");
        cardIcon.setColor(color);
        iconContainer.add(cardIcon);
        card.add(iconContainer);

        // Titel mit Abstand
        H3 cardTitle = new H3(title);
        cardTitle.addClassNames(LumoUtility.Margin.NONE);
        cardTitle.getStyle()
            .set("margin-bottom", "var(--lumo-space-s, 0.75rem)")
            .set("font-size", "var(--lumo-font-size-l)")
            .set("font-weight", "600");
        card.add(cardTitle);

        // Beschreibung mit Abstand
        Paragraph cardDesc = new Paragraph(description);
        cardDesc.addClassNames(LumoUtility.Margin.NONE);
        cardDesc.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("color", "var(--lumo-secondary-text-color)")
            .set("line-height", "1.5")
            .set("margin-bottom", "var(--lumo-space-s, 0.75rem)");
        card.add(cardDesc);

        // Link-Text mit Abstand
        Anchor link = new Anchor(route, "Mehr erfahren →");
        link.addClassNames(LumoUtility.Margin.Top.AUTO);
        link.getStyle()
            .set("font-size", "var(--lumo-font-size-s)")
            .set("font-weight", "500")
            .set("color", color)
            .set("margin-top", "auto");
        card.add(link);

        return card;
    }

}

