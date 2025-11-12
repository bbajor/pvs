package de.bbajor.pvs.settings.ui.tabs;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.service.HealthInsuranceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Tab für die Verwaltung von Versicherungen.
 * Ermöglicht das Hinzufügen, Bearbeiten und Deaktivieren von Versicherungen.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@Slf4j
public class InsuranceSettingsTab extends VerticalLayout {

    private final HealthInsuranceService healthInsuranceService;

    private TextField costCarrierNameField;
    private TextField billingCarrierNameField;
    private TextField costCarrierIdField;
    private TextField billingCarrierIdField;
    private RadioButtonGroup<String> insuranceTypeGroup;
    private Button saveButton;
    private Button cancelButton;
    private Button deleteButton;
    private Grid<HealthInsurance> insuranceGrid;
    
    private HealthInsurance selectedInsurance;
    private List<HealthInsurance> allInsurances;

    @PostConstruct
    private void init() {
        setSpacing(true);
        setPadding(true);

        // InstitutionContext sicherstellen
        ensureInstitutionContext();

        // Prüfe, ob InstitutionContext gesetzt ist
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            H3 errorTitle = new H3("Versicherungsverwaltung");
            add(errorTitle);
            Notification.show("Keine Institution ausgewählt. Bitte melden Sie sich mit einer Institution an.",
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        H3 title = new H3("Versicherungsverwaltung");

        // Initialize grid
        insuranceGrid = new Grid<>(HealthInsurance.class, false);
        insuranceGrid.addColumn(new ComponentRenderer<>(insurance -> {
            String name = insurance.getCostCarrierName() != null ? insurance.getCostCarrierName() : "-";
            Span nameSpan = new Span(name);
            nameSpan.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.SEMIBOLD);
            return nameSpan;
        })).setHeader("Kostenträger").setAutoWidth(true);
        
        insuranceGrid.addColumn(new ComponentRenderer<>(insurance -> {
            String name = insurance.getBillingCarrierName() != null ? insurance.getBillingCarrierName() : "-";
            Span nameSpan = new Span(name);
            nameSpan.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.TextColor.SECONDARY);
            return nameSpan;
        })).setHeader("Abrechnungsstelle").setAutoWidth(true);
        
