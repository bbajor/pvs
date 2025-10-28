package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;

import de.bbajor.pvs.base.ui.component.TimeLineCardConfig;
import de.bbajor.pvs.base.ui.component.TimelineView;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.Diagnosis;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;

public class TreatmentPlanLayout extends VerticalLayout {

    private Binder<TreatmentPlan> binder = new Binder<>(TreatmentPlan.class);

    // Allgemeines
    private final DatePicker creationDatePicker = new DatePicker("Erstellt am");
    private final ComboBox<Patient> patientSelectComboBox = new ComboBox<>("Patient");
    private final ComboBox<Diagnosis> reasonForTreatmentComboBox = new ComboBox<>("Behandlungsgrund");

    // Filter
    private final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    private final ComboBox<Medication> medicationComboBox = new ComboBox<>("Medikament");
    private final DatePicker startDatePicker = new DatePicker("Neue Termine finden ab");
    private final ComboBox<TimeSlotRepetition> repetitionComboBox = new ComboBox<>("Terminintervall");
    private final ComboBox<TimePeriod> timePeriodComboBox = new ComboBox<>("Termine erstellen für");
    private final ComboBox<SurgicalCenter> surgicalCenterComboBox = new ComboBox<>(
            "Bevorzugten Behandlungsort auswählen");
    private final Button filterTimeSlotsButton = new Button("Verfügbare Termine anzeigen");
    private final Grid<SurgicalCenterTimeSlot> timeSlotGrid = new Grid<>();
    private final TimelineView timeLineViewLeftEye;
    private final TimelineView timeLineViewRightEye;
    private final TextArea additionalInformation = new TextArea("Notizen");
    private final TabSheet tabSheet = new TabSheet();
    private final TreatmentPlanPresenter presenter;
    private TreatmentPlan current;

