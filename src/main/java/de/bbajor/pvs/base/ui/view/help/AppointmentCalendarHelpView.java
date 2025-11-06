package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/appointment-calendar")
@PageTitle("Hilfe - Terminkalender")
@PermitAll
public class AppointmentCalendarHelpView extends HelpSubPageView {

    public AppointmentCalendarHelpView() {
        super("Terminkalender", "help/appointment-calendar");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.CALENDAR;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Der Terminkalender ermöglicht es Ihnen, Termine zu verwalten, zu buchen und zu organisieren. "
                + "Sie können zwischen verschiedenen Terminplanern wechseln und freie Termine finden."));

        section.add(createInfoCard("Funktionen",
                "• Termine buchen und verwalten\n"
                + "• Tagesansicht mit Zeitraster\n"
                + "• Wechsel zwischen verschiedenen Terminplanern\n"
                + "• Automatische Suche nach nächstem freien Termin\n"
                + "• Sprechzeitenverwaltung"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Neuen Termin buchen: Klicken Sie auf 'Neuer Termin', um einen neuen Termin anzulegen.",
                "Datum auswählen: Verwenden Sie die Datumsnavigation oder den DatePicker, um ein bestimmtes Datum auszuwählen.",
                "Terminplaner wechseln: Verwenden Sie den SchedulerSwitcher, um zwischen verschiedenen Ärzten/Planern zu wechseln.",
                "Nächster freier Termin: Der Button 'Nächster freier Termin (alle Ärzte)' findet automatisch den nächsten verfügbaren Slot.",
                "Termin bearbeiten: Klicken Sie auf einen Termin im Kalender, um Details anzuzeigen und zu bearbeiten."
        }));

        section.add(createInfoCard("Kalenderansicht",
                "Der Kalender zeigt:\n"
                + "• Sprechzeiten als grau hinterlegte Bereiche\n"
                + "• Gebuchte Termine als farbige Blöcke\n"
                + "• Zeitraster in 15-Minuten-Intervallen\n"
                + "• Patientennamen und Termingrund"));

        section.add(createInfoCard("Tipps",
                "• Verwenden Sie 'Nächster freier Termin' für schnelle Buchungen\n"
                + "• Die Tagesansicht zeigt alle Termine eines Tages auf einen Blick\n"
                + "• Sprechzeiten müssen vorher in den Einstellungen konfiguriert werden"));

        return section;
    }
}

