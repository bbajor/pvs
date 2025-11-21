package de.bbajor.pvs.analytics.ui;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.AlignItems;
import com.vaadin.flow.theme.lumo.LumoUtility.Border;
import com.vaadin.flow.theme.lumo.LumoUtility.BorderRadius;
import com.vaadin.flow.theme.lumo.LumoUtility.Display;
import com.vaadin.flow.theme.lumo.LumoUtility.FlexDirection;
import com.vaadin.flow.theme.lumo.LumoUtility.FontSize;
import com.vaadin.flow.theme.lumo.LumoUtility.FontWeight;
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;

import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Übersichtsseite für Analytics und Auswertungen mit Links zu den einzelnen Auswertungen.
 */
@Route("analytics")
@PageTitle("Auswertungen")
@Menu(order = 7, icon = "vaadin:chart", title = "Auswertungen")
@RolesAllowed({ AppRoles.TECH_USER, AppRoles.ADMIN, AppRoles.OWNER })
@Slf4j
@RequiredArgsConstructor
public class AnalyticsOverviewView extends Main implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Padding ZUERST setzen, dann sizeFull() - wichtig für box-sizing: border-box
        getStyle().set("padding", "var(--lumo-space-l, 1.5rem)");
        getStyle().set("box-sizing", "border-box");
        setSizeFull();
        addClassNames("view-content", Display.FLEX, FlexDirection.COLUMN);
        
        // Überschrift
        H1 title = new H1("Auswertungen");
        title.addClassNames(FontSize.XLARGE, FontWeight.SEMIBOLD, Margin.Bottom.LARGE);
        add(title);
        
        add(createContent());
    }

    private Div createContent() {
        Div content = new Div();
        content.setWidthFull();
        content.getStyle().set("display", "grid");
        content.getStyle().set("grid-template-columns", "repeat(auto-fit, minmax(300px, 1fr))");
        content.getStyle().set("gap", "var(--lumo-space-l, 1.5rem)");
        content.getStyle().set("margin-bottom", "0");
        content.addClassNames(Width.FULL);

        // Link 1: Behandlungen pro Monat/Jahr
        content.add(createLinkCard(
            "Patientenbehandlungen",
            "Behandlungen pro Monat/Jahr",
            "analytics/treatments-over-time",
            VaadinIcon.LINE_CHART,
            "var(--lumo-primary-color)"
        ));

        // Link 2: Behandlungen je Zeitslot
        content.add(createLinkCard(
            "Behandlungen je Zeitslot",
            "Behandlungen nach Zeitslot",
            "analytics/treatments-by-timeslot",
            VaadinIcon.CLOCK,
            "var(--lumo-primary-color-50pct)"
        ));

        // Link 3: Patienten nach Altersklassen
        content.add(createLinkCard(
            "Patienten nach Altersklassen",
            "Verteilung der Patienten nach Altersgruppen",
            "analytics/patients-by-age",
            VaadinIcon.USERS,
            "var(--lumo-primary-color)"
        ));

        // Link 4: Patienten Kasse/Privat
        content.add(createLinkCard(
            "Patienten Kasse/Privat",
            "Verteilung nach Versicherungsart",
            "analytics/patients-by-insurance-type",
            VaadinIcon.CREDIT_CARD,
            "var(--lumo-primary-color-50pct)"
        ));

        // Link 5: Patienten nach Krankenversicherung
        content.add(createLinkCard(
            "Patienten nach Krankenversicherung",
            "Verteilung nach Versicherungsanbieter",
            "analytics/patients-by-insurance-provider",
            VaadinIcon.BUILDING,
            "var(--lumo-primary-color)"
        ));

        // Link 6: Behandlungen je Medikament
        content.add(createLinkCard(
            "Behandlungen je Medikament",
            "Behandlungen nach Medikament",
            "analytics/treatments-by-medication",
            VaadinIcon.PILL,
            "var(--lumo-primary-color-50pct)"
        ));

        return content;
    }

    private Div createLinkCard(String title, String description, String route, VaadinIcon icon, String iconColor) {
        Div card = new Div();
        card.addClassNames(
            Border.ALL,
            BorderRadius.MEDIUM,
            Display.FLEX,
            FlexDirection.COLUMN
        );
        card.getStyle()
            .set("cursor", "pointer")
            .set("transition", "all 0.2s ease")
            .set("background", "var(--lumo-base-color)")
            .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.08)")
            .set("min-height", "160px")
            .set("padding", "var(--lumo-space-l, 1.5rem)");

        // Hover-Effekt
        card.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(route));
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
        iconContainer.addClassNames(Display.FLEX, AlignItems.CENTER);
        iconContainer.getStyle()
            .set("margin-bottom", "var(--lumo-space-s, 0.75rem)");
        
        Icon cardIcon = icon.create();
        cardIcon.setSize("32px");
        cardIcon.setColor(iconColor);
        iconContainer.add(cardIcon);
        card.add(iconContainer);

        // Titel mit Abstand
        H2 cardTitle = new H2(title);
        cardTitle.addClassNames(Margin.NONE);
        cardTitle.getStyle()
            .set("margin-bottom", "var(--lumo-space-s, 0.75rem)")
            .set("font-size", "var(--lumo-font-size-l)")
            .set("font-weight", "600");
        card.add(cardTitle);

        // Beschreibung mit Abstand
        Span descriptionSpan = new Span(description);
        descriptionSpan.addClassNames(Margin.NONE);
        descriptionSpan.getStyle()
            .set("color", "var(--lumo-secondary-text-color)")
            .set("font-size", "var(--lumo-font-size-s)")
            .set("line-height", "1.5");
        card.add(descriptionSpan);

        return card;
    }
}

