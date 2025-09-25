package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.Scroller.ScrollDirection;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.Query;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.bbajor.pvs.base.dto.SideOfEye;
import de.bbajor.pvs.base.ui.component.TimelineView;
import de.bbajor.pvs.intravitreal.treatment.controller.IvomPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomDiagnosisDto;
import de.bbajor.pvs.intravitreal.treatment.dto.IvomPlanDto;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentSlotDto;
import de.bbajor.pvs.medication.dto.IntravitrealMedicationDto;
import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterDto;
import de.bbajor.pvs.surgicalcenter.dto.SurgicalCenterTimeSlotDto;

public class IvomPlanLayout extends VerticalLayout {

    private Binder<IvomPlanDto> binder = new Binder<>(IvomPlanDto.class);

    final DatePicker creationDate = new DatePicker("Erstellt am");
    final ComboBox<PatientDto> patientSelectComboBox = new ComboBox<>("Patient");
    final ComboBox<IvomDiagnosisDto> diseaseComboBox = new ComboBox<>("Behandlungsgrund");
    final ComboBox<SideOfEye> sideOfEye = new ComboBox<>("Welches Auge?");
    final ComboBox<IntravitrealMedicationDto> ivomDrugsComboBox = new ComboBox<>("Medikament");
    final ComboBox<SurgicalCenterDto> surgeryUnitComboBox = new ComboBox<>("Behandlungsort");
    final TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    final Button filterTimeSlotsButton = new Button("Verfügbare Slots filtern");
    final Grid<TreatmentSlotDto> timeSlotGrid = new Grid<>();
    final TimelineView timeLineView = null;
    final TextArea additionalInformation = new TextArea("Notizen");

    private final IvomPlanPresenter presenter;

