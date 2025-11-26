package de.bbajor.pvs.cost.ui;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;

import de.bbajor.pvs.cost.model.CostCalculation;
import de.bbajor.pvs.cost.model.PricingModel;
import de.bbajor.pvs.cost.service.CostCalculationService;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import lombok.extern.slf4j.Slf4j;

/**
 * Dialog für Erstellung und Bearbeitung von Preismodellen für OP-Säle.
 */
@Slf4j
public class CostCalculationDialog extends Dialog {

    private final CostCalculationService costCalculationService;
    private final InstitutionRepository institutionRepository;
    private final UserAccountRepository userAccountRepository;
    private final SurgicalCenter surgicalCenter;
    private CostCalculation costCalculation;
    private Runnable onSaveCallback;

    // Form fields
    private ComboBox<PricingModel> pricingModelComboBox;
    private BigDecimalField pricePerSlotField;
    private BigDecimalField pricePerHourField;
    private BigDecimalField monthlyFixedCostsField;
    private BigDecimalField variableCostPerTreatmentField;
    private DatePicker validFromPicker;
    private DatePicker validToPicker;
    private Checkbox activeCheckbox;

    // Layout containers for conditional fields
    private VerticalLayout rentalFieldsLayout;
    private VerticalLayout ownedFieldsLayout;

    public CostCalculationDialog(
            CostCalculationService costCalculationService,
            InstitutionRepository institutionRepository,
            UserAccountRepository userAccountRepository,
            SurgicalCenter surgicalCenter,
            CostCalculation costCalculation) {
        this.costCalculationService = costCalculationService;
        this.institutionRepository = institutionRepository;
        this.userAccountRepository = userAccountRepository;
        this.surgicalCenter = surgicalCenter;
        this.costCalculation = costCalculation != null ? costCalculation : createNewCostCalculation();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");

        initializeDialog();
    }

    private CostCalculation createNewCostCalculation() {
        CostCalculation calc = new CostCalculation();
        calc.setSurgicalCenter(surgicalCenter);
        calc.setValidFrom(LocalDate.now());
        calc.setActive(true);
        
        // Set institution from context
        ensureInstitutionContext();
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId != null) {
            Institution institution = institutionRepository.findById(institutionId)
                    .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
            calc.setInstitution(institution);
        }
        
