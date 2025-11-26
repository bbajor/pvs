package de.bbajor.pvs.intravitreal.treatment.ui;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.binder.Binder;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.cost.model.TreatmentCost;
import de.bbajor.pvs.cost.service.CostCalculationService;
import de.bbajor.pvs.cost.service.TreatmentCostService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.service.FeatureFlagService;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.service.UserAccountService;

public class TreatmentDetailLayout extends FormLayout {

    private final Binder<Treatment> binder = new Binder<>(Treatment.class);

    private final ComboBox<SideOfEye> sideOfEyeComboBox = new ComboBox<>("Seite des Auges");
    private final DatePicker treatmentDatePicker = new DatePicker("Behandlungsdatum");
    private final NativeLabel surgicalCenterLabel = new NativeLabel("Operationszentrum");
    private final NativeLabel timeSlotLabel = new NativeLabel("Uhrzeit");

    private final ComboBox<MedicationFavourite> medicationComboBox = new ComboBox<>("Medikament");
    private final MultiSelectComboBox<UserAccount> treatingDoctorsComboBox = new MultiSelectComboBox<>("Behandelnde Ärzte");
    private final TextArea additionalInfoField = new TextArea("Notizen");
    private final DatePicker approvalDatePicker = new DatePicker("Behandlung geprüft am");

    private final boolean isEditable;

    private final TreatmentPlanService treatmentPlanService;
    private final UserAccountService userAccountService;
    private final Treatment treatment;
    private ApplicationContext applicationContext;

    public TreatmentDetailLayout(Treatment treatment, boolean isEditable,
            TreatmentPlanService treatmentPlanService, UserAccountService userAccountService) {
        Objects.requireNonNull(treatment);
        Objects.requireNonNull(treatmentPlanService);
        setSizeFull();
        this.isEditable = isEditable;
        this.treatmentPlanService = treatmentPlanService;
        this.userAccountService = userAccountService;
        this.treatment = treatment;

        surgicalCenterLabel.setTitle("Operationszentrum");
        surgicalCenterLabel.setText(treatment.getSurgicalCenterString());
        add(surgicalCenterLabel);

        timeSlotLabel.setTitle("Uhrzeit der Behandlung");
        timeSlotLabel.setText(treatment.getSurgicalCenterTimeSlot().getStartTime().toString());
        add(timeSlotLabel);

        sideOfEyeComboBox.setItems(SideOfEye.values());
        sideOfEyeComboBox.setValue(treatment.getSideOfEye());
        sideOfEyeComboBox.setItemLabelGenerator(SideOfEye::toString);
        add(sideOfEyeComboBox);

        treatmentDatePicker.setValue(treatment.getDate());
        add(treatmentDatePicker);

        medicationComboBox.setItems(treatmentPlanService.getFavouriteMedications());
        medicationComboBox.setValue(treatment.getMedicationFavourite());
        medicationComboBox.setItemLabelGenerator(MedicationFavourite::getEffectiveDisplayName);
        add(medicationComboBox);

        // Treating doctors selection
        treatingDoctorsComboBox.setItems(userAccountService.findUsersByRole(AppRoles.DOCTOR));
        treatingDoctorsComboBox.setValue(treatment.getTreatingDoctors());
        treatingDoctorsComboBox.setItemLabelGenerator(user -> 
            user.getFullName() != null ? user.getFullName() : user.getUsername()
        );
        treatingDoctorsComboBox.setPlaceholder("Ärzte auswählen");
        add(treatingDoctorsComboBox, 2);

        additionalInfoField.setTitle("Notizen");
        additionalInfoField.setWidthFull();
        additionalInfoField.setHeight("500px");
        additionalInfoField.setPlaceholder("Zusätzliche Informationen...");
        add(additionalInfoField, 2);

        approvalDatePicker.setValue(treatment.getApprovalDate());
        add(approvalDatePicker);

        // Kostenübersicht (nur wenn COST_MODULE aktiviert)
        try {
            if (treatment.getSurgicalCenterTimeSlot() != null) {
                addCostSection(treatment);
            }
        } catch (Exception e) {
            // Feature nicht verfügbar - Sektion nicht anzeigen
        }

        initializeBinder(treatment);

        setReadOnly();
    }

