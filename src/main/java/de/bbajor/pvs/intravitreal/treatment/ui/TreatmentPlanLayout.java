package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;

import de.bbajor.pvs.base.ui.component.TimeLineCardConfig;
import de.bbajor.pvs.base.ui.component.TimelineView;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.base.util.TimePeriod;
import de.bbajor.pvs.base.util.TimeSlotRepetition;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.DiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import de.bbajor.pvs.medication.dto.MedicationDto;
import de.bbajor.pvs.patient.dto.PatientDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;

public class TreatmentPlanLayout extends VerticalLayout {

    private Binder<TreatmentPlanDto> binder = new Binder<>(TreatmentPlanDto.class);

    // Allgemeines
    private final DatePicker creationDatePicker = new DatePicker("Erstellt am");
    private final ComboBox<PatientDto> patientSelectComboBox = new ComboBox<>("Patient");
    private final ComboBox<DiagnosisDto> reasonForTreatmentComboBox = new ComboBox<>("Behandlungsgrund");

    // Filter
    private final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    private final ComboBox<MedicationDto> medicationComboBox = new ComboBox<>("Medikament");
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

    private final TabSheet tabSheet = new TabSheet();

    private final TreatmentPlanPresenter presenter;

    private TreatmentPlanDto treatmentPlanDto;

