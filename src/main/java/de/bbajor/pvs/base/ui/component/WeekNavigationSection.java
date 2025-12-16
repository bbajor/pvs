package de.bbajor.pvs.base.ui.component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * Zentrale Komponente für die Wochennavigation.
 * Kann in verschiedenen Dialogen und Views wiederverwendet werden.
 */
public class WeekNavigationSection extends Div {
    
    private LocalDate currentWeekStart;
    private Span weekLabel;
    private DatePicker datePicker;
    private final Consumer<LocalDate> onWeekChange;
    private final String sectionTitle;
    
    public WeekNavigationSection(String sectionTitle, LocalDate initialWeekStart, Consumer<LocalDate> onWeekChange) {
        this.sectionTitle = sectionTitle;
        this.currentWeekStart = initialWeekStart != null 
            ? initialWeekStart.with(DayOfWeek.MONDAY) 
            : LocalDate.now().with(DayOfWeek.MONDAY);
        this.onWeekChange = onWeekChange;
        
        addClassName("dialog-section");
        setWidthFull();
        getStyle()
            .set("background-color", "var(--lumo-contrast-5pct)")
            .set("border", "1px solid var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-m)")
            .set("padding", "var(--lumo-space-m)")
            .set("box-sizing", "border-box")
            .set("margin-bottom", "var(--lumo-space-m)");
        
        // Überschrift
        H4 title = new H4(sectionTitle);
        title.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "var(--lumo-space-s)")
            .set("color", "var(--lumo-primary-text-color)")
            .set("font-size", "var(--lumo-font-size-m)")
            .set("font-weight", "600");
        add(title);
        
        // Navigation-Controls
        HorizontalLayout weekNavigation = new HorizontalLayout();
        weekNavigation.setWidthFull();
        weekNavigation.setAlignItems(FlexComponent.Alignment.CENTER);
        weekNavigation.setSpacing(true);
        weekNavigation.setPadding(false);
        weekNavigation.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        
        // Pfeil nach links (vorherige Woche)
        Button prevWeekButton = new Button(new Icon(VaadinIcon.ARROW_LEFT));
        prevWeekButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        prevWeekButton.setTooltipText("Vorherige Woche");
        prevWeekButton.addClickListener(e -> navigateToWeek(-1));
        
        // Woche-Anzeige
        weekLabel = new Span();
        updateWeekLabel();
        
        // Pfeil nach rechts (nächste Woche)
        Button nextWeekButton = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
        nextWeekButton.addThemeVariants(ButtonVariant.LUMO_ICON);
        nextWeekButton.setTooltipText("Nächste Woche");
        nextWeekButton.addClickListener(e -> navigateToWeek(1));
        
        // DatePicker für spezifische Auswahl
        datePicker = new DatePicker("Datum auswählen");
        datePicker.setValue(currentWeekStart);
        datePicker.setWidth("200px");
        datePicker.setTooltipText("Wählen Sie ein Datum aus der gewünschten Woche");
        
        // Montag als ersten Tag der Woche setzen
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();
        i18n.setFirstDayOfWeek(1); // 1 = Montag, 0 = Sonntag
        i18n.setMonthNames(java.util.Arrays.asList(
            "Januar", "Februar", "März", "April", "Mai", "Juni",
            "Juli", "August", "September", "Oktober", "November", "Dezember"
        ));
        i18n.setWeekdays(java.util.Arrays.asList(
            "So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"
        ));
        i18n.setWeekdaysShort(java.util.Arrays.asList(
            "So", "Mo", "Di", "Mi", "Do", "Fr", "Sa"
        ));
        datePicker.setI18n(i18n);
        
        datePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                LocalDate selectedDate = e.getValue();
                LocalDate monday = selectedDate.with(DayOfWeek.MONDAY);
                if (!monday.equals(currentWeekStart)) {
                    setWeekStart(monday);
                }
            }
        });
        
        weekNavigation.add(prevWeekButton, weekLabel, nextWeekButton, datePicker);
        add(weekNavigation);
    }
    
    private void updateWeekLabel() {
        LocalDate endOfWeek = currentWeekStart.plusDays(6);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
        weekLabel.setText(String.format("Woche: %s - %s", 
            currentWeekStart.format(formatter), 
            endOfWeek.format(formatter)));
    }
    
    private void navigateToWeek(int weeksOffset) {
        LocalDate newWeekStart = currentWeekStart.plusWeeks(weeksOffset);
        setWeekStart(newWeekStart);
    }
    
    public void setWeekStart(LocalDate weekStart) {
        this.currentWeekStart = weekStart.with(DayOfWeek.MONDAY);
        updateWeekLabel();
        datePicker.setValue(currentWeekStart);
        if (onWeekChange != null) {
            onWeekChange.accept(currentWeekStart);
        }
    }
    
    public LocalDate getCurrentWeekStart() {
        return currentWeekStart;
    }
    
    public void setPreviousWeekEnabled(boolean enabled) {
        // Kann für zukünftige Erweiterungen verwendet werden
    }
    
    public void setNextWeekEnabled(boolean enabled) {
        // Kann für zukünftige Erweiterungen verwendet werden
    }
}

