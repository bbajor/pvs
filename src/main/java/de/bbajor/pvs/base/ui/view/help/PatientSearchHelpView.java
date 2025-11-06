package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/patient-search")
@PageTitle("Hilfe - Patientensuche")
@PermitAll
public class PatientSearchHelpView extends HelpSubPageView {

    public PatientSearchHelpView() {
        super("Patientensuche", "help/patient-search");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.MALE;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die Patientensuche ermöglicht es Ihnen, Patienten zu verwalten, zu suchen und deren Daten zu bearbeiten. "
                + "Hier können Sie neue Patienten anlegen, bestehende Daten aktualisieren und nach Patienten suchen."));

        section.add(createInfoCard("Funktionen",
                "• Patienten anlegen und bearbeiten\n"
                + "• Erweiterte Suchfunktion mit Filtern\n"
                + "• Patientenverwaltung mit allen relevanten Daten\n"
                + "• Integration mit anderen Modulen"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Neuen Patienten anlegen: Klicken Sie auf 'Patienten anlegen', um einen neuen Patienten zu erstellen.",
                "Suche und Filter: Verwenden Sie die Filterfelder in der Tabellenkopfzeile, um nach Nachname, Vorname, Geburtsdatum oder Versicherung zu filtern.",
                "Patientendetails anzeigen: Klicken Sie auf einen Patienten in der Tabelle, um die Details anzuzeigen und zu bearbeiten.",
                "Echtzeit-Filterung: Die Filterung erfolgt automatisch während der Eingabe."
        }));

        section.add(createInfoCard("Suchfunktionen",
                "Die Patientensuche unterstützt Filterung nach:\n"
                + "• Nachname\n"
                + "• Vorname\n"
                + "• Geburtsdatum\n"
                + "• Krankenkasse/Versicherung\n\n"
                + "Die Suche ist case-insensitive und findet Teilübereinstimmungen."));

        section.add(createInfoCard("Tipps",
                "• Verwenden Sie die Filterfelder für eine präzise Suche\n"
                + "• Klicken Sie doppelt auf einen Patienten für schnellen Zugriff\n"
                + "• Alle Änderungen werden automatisch gespeichert"));

        return section;
    }
}

