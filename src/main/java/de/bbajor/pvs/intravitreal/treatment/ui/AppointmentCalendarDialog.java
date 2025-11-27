package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import com.flowingcode.addons.ycalendar.MonthCalendar;
import java.util.Set;
import java.util.HashSet;

/**
 * Dialog zur Auswahl eines Termins mit YearCalendar.
 * Zeigt verfügbare Termine und bereits gebuchte Behandlungen des Patienten an.
 */
public class AppointmentCalendarDialog extends Dialog {
    
    private final TreatmentPlan treatmentPlan;
    private final SurgicalCenter surgicalCenter;
    private final TreatmentPlanPresenter presenter;
    private final Consumer<LocalDate> onDateSelected;
    
    private MonthCalendar calendar;
    private LocalDate selectedDate;
    private YearMonth currentMonth;
    private final Set<LocalDate> availableDates = new HashSet<>();
    private final Set<LocalDate> bookedDates = new HashSet<>();
    private Button selectButton;
    
    public AppointmentCalendarDialog(
            TreatmentPlan treatmentPlan,
            SurgicalCenter surgicalCenter,
            TreatmentPlanPresenter presenter,
            ApplicationContext context,
            Consumer<LocalDate> onDateSelected) {
        this.treatmentPlan = treatmentPlan;
        this.surgicalCenter = surgicalCenter;
        this.presenter = presenter;
        this.onDateSelected = onDateSelected;
        
        setHeaderTitle("Termin auswählen");
        setWidth("900px");
        setHeight("700px");
        setCloseOnOutsideClick(false);
        
        add(createContent());
        add(createFooter());
    }
    
    private VerticalLayout createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(true);
        
        // Monatsnavigation
        HorizontalLayout navigation = createMonthNavigation();
        content.add(navigation);
        
        // Setze aktuellen Monat
        currentMonth = YearMonth.now();
        
        // Erstelle MonthCalendar mit aktuellem Monat
        calendar = new MonthCalendar(currentMonth);
        calendar.setSizeFull();
        calendar.setHeight("600px");
        
        // Lade verfügbare Termine und bereits gebuchte Behandlungen
        loadCalendarData();
        
        // Markiere verfügbare und gebuchte Termine mit CSS-Klassen
        calendar.setClassNameGenerator(date -> {
            if (bookedDates.contains(date)) {
                return "booked-date";
            }
            if (availableDates.contains(date)) {
                return "available-date";
            }
            return null;
        });
        
        // Listener für Datumsauswahl
        calendar.addDateSelectedListener(event -> {
            LocalDate date = event.getDate();
            if (date != null && availableDates.contains(date)) {
                selectedDate = date;
                if (selectButton != null) {
                    selectButton.setEnabled(true);
                }
            } else if (date != null) {
                // Datum ausgewählt, aber nicht verfügbar oder bereits gebucht
                selectedDate = null;
                if (selectButton != null) {
                    selectButton.setEnabled(false);
                }
            } else {
                // Auswahl aufgehoben
                selectedDate = null;
                if (selectButton != null) {
                    selectButton.setEnabled(false);
                }
            }
        });
        
        content.add(calendar);
        content.expand(calendar);
        
