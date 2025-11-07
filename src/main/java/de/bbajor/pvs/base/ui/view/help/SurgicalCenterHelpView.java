package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/surgicalcenter")
@PageTitle("Hilfe - Operationszentren")
@PermitAll
public class SurgicalCenterHelpView extends HelpSubPageView {

    public SurgicalCenterHelpView() {
        super("Operationszentren", "help/surgicalcenter");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.BUILDING;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die Operationszentren-Verwaltung ermöglicht es Ihnen, operative Einrichtungen zu verwalten, "
                + "an denen Behandlungen durchgeführt werden. Hier können Sie Kontaktdaten, Adressen und weitere Informationen pflegen."));

        section.add(createInfoCard("Funktionen",
                "• Operationszentren anlegen und bearbeiten\n"
                + "• Kontaktdaten verwalten\n"
                + "• Adressen und Standorte pflegen\n"
                + "• Suche nach Operationszentren\n"
                + "• Kontaktpersonen verwalten"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Neues Operationszentrum anlegen: Klicken Sie auf das Plus-Icon, um ein neues Operationszentrum anzulegen.",
                "Operationszentrum bearbeiten: Klicken Sie auf einen Eintrag in der Tabelle, um Details anzuzeigen und zu bearbeiten.",
                "Suche: Verwenden Sie das Suchfeld, um nach Operationszentren zu suchen.",
                "Kontaktdaten: Verwalten Sie Adressen, Telefonnummern, E-Mail-Adressen und Kontaktpersonen."
        }));

        section.add(createInfoCard("Verwaltete Informationen",
                "Für jedes Operationszentrum können folgende Informationen erfasst werden:\n"
                + "• Name der Einrichtung\n"
                + "• Adresse\n"
                + "• Telefonnummer\n"
                + "• E-Mail-Adresse\n"
                + "• Name der Kontaktperson\n"
                + "• Telefonnummer der Kontaktperson"));

        section.add(createInfoCard("Berechtigungen",
                "Nur Benutzer mit den Rollen TECH_USER, ADMIN oder OWNER können Operationszentren verwalten."));

        section.add(createInfoCard("Tipps",
                "• Pflegen Sie alle Kontaktdaten vollständig\n"
                + "• Aktualisieren Sie Änderungen zeitnah\n"
                + "• Verwenden Sie die Suche für schnellen Zugriff"));

        return section;
    }
}

