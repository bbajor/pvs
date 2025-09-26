package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;

import de.bbajor.pvs.base.dto.SideOfEye;
import de.bbajor.pvs.base.dto.TimePeriod;
import de.bbajor.pvs.base.dto.TimeSlotRepetition;
import de.bbajor.pvs.base.ui.component.TimeLineCardConfig;
import de.bbajor.pvs.base.ui.component.TimelineView;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.IntravitrealTreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentSlotDto;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;

public class TreatmentPlanLayout extends VerticalLayout {

    private Binder<IntravitrealTreatmentDto> binder = new Binder<>(IntravitrealTreatmentDto.class);

    // Allgemeines
    private final DatePicker creationDate = new DatePicker("Erstellt am");
    private final ComboBox<PatientDto> patientSelectComboBox = new ComboBox<>("Patient");
    private final ComboBox<DiagnosisDto> reasonForTreatmentComboBox = new ComboBox<>("Behandlungsgrund");

    // Filter
    private final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    private final ComboBox<IntravitrealMedicationDto> medicationComboBox = new ComboBox<>("Medikament");
    private final DatePicker startDatePicker = new DatePicker("Neue Termine finden ab");
    private final ComboBox<TimeSlotRepetition> repetitionComboBox = new ComboBox<>("Terminintervall");
    private final ComboBox<TimePeriod> timePeriodComboBox = new ComboBox<>("Termine erstellen für");
    private final ComboBox<SurgicalCenterDto> surgicalCenterComboBox = new ComboBox<>(
            "Bevorzugten Behandlungsort auswählen");
    private final Button filterTimeSlotsButton = new Button("Verfügbare Termine anzeigen");
    private final Grid<SurgicalCenterTimeSlotDto> timeSlotGrid = new Grid<>();

    // Behandlungsverlauf linkes Auge
    private final TimelineView timeLineViewLeftEye = new TimelineView();

    // Behandlungsverlauf linkes Auge
    private final TimelineView timeLineViewRightEye = new TimelineView();

    // Notizen
    private final TextArea additionalInformation = new TextArea("Notizen");

    private final TreatmentPlanPresenter presenter;

