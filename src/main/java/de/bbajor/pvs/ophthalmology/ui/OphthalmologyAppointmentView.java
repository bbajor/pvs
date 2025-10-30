package de.bbajor.pvs.ophthalmology.ui;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.base.ui.component.ViewToolbar;
import de.bbajor.pvs.ophthalmology.presenter.OphthalmologyAppointmentPresenter;
import de.bbajor.pvs.patient.model.Patient;
import jakarta.annotation.security.PermitAll;

@Route("augen-termine")
@PageTitle("Augenheilkunde - Patiententermine")
@Menu(order = 5, icon = "vaadin:eye", title = "Augen-Termine")
@PermitAll
public class OphthalmologyAppointmentView extends Main {

    private final OphthalmologyAppointmentPresenter presenter;

    private final TabSheet tabSheet = new TabSheet();
    private final ComboBox<Patient> patientSelect = new ComboBox<>("Patient");

    public OphthalmologyAppointmentView(OphthalmologyAppointmentPresenter presenter) {
        this.presenter = presenter;

        setSizeFull();
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        patientSelect.setItems(presenter.getPatients());
        patientSelect.setItemLabelGenerator(p -> p.getLastName() + ", " + p.getFirstName());
        add(new ViewToolbar("Augenheilkunde – Patiententermine", ViewToolbar.group(patientSelect)));

        tabSheet.setSizeFull();

        tabSheet.add("Anamnese", createAnamnesisLayout());
        tabSheet.add("Augenvordergrund", createAnteriorSegmentLayout());
        tabSheet.add("Augenhintergrund", createPosteriorSegmentLayout());
        tabSheet.add("Weitere Details", createOtherDetailsLayout());

        add(tabSheet);
    }

    private VerticalLayout createAnamnesisLayout() {
        TextArea anamnesisText = new TextArea("Allgemeine Anamnese");
        anamnesisText.setWidthFull();
        anamnesisText.setMinHeight("150px");

        FormLayout formLayout = new FormLayout();
        formLayout.add(anamnesisText, 2);
        AccordionPanel panel = new AccordionPanel("Anamnese", formLayout);
        panel.setOpened(true);
        Accordion accordion = new Accordion();
        accordion.add(panel);
        VerticalLayout layout = new VerticalLayout(accordion);
        layout.setSizeFull();
        layout.setPadding(false);
        return layout;
    }

    private VerticalLayout createAnteriorSegmentLayout() {
        // Simple first version of anterior segment findings
        TextField visualAcuityDistance = new TextField("Fernvisus (dezimal)");
        TextField visualAcuityNear = new TextField("Nahvisus");
        TextField iop = new TextField("Augeninnendruck (mmHg)");
        TextField lids = new TextField("Lider / Bindehaut");
        TextField cornea = new TextField("Hornhaut");
        TextField anteriorChamber = new TextField("Vordere Augenkammer");
        TextField irisPupil = new TextField("Iris / Pupille");
        TextField lens = new TextField("Linse");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setMinColumns(3);
        formLayout.add(visualAcuityDistance);
        formLayout.add(visualAcuityNear);
        formLayout.add(iop);
        formLayout.add(lids);
        formLayout.add(cornea);
        formLayout.add(anteriorChamber);
        formLayout.add(irisPupil);
        formLayout.add(lens);

        AccordionPanel panel = new AccordionPanel("Augenvordergrund", formLayout);
        panel.setOpened(true);
        Accordion accordion = new Accordion();
        accordion.add(panel);
        VerticalLayout layout = new VerticalLayout(accordion);
        layout.setSizeFull();
        layout.setPadding(false);
        return layout;
    }

    private VerticalLayout createPosteriorSegmentLayout() {
        TextField vitreous = new TextField("Glaskörper");
        TextField opticDisc = new TextField("Papille");
        TextField macula = new TextField("Makula");
        TextField vessels = new TextField("Gefäße");
        TextField periphery = new TextField("Peripherie");

        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setMinColumns(3);
        formLayout.add(vitreous);
        formLayout.add(opticDisc);
        formLayout.add(macula);
        formLayout.add(vessels);
        formLayout.add(periphery);

        AccordionPanel panel = new AccordionPanel("Augenhintergrund", formLayout);
        panel.setOpened(true);
        Accordion accordion = new Accordion();
        accordion.add(panel);
        VerticalLayout layout = new VerticalLayout(accordion);
        layout.setSizeFull();
        layout.setPadding(false);
        return layout;
    }

    private VerticalLayout createOtherDetailsLayout() {
        TextArea notes = new TextArea("Zusätzliche Hinweise");
        notes.setWidthFull();
        notes.setMinHeight("150px");

        FormLayout formLayout = new FormLayout();
        formLayout.add(notes, 2);

        AccordionPanel panel = new AccordionPanel("Weitere Details", formLayout);
        panel.setOpened(true);
        Accordion accordion = new Accordion();
        accordion.add(panel);
        VerticalLayout layout = new VerticalLayout(accordion);
        layout.setSizeFull();
        layout.setPadding(false);
        return layout;
    }
}
