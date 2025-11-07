package de.bbajor.pvs.base.ui.view.help;

import com.vaadin.flow.component.html.Section;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.annotation.security.PermitAll;

@Route("help/augen-termine")
@PageTitle("Hilfe - Augen-Termine")
@PermitAll
public class OphthalmologyHelpView extends HelpSubPageView {

    public OphthalmologyHelpView() {
        super("Augen-Termine", "help/augen-termine");
    }

    @Override
    protected VaadinIcon getIcon() {
        return VaadinIcon.EYE;
    }

    @Override
    protected Section createMainSection() {
        Section section = new Section();
        section.addClassNames(LumoUtility.Margin.Bottom.LARGE);

        section.add(createInfoCard("Übersicht",
                "Die Augen-Termine ermöglichen es Ihnen, augenheilkundliche Patiententermine zu dokumentieren. "
                + "Hier können Sie Anamnese, Befunde und weitere Details erfassen."));

        section.add(createInfoCard("Funktionen",
                "• Patient auswählen\n"
                + "• Anamnese dokumentieren\n"
                + "• Augenvordergrund-Befunde erfassen\n"
                + "• Augenhintergrund-Befunde dokumentieren\n"
                + "• Zusätzliche Hinweise und Notizen"));

        section.add(createFeatureList("Hauptfunktionen", new String[] {
                "Patient auswählen: Wählen Sie einen Patienten aus der Dropdown-Liste aus.",
                "Anamnese: Dokumentieren Sie die allgemeine Anamnese des Patienten im ersten Tab.",
                "Augenvordergrund: Erfassen Sie Befunde wie Visus, Augeninnendruck, Hornhaut, Linse etc.",
                "Augenhintergrund: Dokumentieren Sie Befunde des Augenhintergrunds (Glaskörper, Papille, Makula, Gefäße, Peripherie).",
                "Weitere Details: Fügen Sie zusätzliche Hinweise und Notizen hinzu."
        }));

        section.add(createInfoCard("Befunderfassung",
                "Die Befunderfassung ist in mehrere Bereiche unterteilt:\n"
                + "• Anamnese: Allgemeine Krankengeschichte\n"
                + "• Augenvordergrund: Visus, IOP, Hornhaut, Linse etc.\n"
                + "• Augenhintergrund: Glaskörper, Netzhaut, Makula, Gefäße\n"
                + "• Weitere Details: Zusätzliche Informationen"));

        section.add(createInfoCard("Tipps",
                "• Wählen Sie zuerst den Patienten aus\n"
                + "• Dokumentieren Sie Befunde vollständig und präzise\n"
                + "• Nutzen Sie die Tabs für eine strukturierte Dokumentation"));

        return section;
    }
}