    public TreatmentPlanLayout(TreatmentPlanPresenter presenter) {
        this.presenter = presenter;

        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new IntravitrealTreatmentDto());
                }
                binder.getBean().setPatient(event.getValue());
            }
        });
        medicationComboBox.setItems(presenter.getDrugs());

        sideOfEye.setItems(SideOfEye.values());
        timeSlotGrid.setSizeFull();
        timeSlotGrid.addColumn(SurgicalCenterTimeSlotDto::getSurgicalCenter).setHeader("Einrichtung");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        timeSlotGrid.addColumn(dto -> dto.getDate().format(formatter))
                .setHeader("Datum");
        timeSlotGrid.setSelectionMode(SelectionMode.MULTI);

        surgicalCenterComboBox.setItems(presenter.getSurgicalCenterList());
        surgicalCenterComboBox.setClearButtonVisible(true);

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDate.setEnabled(false);

        reasonForTreatmentComboBox.setItemLabelGenerator(DiagnosisDto::getName);
        reasonForTreatmentComboBox.setAllowCustomValue(true);
        reasonForTreatmentComboBox.setClearButtonVisible(true);
        reasonForTreatmentComboBox.addCustomValueSetListener(event -> {
            String newValue = event.getDetail();
            // Optional: trim & prüfen
            if (newValue != null && !newValue.trim().isEmpty()) {
                // Neues Dto
                DiagnosisDto newDto = new DiagnosisDto();
                newDto.setName(newValue.trim());

                // In DB speichern, falls nötig
                DiagnosisDto saved = presenter.saveDiagnosis(newDto);

                // ComboBox aktualisieren
                List<DiagnosisDto> items = new ArrayList<>(
                        reasonForTreatmentComboBox.getDataProvider().fetch(new Query<>()).toList());
                items.add(saved);
                reasonForTreatmentComboBox.setItems(items);

                // Setze das neue Entity als ausgewählt
                reasonForTreatmentComboBox.setValue(newDto);
            }
        });
        reasonForTreatmentComboBox.setItems(presenter.getDiseases());

        initializeGeneralDetails();

        initializeTimeSlotFilter();

        add(filterTimeSlotsButton);
        add(timeSlotGrid);

        initializeTimeLineLeftEye();
        initializeTimeLineRightEye();

        // direkt hier einfügen
        add(additionalInformation);
    }

    private void initializeTimeSlotFilter() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(sideOfEye);
        formLayout.add(medicationComboBox);
        formLayout.add(surgicalCenterComboBox);
        startDatePicker.setValue(LocalDate.now());
        formLayout.add(startDatePicker);
        formLayout.add(timePeriodComboBox);
        formLayout.add(repetitionComboBox);
        formLayout.add(filterTimeSlotsButton);

        filterTimeSlotsButton.addClickListener(click -> {
            SurgicalCenterDto selectedCenter = surgicalCenterComboBox.getValue();
            Integer id = selectedCenter == null ? null : selectedCenter.getId();
            Collection<SurgicalCenterTimeSlotDto> availableAndFilteredSlots = presenter.getAllTimeSlotsFilteredBy(
                    startDatePicker.getValue(), timePeriodComboBox.getValue(), repetitionComboBox.getValue(),
                    id);
            timeSlotGrid.setItems(availableAndFilteredSlots);
            timeSlotGrid.setItems(availableAndFilteredSlots);
        });

        add(formLayout);
    }

    private void initializeTimeLineRightEye() {
        // rechtes Auge
        Scroller scrollerRight = new Scroller(timeLineViewRightEye);
        scrollerRight.setScrollDirection(ScrollDirection.HORIZONTAL);
        scrollerRight.setWidthFull();
        scrollerRight.setHeight("300px");
        Accordion accordionRight = new Accordion();
        accordionRight.setWidthFull();
        AccordionPanel accordionPanelRight = accordionRight.add("Behandlungsverlauf rechtes Auge", scrollerRight);
        accordionPanelRight.setOpened(true);
        accordionPanelRight.getElement().getStyle().set("width", "100%");
        add(accordionPanelRight);
        setRightEyeTreatmentHistory();
    }

    private void setRightEyeTreatmentHistory() {
        List<TimeLineCardConfig> rightEyeTreatments = new ArrayList<>();
        if (presenter.getCurrentPatient() != null) {
            List<TreatmentSlotDto> treatments = presenter.getTreatments(SideOfEye.RIGHT);
            for (TreatmentSlotDto treatmentSlotDto : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig();
                config.setTreatmenDate(treatmentSlotDto.getSurgicalCenterTimeSlot().getDate());
                config.setDescription(treatmentSlotDto.getRemarks());
                rightEyeTreatments.add(config);
            }
        }
        timeLineViewRightEye.setItems(rightEyeTreatments);
    }

    private void initializeGeneralDetails() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(creationDate);
        formLayout.add(patientSelectComboBox);
        formLayout.add(reasonForTreatmentComboBox);
        AccordionPanel generalDetailsPanel = new AccordionPanel();
        Accordion generalDetails = new Accordion();
        generalDetailsPanel.add(formLayout);
        generalDetailsPanel.setOpened(true);
        generalDetails.add(generalDetailsPanel);
        add(generalDetails);
    }

    private void initializeTimeLineLeftEye() {
        // linkes Auge
        Scroller scrollerLeft = new Scroller(timeLineViewLeftEye);
        scrollerLeft.setScrollDirection(ScrollDirection.HORIZONTAL);
        scrollerLeft.setWidthFull();
        scrollerLeft.setHeight("300px");
        Accordion accordionLeft = new Accordion();
        accordionLeft.setWidthFull();
        AccordionPanel accordionPanelLeft = accordionLeft.add("Behandlungsverlauf linkes Auge", scrollerLeft);
        accordionPanelLeft.setOpened(true);
        accordionPanelLeft.getElement().getStyle().set("width", "100%");
        add(accordionPanelLeft);
        setLeftEyeTreatmentHistory();
    }

    private void setLeftEyeTreatmentHistory() {
        List<TimeLineCardConfig> leftEyeTreatments = new ArrayList<>();
        if (presenter.getCurrentPatient() != null) {
            List<TreatmentSlotDto> treatments = presenter.getTreatments(SideOfEye.LEFT);
            for (TreatmentSlotDto treatmentSlotDto : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig();
                config.setTreatmenDate(treatmentSlotDto.getSurgicalCenterTimeSlot().getDate());
                config.setDescription(treatmentSlotDto.getRemarks());
                leftEyeTreatments.add(config);
            }
        }
        timeLineViewRightEye.setItems(leftEyeTreatments);
    }

    public void setIvom(IntravitrealTreatmentDto ivom) {
        binder.setBean(ivom);
    }

    public IntravitrealTreatmentDto geIvomDto() {
        return binder.getBean();
    }

    public List<TreatmentSlotDto> getTimeSlotsToCreate() {
        List<TreatmentSlotDto> timeSlotsToCreate = new ArrayList<>();
        Set<SurgicalCenterTimeSlotDto> selectedSlots = timeSlotGrid.getSelectedItems();
        for (SurgicalCenterTimeSlotDto surgicalCenterTimeSlotDto : selectedSlots) {
            TreatmentSlotDto timeSlotToCreate = new TreatmentSlotDto().setSideOfEye(sideOfEye.getValue().asDbString())
                    .setSurgicalCenterTimeSlot(surgicalCenterTimeSlotDto).setTreatmentPlan(presenter.getWorkingCopy());
        }
        return new ArrayList<>();
    }

    public void setBean(IntravitrealTreatmentDto newDto) {
        binder.setBean(newDto);
    }

}