        return content;
    }
    
    /**
     * Erstellt die Monatsnavigation mit Vorheriger/Nächster Monat, Heute und DatePicker.
     */
    private HorizontalLayout createMonthNavigation() {
        HorizontalLayout navigation = new HorizontalLayout();
        navigation.setWidthFull();
        navigation.setJustifyContentMode(HorizontalLayout.JustifyContentMode.CENTER);
        navigation.setAlignItems(HorizontalLayout.Alignment.CENTER);
        navigation.setSpacing(true);
        
        // DatePicker für Monat/Jahr Auswahl (muss vor den Buttons erstellt werden für Referenz)
        DatePicker monthPicker = new DatePicker("Monat auswählen");
        monthPicker.setValue(currentMonth.atDay(1));
        
        // Vorheriger Monat
        Button previousMonth = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            currentMonth = currentMonth.minusMonths(1);
            monthPicker.setValue(currentMonth.atDay(1));
            updateCalendarMonth();
        });
        previousMonth.setTooltipText("Vorheriger Monat");
        
        // Heute
        Button today = new Button("Heute", e -> {
            currentMonth = YearMonth.now();
            monthPicker.setValue(currentMonth.atDay(1));
            updateCalendarMonth();
        });
        
        // Nächster Monat
        Button nextMonth = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            currentMonth = currentMonth.plusMonths(1);
            monthPicker.setValue(currentMonth.atDay(1));
            updateCalendarMonth();
        });
        nextMonth.setTooltipText("Nächster Monat");
        
        monthPicker.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                currentMonth = YearMonth.from(event.getValue());
                updateCalendarMonth();
            }
        });
        
        navigation.add(previousMonth, today, nextMonth, monthPicker);
        
        return navigation;
    }
    
    /**
     * Aktualisiert den angezeigten Monat im Calendar.
     */
    private void updateCalendarMonth() {
        // Erstelle neuen MonthCalendar mit dem neuen Monat
        // Da MonthCalendar immutable sein könnte, entfernen und neu hinzufügen
        VerticalLayout parent = (VerticalLayout) calendar.getParent().orElse(null);
        if (parent != null) {
            parent.remove(calendar);
            calendar = new MonthCalendar(currentMonth);
            calendar.setSizeFull();
            calendar.setHeight("600px");
            
            // Setze ClassNameGenerator erneut
            calendar.setClassNameGenerator(date -> {
                if (bookedDates.contains(date)) {
                    return "booked-date";
                }
                if (availableDates.contains(date)) {
                    return "available-date";
                }
                return null;
            });
            
            // Setze DateSelectedListener erneut
            calendar.addDateSelectedListener(event -> {
                LocalDate date = event.getDate();
                if (date != null && availableDates.contains(date)) {
                    selectedDate = date;
                    if (selectButton != null) {
                        selectButton.setEnabled(true);
                    }
                } else if (date != null) {
                    selectedDate = null;
                    if (selectButton != null) {
                        selectButton.setEnabled(false);
                    }
                } else {
                    selectedDate = null;
                    if (selectButton != null) {
                        selectButton.setEnabled(false);
                    }
                }
            });
            
            parent.add(calendar);
            parent.expand(calendar);
        }
    }
    
    private HorizontalLayout createFooter() {
        HorizontalLayout footer = new HorizontalLayout();
        footer.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.END);
        footer.setSpacing(true);
        
        Button cancelButton = new Button("Abbrechen", e -> close());
        
        selectButton = new Button("Auswählen", e -> {
            if (selectedDate != null) {
                onDateSelected.accept(selectedDate);
                close();
            }
        });
        selectButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        selectButton.setEnabled(false);
        
        footer.add(cancelButton, selectButton);
        return footer;
    }
    
    /**
     * Lädt die Kalenderdaten: verfügbare Termine und bereits gebuchte Behandlungen.
     */
    private void loadCalendarData() {
        availableDates.clear();
        bookedDates.clear();
        
        // Lade verfügbare Termine für den Behandlungsort
        List<SurgicalCenterTimeSlot> availableSlots = loadAvailableTimeSlots();
        
        // Sammle verfügbare Daten
        for (SurgicalCenterTimeSlot slot : availableSlots) {
            if (slot.getDate() != null) {
                availableDates.add(slot.getDate());
            }
        }
        
        // Lade bereits gebuchte Behandlungen des Patienten
        List<Treatment> patientTreatments = loadPatientTreatments();
        
        // Sammle gebuchte Daten
        for (Treatment treatment : patientTreatments) {
            if (treatment.getSurgicalCenterTimeSlot() != null && treatment.getSurgicalCenterTimeSlot().getDate() != null) {
                bookedDates.add(treatment.getSurgicalCenterTimeSlot().getDate());
            }
        }
        
        // Entferne gebuchte Daten aus verfügbaren Daten (nur verfügbare Termine können ausgewählt werden)
        availableDates.removeAll(bookedDates);
    }
    
    /**
     * Lädt verfügbare Termine für den Behandlungsort.
     */
    private List<SurgicalCenterTimeSlot> loadAvailableTimeSlots() {
        if (surgicalCenter == null) {
            return new ArrayList<>();
        }
        
        LocalDate startDate = LocalDate.now();
        
        ensureInstitutionContext();
        
        Integer centerId = surgicalCenter.getId();
        if (centerId == null) {
            return new ArrayList<>();
        }
        
        return presenter.getAllTimeSlotsFilteredBy(
            startDate,
            de.bbajor.pvs.base.util.TimePeriod.THREE_MONTHS,
            de.bbajor.pvs.base.util.TimeSlotRepetition.EVERY_FOUR_WEEKS,
            centerId
        ).stream()
        .filter(slot -> slot.getSurgicalCenter() != null 
            && slot.getSurgicalCenter().getId() != null
            && slot.getSurgicalCenter().getId().equals(centerId))
        .toList();
    }
    
    /**
     * Lädt bereits gebuchte Behandlungen des Patienten.
     */
    private List<Treatment> loadPatientTreatments() {
        if (treatmentPlan == null || treatmentPlan.getId() == null) {
            return new ArrayList<>();
        }
        
        List<Treatment> allTreatments = new ArrayList<>();
        if (treatmentPlan.getPatient() != null) {
            allTreatments.addAll(presenter.getTreatmentDtos(
                de.bbajor.pvs.base.util.SideOfEye.LEFT, 
                treatmentPlan.getId()
            ));
            allTreatments.addAll(presenter.getTreatmentDtos(
                de.bbajor.pvs.base.util.SideOfEye.RIGHT, 
                treatmentPlan.getId()
            ));
        }
        
        return allTreatments;
    }
    
    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     */
    private void ensureInstitutionContext() {
        if (de.bbajor.pvs.institution.context.InstitutionContext.hasInstitution()) {
            return;
        }
        
        if (treatmentPlan != null && treatmentPlan.getInstitution() != null && treatmentPlan.getInstitution().getId() != null) {
            de.bbajor.pvs.institution.context.InstitutionContext.setInstitutionId(treatmentPlan.getInstitution().getId());
        }
    }
}
