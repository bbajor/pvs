package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/settings")
@PageTitle("Hilfe - Einstellungen")
@PermitAll
public class SettingsHelpView extends HelpSubPageView {

    public SettingsHelpView() {
        super("Einstellungen", "help/settings");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.COG;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die Einstellungen ermöglichen es Ihnen, System- und Benutzereinstellungen zu konfigurieren. "
                + "Hier können Sie Praxisdaten, Benutzer, Standorte und weitere Konfigurationen verwalten."));

        section.add(createInfoCard("Funktionen",
                "• Praxisdaten verwalten\n"
                + "• Benutzerverwaltung\n"
                + "• Standortverwaltung\n"
                + "• Medikamenteneinstellungen\n"
                + "• Systemkonfiguration"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Praxisdaten: Verwalten Sie die Stammdaten Ihrer Praxis oder Einrichtung.",
                "Benutzerverwaltung: Erstellen, bearbeiten und verwalten Sie Benutzerkonten und Rollen.",
                "Standortverwaltung: Verwalten Sie verschiedene Standorte oder Filialen.",
                "Medikamenteneinstellungen: Konfigurieren Sie Einstellungen für die Medikamentendatenbank.",
                "Systemkonfiguration: Passen Sie Systemeinstellungen an Ihre Bedürfnisse an."
        }));

        section.add(createInfoCard("Berechtigungen",
                "Nur Benutzer mit den Rollen ADMIN, TECH_USER oder OWNER können die Einstellungen verwalten."));

        section.add(createInfoCard("Tipps",
                "• Überprüfen Sie regelmäßig die Benutzerverwaltung\n"
                + "• Aktualisieren Sie Praxisdaten bei Änderungen\n"
                + "• Dokumentieren Sie wichtige Konfigurationsänderungen"));

        return section;
    }
}

