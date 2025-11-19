package de.bbajor.pvs.analytics.ui;

import com.vaadin.flow.component.html.Anchor;
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
import com.vaadin.flow.theme.lumo.LumoUtility.Gap;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;
import com.vaadin.flow.theme.lumo.LumoUtility.Padding;
import com.vaadin.flow.theme.lumo.LumoUtility.Width;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
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
        addClassName(Padding.MEDIUM);
        add(new ViewToolbar("Auswertungen"));
        add(createContent());
    }

    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.addClassNames(Gap.LARGE, Width.FULL);

        H2 title = new H2("Verfügbare Auswertungen");
        title.addClassNames(Margin.NONE, Margin.Bottom.MEDIUM);
        content.add(title);

        VerticalLayout linksContainer = new VerticalLayout();
        linksContainer.addClassNames(Gap.MEDIUM, Width.FULL);
        linksContainer.setSpacing(true);
        linksContainer.setPadding(false);

        // Link 1: Behandlungen pro Monat/Jahr
        linksContainer.add(createLinkCard(
            "Patientenbehandlungen",
            "Behandlungen pro Monat/Jahr",
            "analytics/treatments-over-time",
            VaadinIcon.LINE_CHART
        ));

        // Link 2: Behandlungen je Zeitslot
        linksContainer.add(createLinkCard(
            "Behandlungen je Zeitslot",
            "Behandlungen nach Zeitslot",
            "analytics/treatments-by-timeslot",
            VaadinIcon.CLOCK
        ));

        // Link 3: Patienten nach Altersklassen
        linksContainer.add(createLinkCard(
            "Patienten nach Altersklassen",
            "Verteilung der Patienten nach Altersgruppen",
            "analytics/patients-by-age",
            VaadinIcon.USERS
        ));

        // Link 4: Patienten Kasse/Privat
        linksContainer.add(createLinkCard(
            "Patienten Kasse/Privat",
            "Verteilung nach Versicherungsart",
            "analytics/patients-by-insurance-type",
            VaadinIcon.CREDIT_CARD
        ));

        // Link 5: Patienten nach Krankenversicherung
        linksContainer.add(createLinkCard(
            "Patienten nach Krankenversicherung",
            "Verteilung nach Versicherungsanbieter",
            "analytics/patients-by-insurance-provider",
            VaadinIcon.BUILDING
        ));

        // Link 6: Behandlungen je Medikament
        linksContainer.add(createLinkCard(
            "Behandlungen je Medikament",
            "Behandlungen nach Medikament",
            "analytics/treatments-by-medication",
            VaadinIcon.PILL
        ));

        content.add(linksContainer);
        return content;
    }

    private VerticalLayout createLinkCard(String title, String description, String route, VaadinIcon icon) {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(
            Padding.LARGE,
            Border.ALL,
            BorderRadius.MEDIUM,
            "shadow-s",
            Width.FULL,
            Display.FLEX,
            FlexDirection.COLUMN,
            Gap.SMALL
        );
        card.setSpacing(false);
        card.setPadding(true);
        card.getStyle().set("cursor", "pointer");
        card.getStyle().set("transition", "box-shadow 0.2s");

        // Hover-Effekt
        card.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.navigate(route));
        });
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-m)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle().set("box-shadow", "var(--lumo-box-shadow-s)");
        });

        // Icon und Titel
        VerticalLayout header = new VerticalLayout();
        header.setSpacing(false);
        header.setPadding(false);
        header.addClassNames(Display.FLEX, FlexDirection.ROW, Gap.MEDIUM, AlignItems.CENTER);

        Icon cardIcon = icon.create();
        cardIcon.addClassNames(Margin.NONE);
        header.add(cardIcon);

        H2 cardTitle = new H2(title);
        cardTitle.addClassNames(Margin.NONE);
        header.add(cardTitle);

        card.add(header);

        // Beschreibung
        Span descriptionSpan = new Span(description);
        descriptionSpan.addClassNames(Margin.NONE, Margin.Top.SMALL);
        descriptionSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        card.add(descriptionSpan);

        return card;
    }
}

