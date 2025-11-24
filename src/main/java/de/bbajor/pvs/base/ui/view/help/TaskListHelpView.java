package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/aufgabenliste")
@PageTitle("Hilfe - Aufgabenliste")
@PermitAll
public class TaskListHelpView extends HelpSubPageView {

    public TaskListHelpView() {
        super("Zu überprüfende Behandlungen", "help/aufgabenliste");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.CLIPBOARD_CHECK;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die Aufgabenliste zeigt alle zurückliegenden Behandlungen, die noch überprüft werden müssen. "
                + "Hier können Sie Behandlungen genehmigen, ablehnen oder weitere Informationen hinzufügen."));

        section.add(createInfoCard("Funktionen",
                "• Übersicht über zu überprüfende Behandlungen\n"
                + "• Behandlungen genehmigen oder ablehnen\n"
                + "• Tägliche Aufgaben automatisch generieren\n"
                + "• Filterung nach Status und Beschreibung\n"
                + "• Behandlungsergebnisse dokumentieren"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Aufgabenliste aktualisieren: Klicken Sie auf 'Aktualisieren', um neue Aufgaben für zurückliegende Behandlungen zu generieren.",
                "Aufgabe bearbeiten: Doppelklicken Sie auf eine Aufgabe, um Details anzuzeigen und die Behandlung zu überprüfen.",
                "Filter anwenden: Verwenden Sie das Filterfeld, um nach bestimmten Beschreibungen zu suchen.",
                "Abgeschlossene Aufgaben: Verwenden Sie den Toggle-Button, um abgeschlossene Aufgaben ein- oder auszublenden.",
                "Fälligkeitsdatum: Jede Aufgabe kann ein Fälligkeitsdatum haben, um Prioritäten zu setzen."
        }));

        section.add(createInfoCard("Berechtigungen",
                "Nur Benutzer mit den Rollen ADMIN, DOCTOR oder OWNER können die Aufgabenliste einsehen. "
                + "ADMIN und DOCTOR können zusätzliche Informationen zu Behandlungen speichern. "
                + "Behandlungen genehmigen können nur DOCTOR, MEDICAL_STAFF und OWNER."));

        section.add(createInfoCard("Automatische Generierung",
                "Das System generiert automatisch Aufgaben für zurückliegende Behandlungen, die noch nicht überprüft wurden. "
                + "Dies erfolgt täglich und kann auch manuell über den 'Aktualisieren'-Button ausgelöst werden."));

        section.add(createInfoCard("Tipps",
                "• Überprüfen Sie die Aufgabenliste täglich\n"
                + "• Verwenden Sie Filter, um sich auf bestimmte Aufgaben zu konzentrieren\n"
                + "• Dokumentieren Sie Behandlungsergebnisse vollständig"));

        return section;
    }
}

