package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import jakarta.annotation.security.PermitAll;

@PermitAll
public abstract class HelpSubPageView extends Main {

    protected HelpSubPageView(String title, String route) {
        // Padding ZUERST setzen, dann sizeFull() - wichtig für box-sizing: border-box
        getStyle().set("padding", "var(--lumo-space-l, 1.5rem)");
        getStyle().set("box-sizing", "border-box");
        setSizeFull();
        addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                "view-content", LumoUtility.Gap.MEDIUM);

        // Überschrift
        H1 titleElement = new H1(title);
        titleElement.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        add(titleElement);

        // Zurück-Button
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.START);
        buttonLayout.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        
        Button backButton = new Button("Zurück zur Übersicht", VaadinIcon.ARROW_LEFT.create());
        backButton.addClickListener(e -> {
            com.vaadin.flow.component.UI.getCurrent().navigate("help");
        });
        buttonLayout.add(backButton);
        add(buttonLayout);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setWidthFull();
        content.setMaxWidth("1000px");
        content.addClassNames(LumoUtility.Margin.Horizontal.AUTO);

        // Icon und Titel
        Section headerSection = createHeaderSection(title, getIcon());
        content.add(headerSection);

        // Hauptinhalt
        Section mainSection = createMainSection();
        content.add(mainSection);

        // Weitere Informationen
        Section additionalSection = createAdditionalSection();
        if (additionalSection != null) {
            content.add(additionalSection);
        }

        add(content);
    }

    protected abstract VaadinIcon getIcon();

    protected abstract Section createMainSection();

    protected Section createAdditionalSection() {
        return null;
    }

    private Section createHeaderSection(String title, VaadinIcon icon) {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE, LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER, LumoUtility.Gap.MEDIUM);

        Icon headerIcon = icon.create();
        headerIcon.setSize("64px");
        headerIcon.setColor("var(--lumo-primary-color)");
        section.add(headerIcon);

        H2 header = new H2(title);
        header.addClassNames(LumoUtility.Margin.NONE);
        section.add(header);

        return section;
    }

    protected Section createInfoCard(String title, String content) {
        Section card = new Section();
        card.addClassNames(LumoUtility.Border.ALL, LumoUtility.BorderRadius.MEDIUM, LumoUtility.Padding.MEDIUM,
                LumoUtility.Background.BASE, LumoUtility.Margin.Bottom.MEDIUM);

        H3 cardTitle = new H3(title);
        cardTitle.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        card.add(cardTitle);

        Paragraph cardContent = new Paragraph(content);
        cardContent.addClassNames(LumoUtility.Margin.Top.NONE);
        card.add(cardContent);

        return card;
    }

    protected Section createFeatureList(String title, String[] features) {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        H3 sectionTitle = new H3(title);
        sectionTitle.addClassNames(LumoUtility.Margin.Top.NONE, LumoUtility.Margin.Bottom.SMALL);
        section.add(sectionTitle);

        VerticalLayout list = new VerticalLayout();
        list.setSpacing(true);
        list.setPadding(false);

        for (String feature : features) {
            Paragraph item = new Paragraph("• " + feature);
            item.addClassNames(LumoUtility.Margin.NONE, LumoUtility.Padding.Left.MEDIUM);
            list.add(item);
        }

        section.add(list);
        return section;
    }
}

