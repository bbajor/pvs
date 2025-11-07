package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/ivom-drugs")
@PageTitle("Hilfe - Medikamentendatenbank")
@PermitAll
public class MedicationHelpView extends HelpSubPageView {

    public MedicationHelpView() {
        super("Medikamentendatenbank", "help/ivom-drugs");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.PILL;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die Medikamentendatenbank verwaltet alle intravitrealen Medikamente. "
                + "Hier können Sie Medikamente importieren, durchsuchen und als Favoriten markieren."));

        section.add(createInfoCard("Funktionen",
                "• Medikamente importieren (CSV-Format)\n"
                + "• Hierarchische Darstellung nach Bezeichnung und Wirkstoff\n"
                + "• Suche und Filterung\n"
                + "• Favoriten markieren\n"
                + "• Medikamentendetails anzeigen und bearbeiten"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "CSV-Import: Laden Sie Medikamentendaten als CSV-Datei vom DIMDI-Portal hoch.",
                "Hierarchische Ansicht: Medikamente sind nach Arzneimittelbezeichnung und Wirkstoff gruppiert.",
                "Suche: Verwenden Sie das Filterfeld, um nach Medikamenten zu suchen.",
                "Favoriten: Markieren Sie häufig verwendete Medikamente als Favoriten für schnellen Zugriff.",
                "Details anzeigen: Doppelklicken Sie auf ein Medikament, um alle Details anzuzeigen und zu bearbeiten."
        }));

        section.add(createInfoCard("CSV-Import",
                "Medikamente können vom DIMDI Arzneimittel-Informationssystem importiert werden:\n"
                + "1. Laden Sie die CSV-Datei vom DIMDI-Portal herunter\n"
                + "2. Verwenden Sie den Upload-Button in der Medikamentendatenbank\n"
                + "3. Das System importiert automatisch neue Medikamente\n\n"
                + "Hinweis: Bereits vorhandene Medikamente werden nicht dupliziert."));

        section.add(createInfoCard("Berechtigungen",
                "Nur Benutzer mit den Rollen ADMIN, TECH_USER oder OWNER können Medikamente importieren und bearbeiten. "
                + "Alle anderen Benutzer haben Lesezugriff."));

        section.add(createInfoCard("Tipps",
                "• Importieren Sie regelmäßig aktualisierte Daten vom DIMDI-Portal\n"
                + "• Markieren Sie häufig verwendete Medikamente als Favoriten\n"
                + "• Die hierarchische Ansicht erleichtert die Navigation"));

        return section;
    }
}