    public TreatmentPlanLayout(TreatmentPlanPresenter presenter, TreatmentPlanDto treatmentPlanDto) {
        this.presenter = presenter;
        this.treatmentPlanDto = treatmentPlanDto;

        setSizeFull();

        add(tabSheet);

        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new TreatmentPlanDto());
                }
                binder.getBean().setPatient(event.getValue());
            }
        });
        medicationComboBox.setItems(presenter.getDrugs());

        sideOfEye.setItems(SideOfEye.values());
        timeSlotGrid.setSizeFull();
        timeSlotGrid.setMinHeight("500px");
        timeSlotGrid.addColumn(SurgicalCenterTimeSlotDto::getSurgicalCenter).setHeader("Einrichtung");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        timeSlotGrid.addColumn(dto -> dto.getDate().format(formatter))
                .setHeader("Datum");
        timeSlotGrid.setSelectionMode(SelectionMode.MULTI);

        surgicalCenterComboBox.setItems(presenter.getSurgicalCenters());
        surgicalCenterComboBox.setClearButtonVisible(true);

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDatePicker.setEnabled(false);

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
                reasonForTreatmentComboBox.setValue(saved);
            }
        });
        reasonForTreatmentComboBox.setItems(presenter.getResaonsForTreatment());

        initializeGeneralDetailsTab();
        initializeTreatmentAppointmentOverviewTab();
        initializeAppointmentTab();

        initializeBinder(treatmentPlanDto);
    }

    private void initializeTreatmentAppointmentOverviewTab() {
        VerticalLayout timeLineLayout = new VerticalLayout();
        timeLineLayout.setSizeFull();
        initializeTimeLineLeftEye(timeLineLayout);
        initializeTimeLineRightEye(timeLineLayout);
        tabSheet.add("Behandlungsübersicht", timeLineLayout);
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

    private void initializeBinder(TreatmentPlanDto dto) {
        binder.bind(creationDatePicker, TreatmentPlanDto::getCreationDate,
                TreatmentPlanDto::setCreationDate);
        binder.bind(additionalInformation, TreatmentPlanDto::getAdditionalInformation,
                TreatmentPlanDto::setAdditionalInformation);
        binder.bind(patientSelectComboBox, TreatmentPlanDto::getPatient, TreatmentPlanDto::setPatient);
        binder.bind(reasonForTreatmentComboBox, TreatmentPlanDto::getDiagnosis,
                TreatmentPlanDto::setDiagnosis);
        binder.setBean(dto == null ? new TreatmentPlanDto() : dto);
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
            SurgicalCenterDto selectedCenter = surgicalCenterComboBox.getValue();
            Integer id = selectedCenter == null ? null : selectedCenter.getId();
            Collection<SurgicalCenterTimeSlotDto> availableAndFilteredSlots = presenter.getAllTimeSlotsFilteredBy(
                    startDatePicker.getValue(), timePeriodComboBox.getValue(), repetitionComboBox.getValue(),
                    id);
            timeSlotGrid.setItems(availableAndFilteredSlots);
            timeSlotGrid.setItems(availableAndFilteredSlots);
        });

        verticalLayout.add(formLayout);
    }

    private void initializeTimeLineRightEye(VerticalLayout timeLineLayout) {
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
        timeLineLayout.add(accordionPanelRight);
        setRightEyeTreatmentHistory(treatmentPlanDto == null ? null : treatmentPlanDto.getId());
    }

    private void setRightEyeTreatmentHistory(Long treatmentPlanId) {
        List<TimeLineCardConfig> rightEyeTreatments = new ArrayList<>();
        if (treatmentPlanId != null) {
            List<TreatmentDto> treatments = presenter.getTreatmentDtos(SideOfEye.RIGHT, treatmentPlanId);
            for (TreatmentDto treatmentSlotDto : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setSideOfEye(SideOfEye.byDbString(treatmentSlotDto.getSideOfEye()))
                        .setLocationInfo(treatmentSlotDto.getSurgicalCenterString())
                        .setTreatmentDate(treatmentSlotDto.getSurgicalCenterTimeSlot().getDate())
                        .setAdditionalInfo(treatmentSlotDto.getAdditionalInfo())
                        .setStartTime(treatmentSlotDto.getSurgicalCenterTimeSlot().getStartTime());
                rightEyeTreatments.add(config);
            }
        }
        timeLineViewRightEye.setStartOfTreatmentPlan(
                treatmentPlanDto != null ? treatmentPlanDto.getCreationDate() : LocalDate.now());
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
        timeLineLayout.add(accordionPanelLeft);
        setLeftEyeTreatmentHistory(treatmentPlanDto == null ? null : treatmentPlanDto.getId());
    }

    private void setLeftEyeTreatmentHistory(Long treatmentPlanId) {
        List<TimeLineCardConfig> leftEyeTreatments = new ArrayList<>();
        if (treatmentPlanId != null) {
            List<TreatmentDto> treatments = presenter.getTreatmentDtos(SideOfEye.LEFT, treatmentPlanId);
            for (TreatmentDto treatmentSlotDto : treatments) {
                TimeLineCardConfig config = new TimeLineCardConfig()
                        .setTreatmentDate(treatmentSlotDto.getSurgicalCenterTimeSlot().getDate())
                        .setAdditionalInfo(treatmentSlotDto.getAdditionalInfo())
                        .setSideOfEye(SideOfEye.byDbString(treatmentSlotDto.getSideOfEye()))
                        .setLocationInfo(treatmentSlotDto.getSurgicalCenterString())
                        .setStartTime(treatmentSlotDto.getSurgicalCenterTimeSlot().getStartTime());
                leftEyeTreatments.add(config);
            }
        }
        timeLineViewLeftEye.setStartOfTreatmentPlan(
                treatmentPlanDto != null ? treatmentPlanDto.getCreationDate() : LocalDate.now());
        timeLineViewLeftEye.setItems(leftEyeTreatments);
    }

    public TreatmentPlanDto getTreatmentPlanDto() {
        TreatmentPlanDto dto = binder.getBean();
        dto.getTreatments().addAll(getTimeSlotsToCreate());
        dto.setPatient(patientSelectComboBox.getValue());
        return dto;
    }

    public List<TreatmentDto> getTimeSlotsToCreate() {
        List<TreatmentDto> timeSlotsToCreate = new ArrayList<>();
        Set<SurgicalCenterTimeSlotDto> selectedSlots = timeSlotGrid.getSelectedItems();
        for (SurgicalCenterTimeSlotDto surgicalCenterTimeSlotDto : selectedSlots) {
            TreatmentDto timeSlotToCreate = new TreatmentDto()
                    .setSideOfEye(sideOfEye.getValue().asDbString())
                    .setMedication(medicationComboBox.getValue())
                    .setSurgicalCenterTimeSlot(surgicalCenterTimeSlotDto)
                    .setTreatmentPlan(treatmentPlanDto);
            timeSlotsToCreate.add(timeSlotToCreate);
        }
        return timeSlotsToCreate;
    }

    public void setTreatmentPlan(TreatmentPlanDto dto) {
        this.treatmentPlanDto = dto;
        binder.setBean(treatmentPlanDto);
        if (treatmentPlanDto.getPatient() != null) {
            patientSelectComboBox.setReadOnly(true);
        }
        setLeftEyeTreatmentHistory(treatmentPlanDto.getId());
        setRightEyeTreatmentHistory(treatmentPlanDto.getId());
    }
}