        return calc;
    }

    private void initializeDialog() {
        H3 title = new H3(costCalculation.getId() != null ? "Preismodell bearbeiten" : "Neues Preismodell");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        add(title, formLayout, buttonLayout);
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Pricing Model
        pricingModelComboBox = new ComboBox<>("Preismodell");
        pricingModelComboBox.setItems(PricingModel.values());
        pricingModelComboBox.setValue(costCalculation.getPricingModel());
        pricingModelComboBox.setItemLabelGenerator(this::translatePricingModel);
        pricingModelComboBox.setRequiredIndicatorVisible(true);
        pricingModelComboBox.addValueChangeListener(e -> updateFieldVisibility(e.getValue()));

        // Valid From
        validFromPicker = new DatePicker("Gültig ab");
        validFromPicker.setValue(costCalculation.getValidFrom() != null 
                ? costCalculation.getValidFrom() 
                : LocalDate.now());
        validFromPicker.setRequiredIndicatorVisible(true);

        // Valid To
        validToPicker = new DatePicker("Gültig bis (optional)");
        validToPicker.setValue(costCalculation.getValidTo());

        // Active
        activeCheckbox = new Checkbox("Aktiv");
        activeCheckbox.setValue(costCalculation.getActive() != null ? costCalculation.getActive() : true);

        // RENTAL fields
        rentalFieldsLayout = new VerticalLayout();
        rentalFieldsLayout.setSpacing(false);
        rentalFieldsLayout.setPadding(false);
        
        pricePerSlotField = new BigDecimalField("Preis pro Zeitslot (€)");
        pricePerSlotField.setValue(costCalculation.getPricePerSlot());
        pricePerSlotField.setPlaceholder("z.B. 500.00");
        
        pricePerHourField = new BigDecimalField("Preis pro Stunde (€)");
        pricePerHourField.setValue(costCalculation.getPricePerHour());
        pricePerHourField.setPlaceholder("z.B. 150.00");
        
        rentalFieldsLayout.add(pricePerSlotField, pricePerHourField);

        // OWNED fields
        ownedFieldsLayout = new VerticalLayout();
        ownedFieldsLayout.setSpacing(false);
        ownedFieldsLayout.setPadding(false);
        
        monthlyFixedCostsField = new BigDecimalField("Monatliche Fixkosten (€)");
        monthlyFixedCostsField.setValue(costCalculation.getMonthlyFixedCosts());
        monthlyFixedCostsField.setPlaceholder("z.B. 5000.00");
        monthlyFixedCostsField.setRequiredIndicatorVisible(true);
        
        variableCostPerTreatmentField = new BigDecimalField("Variable Kosten pro Behandlung (€)");
        variableCostPerTreatmentField.setValue(costCalculation.getVariableCostPerTreatment());
        variableCostPerTreatmentField.setPlaceholder("z.B. 50.00");
        
        ownedFieldsLayout.add(monthlyFixedCostsField, variableCostPerTreatmentField);

        layout.add(pricingModelComboBox, validFromPicker, validToPicker, activeCheckbox);
        layout.add(rentalFieldsLayout, ownedFieldsLayout);

        // Initial visibility
        updateFieldVisibility(costCalculation.getPricingModel());

        return layout;
    }

    private void updateFieldVisibility(PricingModel model) {
        if (model == null) {
            rentalFieldsLayout.setVisible(false);
            ownedFieldsLayout.setVisible(false);
            return;
        }

        switch (model) {
            case RENTAL:
                rentalFieldsLayout.setVisible(true);
                ownedFieldsLayout.setVisible(false);
                break;
            case OWNED:
                rentalFieldsLayout.setVisible(false);
                ownedFieldsLayout.setVisible(true);
                break;
        }
    }

    private String translatePricingModel(PricingModel model) {
        return switch (model) {
            case RENTAL -> "Miete (Fixpreis pro Zeitslot/Stunde)";
            case OWNED -> "Eigener OP-Saal (Laufende Kosten)";
        };
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        Button cancelButton = new Button("Abbrechen", event -> close());
        
        Button saveButton = new Button("Speichern", event -> saveCostCalculation());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(cancelButton, saveButton);
        return layout;
    }

    private void saveCostCalculation() {
        try {
            ensureInstitutionContext();

            // Validation
            if (pricingModelComboBox.getValue() == null) {
                showError("Bitte wählen Sie ein Preismodell aus");
                return;
            }

            if (validFromPicker.getValue() == null) {
                showError("Bitte geben Sie ein Gültigkeitsdatum an");
                return;
            }

            PricingModel model = pricingModelComboBox.getValue();
            if (model == PricingModel.RENTAL) {
                if (pricePerSlotField.getValue() == null && pricePerHourField.getValue() == null) {
                    showError("Bitte geben Sie entweder einen Preis pro Zeitslot oder pro Stunde an");
                    return;
                }
            } else if (model == PricingModel.OWNED) {
                if (monthlyFixedCostsField.getValue() == null) {
                    showError("Bitte geben Sie die monatlichen Fixkosten an");
                    return;
                }
            }

            // Set values
            costCalculation.setPricingModel(model);
            costCalculation.setValidFrom(validFromPicker.getValue());
            costCalculation.setValidTo(validToPicker.getValue());
            costCalculation.setActive(activeCheckbox.getValue());

            if (model == PricingModel.RENTAL) {
                costCalculation.setPricePerSlot(pricePerSlotField.getValue());
                costCalculation.setPricePerHour(pricePerHourField.getValue());
                costCalculation.setMonthlyFixedCosts(null);
                costCalculation.setVariableCostPerTreatment(null);
            } else {
                costCalculation.setPricePerSlot(null);
                costCalculation.setPricePerHour(null);
                costCalculation.setMonthlyFixedCosts(monthlyFixedCostsField.getValue());
                costCalculation.setVariableCostPerTreatment(variableCostPerTreatmentField.getValue());
            }

            // Ensure institution is set
            if (costCalculation.getInstitution() == null) {
                Long institutionId = InstitutionContext.getInstitutionId();
                if (institutionId != null) {
                    Institution institution = institutionRepository.findById(institutionId)
                            .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
                    costCalculation.setInstitution(institution);
                }
            }

            costCalculationService.save(costCalculation);

            showSuccess("Preismodell erfolgreich gespeichert");
            
            if (onSaveCallback != null) {
                onSaveCallback.run();
            }
            
            close();
        } catch (Exception e) {
            log.error("Fehler beim Speichern des Preismodells", e);
            showError("Fehler beim Speichern: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    /**
     * Stellt sicher, dass InstitutionContext gesetzt ist.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            if (userAccountRepository != null) {
                try {
                    String username = adapter.getUsername();
                    UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                    
                    if (userAccount != null && userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                    }
                } catch (Exception e) {
                    log.warn("Fehler beim Wiederherstellen des InstitutionContext: {}", e.getMessage());
                }
            }
        }
    }
}