        insuranceGrid.addColumn(new ComponentRenderer<>(insurance -> {
            String id = insurance.getCostCarrierId() != null ? insurance.getCostCarrierId() : "-";
            Span idSpan = new Span(id);
            idSpan.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.TextColor.SECONDARY);
            return idSpan;
        })).setHeader("Kostenträger-ID").setAutoWidth(true);
        
        insuranceGrid.addColumn(new ComponentRenderer<>(insurance -> {
            String type = insurance.getInsuranceType() != null ? insurance.getInsuranceType() : "-";
            Span typeSpan = new Span(type);
            return typeSpan;
        })).setHeader("Versicherungsart").setAutoWidth(true);
        
        insuranceGrid.setSizeFull();
        insuranceGrid.setHeightFull();
        insuranceGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        
        insuranceGrid.asSingleSelect().addValueChangeListener(event -> {
            selectedInsurance = event.getValue();
            if (selectedInsurance != null) {
                loadInsuranceData(selectedInsurance);
            } else {
                clearForm();
            }
        });

        // Form fields
        costCarrierNameField = new TextField("Kostenträger");
        costCarrierNameField.setRequired(true);
        costCarrierNameField.setWidthFull();

        billingCarrierNameField = new TextField("Abrechnungsstelle");
        billingCarrierNameField.setWidthFull();

        costCarrierIdField = new TextField("Kostenträger-ID");
        costCarrierIdField.setWidthFull();

        billingCarrierIdField = new TextField("Abrechnungsstellen-ID");
        billingCarrierIdField.setWidthFull();

        insuranceTypeGroup = new RadioButtonGroup<>();
        insuranceTypeGroup.setLabel("Versicherungsart");
        insuranceTypeGroup.setItems("Gesetzlich", "Privat");
        insuranceTypeGroup.setValue("Gesetzlich");
        insuranceTypeGroup.setRequired(true);

        // Buttons - Business-App Style
        saveButton = new Button("Speichern", e -> saveInsurance());
        saveButton.setIcon(VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.SEMIBOLD);

        cancelButton = new Button("Abbrechen", e -> {
            clearForm();
            insuranceGrid.asSingleSelect().clear();
        });
        cancelButton.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.SEMIBOLD);

        deleteButton = new Button("Deaktivieren", e -> deleteInsurance());
        deleteButton.setIcon(VaadinIcon.TRASH.create());
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.addClassNames(com.vaadin.flow.theme.lumo.LumoUtility.FontWeight.SEMIBOLD);
        deleteButton.setEnabled(false);

        // Layout: Grid links, Form rechts (Business-App Style)
        FormLayout formLayout = new FormLayout();
        formLayout.add(costCarrierNameField, 2);
        formLayout.add(billingCarrierNameField, 2);
        formLayout.add(costCarrierIdField, 1);
        formLayout.add(billingCarrierIdField, 1);
        formLayout.add(insuranceTypeGroup, 2);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.add(saveButton, cancelButton, deleteButton);

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setSpacing(true);
        formContainer.setPadding(true);
        formContainer.setWidth("400px");
        formContainer.setMinWidth("400px");
        formContainer.add(new H3("Versicherung hinzufügen/bearbeiten"));
        formContainer.add(formLayout);
        formContainer.add(buttonLayout);

        // Split Layout: Grid links (flex-grow), Form rechts (fixed width)
        HorizontalLayout splitLayout = new HorizontalLayout();
        splitLayout.setSizeFull();
        splitLayout.setSpacing(true);
        splitLayout.setPadding(false);
        splitLayout.add(insuranceGrid);
        splitLayout.add(formContainer);
        splitLayout.setFlexGrow(1, insuranceGrid);
        splitLayout.setFlexGrow(0, formContainer);

        add(title);
        add(splitLayout);
        setFlexGrow(1, splitLayout);

        refreshGrid();
    }

    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext gesetzt aus InstitutionAuthenticationToken: {}", 
                        institutionAuth.getInstitutionId());
            }
        }
    }

    private void refreshGrid() {
        ensureInstitutionContext();
        allInsurances = healthInsuranceService.findAllForCurrentInstitution();
        insuranceGrid.setItems(allInsurances);
    }

    private void loadInsuranceData(HealthInsurance insurance) {
        costCarrierNameField.setValue(insurance.getCostCarrierName() != null ? insurance.getCostCarrierName() : "");
        billingCarrierNameField.setValue(insurance.getBillingCarrierName() != null ? insurance.getBillingCarrierName() : "");
        costCarrierIdField.setValue(insurance.getCostCarrierId() != null ? insurance.getCostCarrierId() : "");
        billingCarrierIdField.setValue(insurance.getBillingCarrierId() != null ? insurance.getBillingCarrierId() : "");
        
        String insuranceType = insurance.getInsuranceType();
        if (insuranceType != null) {
            if (insuranceType.contains("Gesetzlich") || insuranceType.contains("gesetzlich")) {
                insuranceTypeGroup.setValue("Gesetzlich");
            } else if (insuranceType.contains("Privat") || insuranceType.contains("privat")) {
                insuranceTypeGroup.setValue("Privat");
            }
        }
        
        deleteButton.setEnabled(true);
    }

    private void clearForm() {
        costCarrierNameField.clear();
        billingCarrierNameField.clear();
        costCarrierIdField.clear();
        billingCarrierIdField.clear();
        insuranceTypeGroup.setValue("Gesetzlich");
        selectedInsurance = null;
        deleteButton.setEnabled(false);
    }

    private void saveInsurance() {
        if (costCarrierNameField.getValue() == null || costCarrierNameField.getValue().trim().isEmpty()) {
            Notification.show("Bitte geben Sie einen Kostenträger ein.",
                    3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            ensureInstitutionContext();
            
            HealthInsurance insurance;
            if (selectedInsurance != null) {
                insurance = selectedInsurance;
            } else {
                insurance = new HealthInsurance();
            }

            insurance.setCostCarrierName(costCarrierNameField.getValue().trim());
            insurance.setBillingCarrierName(billingCarrierNameField.getValue() != null 
                    ? billingCarrierNameField.getValue().trim() : null);
            insurance.setCostCarrierId(costCarrierIdField.getValue() != null 
                    ? costCarrierIdField.getValue().trim() : null);
            insurance.setBillingCarrierId(billingCarrierIdField.getValue() != null 
                    ? billingCarrierIdField.getValue().trim() : null);
            insurance.setInsuranceType(insuranceTypeGroup.getValue());
            insurance.setInsuranceStart(LocalDate.now());

            healthInsuranceService.save(insurance);
            
            Notification.show("Versicherung gespeichert.",
                    3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            clearForm();
            refreshGrid();
        } catch (Exception e) {
            log.error("Fehler beim Speichern der Versicherung", e);
            Notification.show("Fehler beim Speichern: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteInsurance() {
        if (selectedInsurance == null) {
            return;
        }

        try {
            ensureInstitutionContext();
            healthInsuranceService.deactivate(selectedInsurance);
            
            Notification.show("Versicherung deaktiviert.",
                    3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            clearForm();
            refreshGrid();
        } catch (Exception e) {
            log.error("Fehler beim Deaktivieren der Versicherung", e);
            Notification.show("Fehler beim Deaktivieren: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}

