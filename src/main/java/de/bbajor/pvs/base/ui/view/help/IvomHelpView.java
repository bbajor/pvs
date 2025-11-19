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
                "Der IVOM-Planer ermöglicht es Ihnen, intravitreale Behandlungspläne zu erstellen, zu verwalten und zu überwachen. "
                + "Die Ansicht ist in zwei Tabs unterteilt: 'Behandlungspläne' für die Verwaltung der Pläne und 'Behandlungsprüfung' für die Überprüfung von Behandlungen."));

        section.add(createInfoCard("Tab: Behandlungspläne",
                "Im Tab 'Behandlungspläne' können Sie:\n"
                + "• Behandlungspläne erstellen und bearbeiten\n"
                + "• Patienten suchen und zuordnen\n"
                + "• Termine für Behandlungen buchen\n"
                + "• Wochenlisten für OP-Slots generieren\n"
                + "• Behandlungsdaten verwalten"));

        section.add(createInfoCard("Tab: Behandlungsprüfung",
                "Im Tab 'Behandlungsprüfung' finden Sie:\n"
                + "• Eine Aufgabenliste mit zu überprüfenden Behandlungen\n"
                + "• Filterung nach Beschreibung\n"
                + "• Ein-/Ausblenden abgeschlossener Aufgaben\n"
                + "• Aktualisierung der Aufgabenliste\n"
                + "• Doppelklick auf eine Aufgabe öffnet den Prüfdialog"));

        section.add(createFeatureList("Hauptfunktionen - Behandlungspläne", new String[] {
                "Suche nach Patienten: Verwenden Sie das Suchfeld, um nach Name, Vorname, Geburtsjahr, Krankenkasse, Diagnose oder zusätzlichen Informationen zu suchen.",
                "Neuen Behandlungsplan erstellen: Klicken Sie auf 'Neuer Behandlungsplan', um einen neuen Behandlungsplan anzulegen.",
                "Wochenliste generieren: Der Button 'Wochenliste anzeigen' erstellt eine Übersicht aller Behandlungen für die aktuelle Woche (Montag bis Sonntag).",
                "Behandlungsplan bearbeiten: Klicken Sie auf einen Eintrag in der Tabelle, um Details anzuzeigen und zu bearbeiten."
        }));

        section.add(createFeatureList("Hauptfunktionen - Behandlungsprüfung", new String[] {
                "Aufgabenliste anzeigen: Die Liste zeigt alle zu überprüfenden Behandlungen mit Beschreibung, Fälligkeitsdatum und Erstellungsdatum.",
                "Aufgabe öffnen: Doppelklicken Sie auf eine Aufgabe, um den Prüfdialog zu öffnen und die Behandlung zu überprüfen.",
                "Filter anwenden: Verwenden Sie das Filterfeld, um Aufgaben nach Beschreibung zu filtern.",
                "Abgeschlossene Aufgaben: Mit dem Button 'Abgeschlossene ein-/ausblenden' können Sie abgeschlossene Aufgaben ein- oder ausblenden.",
                "Aufgabenliste aktualisieren: Der Button 'Aktualisieren' erstellt neue tägliche Aufgaben und aktualisiert die Liste."
        }));

        section.add(createInfoCard("Berechtigungen",
                "Alle Benutzer können Behandlungspläne einsehen. Um Termine zu buchen oder zu löschen, benötigen Sie die Rolle ADMIN, DOCTOR oder TECH_USER. "
                + "Die Behandlungsprüfung ist für ADMIN, DOCTOR und OWNER verfügbar."));

        section.add(createInfoCard("Tipps",
                "• Verwenden Sie die Suchfunktion, um schnell Patienten zu finden\n"
                + "• Die Wochenliste hilft bei der Planung der OP-Slots\n"
                + "• Behandlungspläne können jederzeit aktualisiert werden\n"
                + "• Überprüfen Sie regelmäßig die Aufgabenliste im Tab 'Behandlungsprüfung'\n"
                + "• Überfällige Aufgaben werden rot markiert"));

        return section;
    }
}