    public TreatmentPlanLayout(TreatmentPlanPresenter presenter, TreatmentPlan treatmentPlan,
            ApplicationContext context) {
        this.presenter = presenter;
        this.current = treatmentPlan;

        setSizeFull();
        // Verhindere Scrollen auf diesem Layout-Level
        getStyle().set("overflow", "hidden");
        
        add(tabSheet);
        expand(tabSheet);

        timeLineViewLeftEye = new TimelineView(context);
        timeLineViewRightEye = new TimelineView(context);

        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new TreatmentPlan());
                }
                binder.getBean().setPatient(event.getValue());
            }
        });
        medicationComboBox.setItems(presenter.getDrugs());

        sideOfEye.setItems(SideOfEye.values());
        timeSlotGrid.setSizeFull();
        timeSlotGrid.setMinHeight("500px");
        timeSlotGrid.addColumn(SurgicalCenterTimeSlot::getSurgicalCenter).setHeader("Einrichtung");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        timeSlotGrid.addColumn(dto -> dto.getDate().format(formatter))
                .setHeader("Datum");
        timeSlotGrid.setSelectionMode(SelectionMode.MULTI);

        surgicalCenterComboBox.setItems(presenter.getSurgicalCenters());
        surgicalCenterComboBox.setClearButtonVisible(true);

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDatePicker.setEnabled(false);

        reasonForTreatmentComboBox.setItemLabelGenerator(Diagnosis::getName);
        reasonForTreatmentComboBox.setAllowCustomValue(true);
        reasonForTreatmentComboBox.setClearButtonVisible(true);
        reasonForTreatmentComboBox.addCustomValueSetListener(event -> {
            String newValue = event.getDetail();
            // Optional: trim & prüfen
            if (newValue != null && !newValue.trim().isEmpty()) {
                // Neues entity
                Diagnosis newDiagnosis = new Diagnosis();
                newDiagnosis.setName(newValue.trim());

                // In DB speichern, falls nötig
                Diagnosis saved = presenter.saveDiagnosis(newDiagnosis);

                // ComboBox aktualisieren
                List<Diagnosis> items = new ArrayList<>(
                        reasonForTreatmentComboBox.getDataProvider().fetch(new Query<>()).toList());
                items.add(saved);
                reasonForTreatmentComboBox.setItems(items);

                // Setze das neue Entity als ausgewählt
                reasonForTreatmentComboBox.setValue(saved);
            }
        });
        reasonForTreatmentComboBox.setItems(presenter.getResaonsForTreatment());

        initializeGeneralDetailsTab();
        initializeTreatmentAppointmentOverviewTab();
        initializeAppointmentTab();

        initializeBinder(treatmentPlan);
    }

    private void initializeTreatmentAppointmentOverviewTab() {
        VerticalLayout timeLineLayout = new VerticalLayout();
        timeLineLayout.setSizeFull();
        timeLineLayout.setPadding(false);
        timeLineLayout.setSpacing(false);
        timeLineLayout.getStyle().set("overflow", "hidden"); // Prevent scroll on tab sheet
        
        // Orientation toggle controls both timelines
        RadioButtonGroup<TimelineView.Orientation> orientationToggle = new RadioButtonGroup<>();
        orientationToggle.setLabel("Ausrichtung");
        orientationToggle.setItems(TimelineView.Orientation.HORIZONTAL, TimelineView.Orientation.VERTICAL);
        orientationToggle.setValue(TimelineView.Orientation.HORIZONTAL);
        orientationToggle.setItemLabelGenerator(item ->
                item == TimelineView.Orientation.HORIZONTAL ? "Horizontal" : "Vertikal");
        orientationToggle.addValueChangeListener(e -> {
            TimelineView.Orientation o = e.getValue();
            timeLineViewLeftEye.setOrientation(o);
            timeLineViewRightEye.setOrientation(o);
            
            // Update layout based on orientation
            updateTimelineLayout(timeLineLayout, o);
        });
        timeLineLayout.add(orientationToggle);
        timeLineViewLeftEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        timeLineViewRightEye.setOrientation(TimelineView.Orientation.HORIZONTAL);
        
        // Initial setup
        updateTimelineLayout(timeLineLayout, TimelineView.Orientation.HORIZONTAL);
        
        tabSheet.add("Behandlungsübersicht", timeLineLayout);
    }
    
    private void updateTimelineLayout(VerticalLayout timeLineLayout, TimelineView.Orientation orientation) {
        // Remove existing timeline accordions
        timeLineLayout.getChildren()
                .filter(child -> child.getClass().getSimpleName().equals("Accordion"))
                .forEach(timeLineLayout::remove);
        
        // Check if treatments exist for each eye
        boolean hasRightEye = current != null && hasTreatmentsForEye(SideOfEye.RIGHT);
        boolean hasLeftEye = current != null && hasTreatmentsForEye(SideOfEye.LEFT);
        
        if (!hasRightEye && !hasLeftEye) {
            // No treatments for either eye
            return;
        }
        
        if (orientation == TimelineView.Orientation.VERTICAL) {
            // Vertical: show eyes side by side
            VerticalLayout eyesContainer = new VerticalLayout();
            eyesContainer.setWidthFull();
            eyesContainer.setPadding(false);
            eyesContainer.setSpacing(false);
            eyesContainer.getStyle().set("display", "flex");
            eyesContainer.getStyle().set("flex-direction", "row");
            
            if (hasRightEye) {
                initializeTimeLineRightEye(eyesContainer); // OD (rechts vom Patienten = links in UI)
            }
            if (hasLeftEye) {
                initializeTimeLineLeftEye(eyesContainer);  // OS (links vom Patienten = rechts in UI)
            }
            
            timeLineLayout.add(eyesContainer);
        } else {
            // Horizontal: show vertically stacked
            if (hasRightEye) {
                initializeTimeLineRightEye(timeLineLayout); // OD (rechts vom Patienten = links in UI)
            }
            if (hasLeftEye) {
                initializeTimeLineLeftEye(timeLineLayout);  // OS (links vom Patienten = rechts in UI)
            }
        }
    }
    
    private boolean hasTreatmentsForEye(SideOfEye side) {
        if (current == null || current.getId() == null) {
            return false;
        }
        List<Treatment> treatments = presenter.getTreatmentDtos(side, current.getId());
        return treatments != null && !treatments.isEmpty();
    }

    private void initializeAppointmentTab() {
        VerticalLayout appointmentLayout = new VerticalLayout();
        appointmentLayout.setSizeFull();
        initializeTimeSlotFilter(appointmentLayout);
        appointmentLayout.add(filterTimeSlotsButton);
        timeSlotGrid.setSizeFull();
        appointmentLayout.add(timeSlotGrid);
        tabSheet.add("Behandlungen planen", appointmentLayout);
    }

    private void initializeBinder(TreatmentPlan dto) {
        binder.bind(creationDatePicker, TreatmentPlan::getCreationDate,
                TreatmentPlan::setCreationDate);
        binder.bind(additionalInformation, TreatmentPlan::getAdditionalInformation,
                TreatmentPlan::setAdditionalInformation);
        binder.bind(patientSelectComboBox, TreatmentPlan::getPatient, TreatmentPlan::setPatient);
        binder.bind(reasonForTreatmentComboBox, TreatmentPlan::getDiagnosis,
                TreatmentPlan::setDiagnosis);
        binder.setBean(dto == null ? new TreatmentPlan() : dto);
    }

    private void initializeTimeSlotFilter(VerticalLayout verticalLayout) {
        FormLayout formLayout = new FormLayout();
        formLayout.add(sideOfEye);
        formLayout.add(medicationComboBox);
        formLayout.add(surgicalCenterComboBox);
        startDatePicker.setValue(LocalDate.now());
        formLayout.add(startDatePicker);
        timePeriodComboBox.setItems(TimePeriod.values());
        timePeriodComboBox.setValue(TimePeriod.THREE_MONTHS);
        formLayout.add(timePeriodComboBox);
        repetitionComboBox.setItems(TimeSlotRepetition.values());
        repetitionComboBox.setValue(TimeSlotRepetition.EVERY_FOUR_WEEKS);
        formLayout.add(repetitionComboBox);
        formLayout.add(filterTimeSlotsButton);

        filterTimeSlotsButton.addClickListener(click -> {
            SurgicalCenter selectedCenter = surgicalCenterComboBox.getValue();
            Integer id = selectedCenter == null ? null : selectedCenter.getId();
            Collection<SurgicalCenterTimeSlot> availableAndFilteredSlots = presenter.getAllTimeSlotsFilteredBy(
                    startDatePicker.getValue(), timePeriodComboBox.getValue(), repetitionComboBox.getValue(),
                    id);
            timeSlotGrid.setItems(availableAndFilteredSlots);
            timeSlotGrid.setItems(availableAndFilteredSlots);
        });

        verticalLayout.add(formLayout);
    }

    private void initializeTimeLineRightEye(VerticalLayout timeLineLayout) {
        // rechtes Auge mit medizinisch korrekter Darstellung (rechts = links vom Patienten)
        timeLineViewRightEye.setTimelineHeight("300px");
        timeLineViewRightEye.addClassName("right-eye-timeline");
        timeLineViewRightEye.getStyle().set("background-color", "#E3F2FD"); // Blue tint
        Accordion accordionRight = new Accordion();
        accordionRight.setWidthFull();
        accordionRight.getStyle().set("overflow", "hidden"); // Prevent scroll on accordion level
        AccordionPanel accordionPanelRight = accordionRight.add("Behandlungsverlauf rechtes Auge (OD)", timeLineViewRightEye);
        accordionPanelRight.setOpened(true);
        accordionPanelRight.getElement().getStyle().set("width", "100%");
        accordionPanelRight.getElement().getStyle().set("overflow", "hidden"); // No scroll on panel, only in TimelineView
        timeLineLayout.add(accordionRight);
        setRightEyeTreatmentHistory(current == null ? null : current.getId());
    }

    private void setRightEyeTreatmentHistory(Long treatmentPlanId) {
        List<TimeLineCardConfig> rightEyeTreatments = new ArrayList<>();
        if (treatmentPlanId != null) {
            List<Treatment> treatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, treatmentPlanId);
            for (Treatment treatment : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatment(treatment);
                rightEyeTreatments.add(config);
            }
        }
        // Auch bei null (neuer Plan) initialisieren - zeigt dann wenigstens Start-Marker
        timeLineViewRightEye.setStartOfTreatmentPlan(
                current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now());
        timeLineViewRightEye.setItems(rightEyeTreatments);
    }

    private void initializeGeneralDetailsTab() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(creationDatePicker);
        formLayout.add(patientSelectComboBox);
        formLayout.add(reasonForTreatmentComboBox);
        formLayout.add(additionalInformation, 2);
        AccordionPanel generalDetailsPanel = new AccordionPanel();
        Accordion generalDetails = new Accordion();
        generalDetailsPanel.add(formLayout);
        generalDetailsPanel.setOpened(true);
        generalDetails.add(generalDetailsPanel);
        tabSheet.add("Allgemeine Informationen", generalDetails);
    }

    private void initializeTimeLineLeftEye(VerticalLayout timeLineLayout) {
        // linkes Auge mit medizinisch korrekter Darstellung (links = rechts vom Patienten)
        timeLineViewLeftEye.setTimelineHeight("300px");
        timeLineViewLeftEye.addClassName("left-eye-timeline");
        timeLineViewLeftEye.getStyle().set("background-color", "#FFF3E0"); // Orange tint
        Accordion accordionLeft = new Accordion();
        accordionLeft.setWidthFull();
        accordionLeft.getStyle().set("overflow", "hidden"); // Prevent scroll on accordion level
        AccordionPanel accordionPanelLeft = accordionLeft.add("Behandlungsverlauf linkes Auge (OS)", timeLineViewLeftEye);
        accordionPanelLeft.setOpened(true);
        accordionPanelLeft.getElement().getStyle().set("width", "100%");
        accordionPanelLeft.getElement().getStyle().set("overflow", "hidden"); // No scroll on panel, only in TimelineView
        timeLineLayout.add(accordionLeft);
        setLeftEyeTreatmentHistory(current == null ? null : current.getId());
    }

    private void setLeftEyeTreatmentHistory(Long treatmentPlanId) {
        List<TimeLineCardConfig> leftEyeTreatments = new ArrayList<>();
        if (treatmentPlanId != null) {
            List<Treatment> treatments = presenter.getTreatmentDtos(SideOfEye.LEFT, treatmentPlanId);
            for (Treatment treatment : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatment(treatment);
                leftEyeTreatments.add(config);
            }
        }
        // Auch bei null (neuer Plan) initialisieren - zeigt dann wenigstens Start-Marker
        timeLineViewLeftEye.setStartOfTreatmentPlan(
                current != null && current.getCreationDate() != null 
                        ? current.getCreationDate() 
                        : LocalDate.now());
        timeLineViewLeftEye.setItems(leftEyeTreatments);
    }

    public TreatmentPlan getCurrent() {
        TreatmentPlan dto = binder.getBean();
        dto.getTreatments().addAll(getTimeSlotsToCreate());
        dto.setPatient(patientSelectComboBox.getValue());
        return dto;
    }

    public List<Treatment> getTimeSlotsToCreate() {
        List<Treatment> timeSlotsToCreate = new ArrayList<>();
        Set<SurgicalCenterTimeSlot> selectedSlots = timeSlotGrid.getSelectedItems();
        for (SurgicalCenterTimeSlot timeSlot : selectedSlots) {
            Treatment timeSlotToCreate = new Treatment();
            timeSlotToCreate.setSideOfEye(sideOfEye.getValue());
            timeSlotToCreate.setMedication(medicationComboBox.getValue());
            timeSlotToCreate.setSurgicalCenterTimeSlot(timeSlot);
            timeSlotToCreate.setTreatmentPlan(current);
            timeSlotsToCreate.add(timeSlotToCreate);
        }
        return timeSlotsToCreate;
    }

    public void setCurrent(TreatmentPlan newCurrent) {
        this.current = newCurrent;
        binder.setBean(newCurrent);
        if (newCurrent.getPatient() != null) {
            patientSelectComboBox.setReadOnly(true);
        }
        setLeftEyeTreatmentHistory(newCurrent.getId());
        setRightEyeTreatmentHistory(newCurrent.getId());
        
        // Refresh timeline display if orientation toggle exists
        // Note: This will be called after the layout is already built, so we need to update it
        // The accordions will be re-added by updateTimelineLayout if needed
    }
}
