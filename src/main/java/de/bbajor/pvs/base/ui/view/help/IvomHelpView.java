package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/ivom")
@PageTitle("Hilfe - IVOM-Behandlungsplan")
@PermitAll
public class IvomHelpView extends HelpSubPageView {

    public IvomHelpView() {
        super("IVOM-Behandlungsplan", "help/ivom");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.CALENDAR_USER;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die IVOM-Verwaltung ermöglicht es Ihnen, intravitreale Behandlungspläne zu erstellen, zu verwalten und zu überwachen. "
                + "Hier können Sie Patientenbehandlungen planen, Termine buchen und Wochenlisten generieren."));

        section.add(createInfoCard("Funktionen",
                "• Behandlungspläne erstellen und bearbeiten\n"
                + "• Patienten suchen und zuordnen\n"
                + "• Termine für Behandlungen buchen\n"
                + "• Wochenlisten für OP-Slots generieren\n"
                + "• Behandlungsdaten verwalten"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Suche nach Patienten: Verwenden Sie das Suchfeld, um nach Name, Vorname, Geburtsdatum oder Krankenkasse zu suchen.",
                "Neuen Behandlungsplan erstellen: Klicken Sie auf das Plus-Icon, um einen neuen Behandlungsplan anzulegen.",
                "Wochenliste generieren: Der Button 'Wochenliste anzeigen' erstellt eine Übersicht aller Behandlungen für die aktuelle Woche.",
                "Behandlungsplan bearbeiten: Klicken Sie auf einen Eintrag in der Tabelle, um Details anzuzeigen und zu bearbeiten."
        }));

        section.add(createInfoCard("Berechtigungen",
                "Alle Benutzer können Behandlungspläne einsehen. Um Termine zu buchen oder zu löschen, benötigen Sie die Rolle ADMIN, DOCTOR oder TECH_USER."));

        section.add(createInfoCard("Tipps",
                "• Verwenden Sie die Suchfunktion, um schnell Patienten zu finden\n"
                + "• Die Wochenliste hilft bei der Planung der OP-Slots\n"
                + "• Behandlungspläne können jederzeit aktualisiert werden"));

        return section;
    }
}