    /**
     * Setzt den ApplicationContext für Service-Zugriff.
     */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Fügt die Kostenübersicht-Sektion hinzu (nur wenn COST_MODULE aktiviert).
     */
    private void addCostSection(Treatment treatment) {
        if (applicationContext == null) {
            return;
        }

        try {
            FeatureFlagService featureFlagService = applicationContext.getBean(FeatureFlagService.class);
            if (featureFlagService == null || !featureFlagService.isFeatureEnabled("COST_MODULE")) {
                return;
            }

            TreatmentCostService treatmentCostService = applicationContext.getBean(TreatmentCostService.class);
            CostCalculationService costCalculationService = applicationContext.getBean(CostCalculationService.class);

            // Section für Kostenübersicht
            Div costSection = new Div();
            costSection.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
            costSection.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
            costSection.getStyle().set("padding", "var(--lumo-space-m)");
            costSection.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
            costSection.getStyle().set("margin-top", "var(--lumo-space-m)");

            H4 costTitle = new H4("Kostenübersicht");
            costTitle.getStyle().set("margin-top", "0");
            costTitle.getStyle().set("margin-bottom", "var(--lumo-space-s)");

            VerticalLayout costContent = new VerticalLayout();
            costContent.setSpacing(true);
            costContent.setPadding(false);

            // Prüfe, ob bereits Kosten berechnet wurden
            Optional<TreatmentCost> existingCost = treatmentCostService.findByTreatmentId(treatment.getId());
            
            if (existingCost.isPresent()) {
                TreatmentCost treatmentCost = existingCost.get();
                costContent.add(new Span("Gesamtkosten Zeitslot: " + 
                    String.format("%.2f €", treatmentCost.getTotalCost())));
                costContent.add(new Span("Kostenanteil pro Patient: " + 
                    String.format("%.2f €", treatmentCost.getCostPerPatient())));
                costContent.add(new Span("Anzahl Patienten im Zeitslot: " + 
                    (treatmentCost.getPatientCountAtCalculation() != null 
                        ? treatmentCost.getPatientCountAtCalculation() : "-")));
                if (treatmentCost.getPricingModelUsed() != null) {
                    costContent.add(new Span("Preismodell: " + 
                        (treatmentCost.getPricingModelUsed() == de.bbajor.pvs.cost.model.PricingModel.RENTAL 
                            ? "Miete" : "Eigener OP-Saal")));
                }
            } else {
                // Berechne vorläufige Kosten
                if (treatment.getSurgicalCenterTimeSlot() != null 
                        && treatment.getSurgicalCenterTimeSlot().getSurgicalCenter() != null
                        && treatment.getSurgicalCenterTimeSlot().getDate() != null) {
                    BigDecimal estimatedCost = costCalculationService.calculateCostForTimeSlot(
                        treatment.getSurgicalCenterTimeSlot(),
                        treatment.getSurgicalCenterTimeSlot().getDate());
                    
                    if (estimatedCost.compareTo(BigDecimal.ZERO) > 0) {
                        costContent.add(new Span("Geschätzte Kosten (Zeitslot): " + 
                            String.format("%.2f €", estimatedCost)));
                        costContent.add(new Span("Hinweis: Kosten werden erst nach Berechnung gespeichert."));
                    } else {
                        costContent.add(new Span("Kein Preismodell für diesen OP-Saal konfiguriert."));
                    }
                }

                // Button zum Berechnen und Speichern
                Button calculateButton = new Button("Kosten berechnen", VaadinIcon.CALC_BOOK.create());
                calculateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                calculateButton.addClickListener(e -> {
                    try {
                        // Hole aktuellen User
                        UserAccount currentUser = null;
                        try {
                            String username = org.springframework.security.core.context.SecurityContextHolder
                                .getContext().getAuthentication().getName();
                            currentUser = userAccountService.findAll().stream()
                                .filter(u -> u.getUsername().equals(username))
                                .findFirst()
                                .orElse(null);
                        } catch (Exception ex) {
                            // User nicht gefunden - trotzdem berechnen
                        }

                        TreatmentCost calculatedCost = treatmentCostService.calculateAndSaveTreatmentCost(
                            treatment, currentUser);
                        
                        Notification.show("Kosten erfolgreich berechnet und gespeichert", 3000,
                            Notification.Position.BOTTOM_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        
                        // Aktualisiere die Anzeige
                        costContent.removeAll();
                        costContent.add(new Span("Gesamtkosten Zeitslot: " + 
                            String.format("%.2f €", calculatedCost.getTotalCost())));
                        costContent.add(new Span("Kostenanteil pro Patient: " + 
                            String.format("%.2f €", calculatedCost.getCostPerPatient())));
                        costContent.add(new Span("Anzahl Patienten im Zeitslot: " + 
                            (calculatedCost.getPatientCountAtCalculation() != null 
                                ? calculatedCost.getPatientCountAtCalculation() : "-")));
                        if (calculatedCost.getPricingModelUsed() != null) {
                            costContent.add(new Span("Preismodell: " + 
                                (calculatedCost.getPricingModelUsed() == de.bbajor.pvs.cost.model.PricingModel.RENTAL 
                                    ? "Miete" : "Eigener OP-Saal")));
                        }
                        calculateButton.setVisible(false);
                    } catch (Exception ex) {
                        Notification.show("Fehler beim Berechnen der Kosten: " + ex.getMessage(), 5000,
                            Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                });
                costContent.add(calculateButton);
            }

            costSection.add(costTitle, costContent);
            add(costSection, 2); // Spannt über 2 Spalten
        } catch (Exception e) {
            // Feature nicht verfügbar - Sektion nicht anzeigen
        }
    }

    private void initializeBinder(Treatment treatment) {
        binder.forField(sideOfEyeComboBox).asRequired("Bitte Seite des Auges auswählen")
                .bind(Treatment::getSideOfEye, Treatment::setSideOfEye);
        binder.forField(treatmentDatePicker).asRequired("Bitte Behandlungsdatum auswählen")
                .bind(Treatment::getDate, (t, v) -> {
                    // no setter available
                });
        binder.forField(medicationComboBox).asRequired("Bitte Medikament auswählen")
                .bind(Treatment::getMedicationFavourite, Treatment::setMedicationFavourite);
        binder.forField(treatingDoctorsComboBox)
                .bind(t -> t.getTreatingDoctors(), 
                      (t, doctors) -> {
                          t.getTreatingDoctors().clear();
                          if (doctors != null) {
                              t.getTreatingDoctors().addAll(doctors);
                          }
                      });
        binder.forField(additionalInfoField).bind(Treatment::getAdditionalInfo, Treatment::setAdditionalInfo);
        binder.forField(approvalDatePicker).bind(Treatment::getApprovalDate, Treatment::setApprovalDate);
        binder.readBean(treatment);
    }

    private void setReadOnly() {
        sideOfEyeComboBox.setReadOnly(!isEditable);
        treatmentDatePicker.setReadOnly(!isEditable);
        medicationComboBox.setReadOnly(!isEditable);
        treatingDoctorsComboBox.setReadOnly(!isEditable);
        additionalInfoField.setReadOnly(!isEditable);
        approvalDatePicker.setReadOnly(!isEditable);
    }
}