    public IvomPlanLayout(IvomPlanPresenter presenter) {
        this.presenter = presenter;

        sideOfEye.setItems(SideOfEye.values());
        patientSelectComboBox.setItems(presenter.getPatients());
        patientSelectComboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                if (binder.getBean() == null) {
                    binder.setBean(new IvomPlanDto());
                }
                binder.getBean().setPatient(event.getValue());
            }
        });
        ivomDrugsComboBox.setItems(presenter.getDrugs());

        timeSlotGrid.setSizeFull();
        timeSlotGrid.addColumn(TreatmentSlotDto::getSurgeryUnitString).setHeader("Einrichtung");
        timeSlotGrid.addColumn(TreatmentSlotDto::getDate).setHeader("Datum");
        timeSlotGrid.addColumn(TreatmentSlotDto::getDate)
                .setRenderer(new ComponentRenderer<NativeLabel, TreatmentSlotDto>(dto -> {
                    return new NativeLabel(dto.getDate().getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.GERMAN));
                })).setHeader("Wochentag");
        timeSlotGrid.setSelectionMode(SelectionMode.MULTI);

        surgeryUnitComboBox.setItems(presenter.getSurgeryUnits());
        surgeryUnitComboBox.setClearButtonVisible(true);

        additionalInformation.setWidthFull();
        additionalInformation.setHeight("150px");
        creationDate.setEnabled(false);

        diseaseComboBox.setItemLabelGenerator(IvomDiagnosisDto::getName);
        diseaseComboBox.setAllowCustomValue(true);
        diseaseComboBox.setClearButtonVisible(true);
        diseaseComboBox.addCustomValueSetListener(event -> {
            String newValue = event.getDetail();
            // Optional: trim & prüfen
            if (newValue != null && !newValue.trim().isEmpty()) {
                // Neues Dto
                IvomDiagnosisDto newDto = new IvomDiagnosisDto();
                newDto.setName(newValue.trim());

                // In DB speichern, falls nötig
                IvomDiagnosisDto saved = presenter.saveDiagnosis(newDto);

                // ComboBox aktualisieren
                List<IvomDiagnosisDto> items = new ArrayList<>(
                        diseaseComboBox.getDataProvider().fetch(new Query<>()).toList());
                items.add(saved);
                diseaseComboBox.setItems(items);

                // Setze das neue Entity als ausgewählt
                diseaseComboBox.setValue(newDto);
            }
        });
        diseaseComboBox.setItems(presenter.getDiseases());

        filterTimeSlotsButton.addClickListener(click -> {
            List<SurgicalCenterTimeSlotDto> filteredTimeSlots = new ArrayList<>(
                    presenter.getAllTimeSlotsFilteredBy(timeSlotConfigForm.getCurrentConfig(),
                            surgeryUnitComboBox.getValue()));
            List<TreatmentSlotDto> bookedTimeSlots = new ArrayList<>();
            for (SurgicalCenterTimeSlotDto availableSlot : filteredTimeSlots) {
                TreatmentSlotDto bookedTimeSlot = new TreatmentSlotDto();
                bookedTimeSlot.setIvomPlan(geIvomDto());
                bookedTimeSlot.setSurgicalCenterTimeSlot(availableSlot);
                bookedTimeSlots.add(bookedTimeSlot);
            }
            timeSlotGrid.setItems(bookedTimeSlots);
        });

        initializeGeneralDetails();

        // Filter als HorizontalLayout im Accordion nutzen
        add(timeSlotConfigForm); // TODO es reichen zwei Checkboxen zur Eingrenzung der Termine ein halbes Jahr
                                 // im Voraus
        add(filterTimeSlotsButton);

        // direkt hier einfügen
        add(timeSlotGrid);

        Random random = new Random();

        List<LocalDate> dates = new ArrayList<>();
        // Startdatum (z. B. heute)
        LocalDate currentDate = LocalDate.now();
        dates.add(currentDate);

        // mögliche Abstände in Wochen
        int[] possibleWeeks = { 4, 6, 8, 16 };

        for (int i = 1; i < 50; i++) {
            // Zufälligen Index für Abstand auswählen
            int weeks = possibleWeeks[random.nextInt(possibleWeeks.length)];
            currentDate = currentDate.plusWeeks(weeks);
            dates.add(currentDate);
        }

        initializeTimeLineLeftEye(dates);

        initializeTimeLineRightEye(dates);

        // direkt hier einfügen
        add(additionalInformation);

        binder.forField(creationDate).bind(IvomPlanDto::getCreationDate, IvomPlanDto::setCreationDate);
        binder.forField(sideOfEye).bind(IvomPlanDto::getSideOfEye, IvomPlanDto::setSideOfEye);

    }

    private void initializeTimeLineRightEye(List<LocalDate> dates) {
        // rechtes Auge
        Scroller scrollerRight = new Scroller(new TimelineView(dates));
        scrollerRight.setScrollDirection(ScrollDirection.HORIZONTAL);
        scrollerRight.setWidthFull();
        scrollerRight.setHeight("300px");
        Accordion accordionRight = new Accordion();
        accordionRight.setWidthFull();
        AccordionPanel accordionPanelRight = accordionRight.add("Behandlungsverlauf rechtes Auge", scrollerRight);
        accordionPanelRight.setOpened(true);
        accordionPanelRight.getElement().getStyle().set("width", "100%");
        add(accordionPanelRight);
    }

    private void initializeGeneralDetails() {
        FormLayout formLayout = new FormLayout();
        formLayout.add(creationDate);
        formLayout.add(patientSelectComboBox);
        formLayout.add(diseaseComboBox);
        formLayout.add(sideOfEye);
        formLayout.add(ivomDrugsComboBox);
        formLayout.add(surgeryUnitComboBox);
        AccordionPanel generalDetailsPanel = new AccordionPanel();
        Accordion generalDetails = new Accordion();
        generalDetailsPanel.add(formLayout);
        generalDetailsPanel.setOpened(true);
        generalDetails.add(generalDetailsPanel);
        add(generalDetails);
    }

    private void initializeTimeLineLeftEye(List<LocalDate> dates) {
        // linkes Auge
        Scroller scrollerLeft = new Scroller(new TimelineView(dates));
        scrollerLeft.setScrollDirection(ScrollDirection.HORIZONTAL);
        scrollerLeft.setWidthFull();
        scrollerLeft.setHeight("300px");
        Accordion accordionLeft = new Accordion();
        accordionLeft.setWidthFull();
        AccordionPanel accordionPanelLeft = accordionLeft.add("Behandlungsverlauf linkes Auge", scrollerLeft);
        accordionPanelLeft.setOpened(true);
        accordionPanelLeft.getElement().getStyle().set("width", "100%");
        add(accordionPanelLeft);
    }

    public void setIvom(IvomPlanDto ivom) {
        binder.setBean(ivom);
    }

    public IvomPlanDto geIvomDto() {
        return binder.getBean();
    }

    public List<TreatmentSlotDto> getTimeSlotsToCreate() {
        return new ArrayList<>(timeSlotGrid.getSelectedItems());
    }

    public void setBean(IvomPlanDto newDto) {
        binder.setBean(newDto);
    }

}
