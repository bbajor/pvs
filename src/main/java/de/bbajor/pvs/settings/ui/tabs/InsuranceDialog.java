package de.bbajor.pvs.settings.ui.tabs;

import java.time.LocalDate;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.service.HealthInsuranceService;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import lombok.extern.slf4j.Slf4j;

/**
 * Dialog zum Erstellen und Bearbeiten von Versicherungen.
 * Angelehnt an den Aufbau der Benutzerverwaltung.
 */
@Slf4j
public class InsuranceDialog extends Dialog {

    private final HealthInsuranceService healthInsuranceService;
    private final InstitutionRepository institutionRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUser currentUser;
    private final HealthInsurance insurance;
    private Runnable onSaveCallback;

    private TextField costCarrierNameField;
    private TextField billingCarrierNameField;
    private TextField costCarrierIdField;
    private TextField billingCarrierIdField;
    private RadioButtonGroup<String> insuranceTypeGroup;
    private Button saveButton;

    public InsuranceDialog(
            HealthInsuranceService healthInsuranceService,
            InstitutionRepository institutionRepository,
            UserAccountRepository userAccountRepository,
            CurrentUser currentUser,
            HealthInsurance insurance) {
        this.healthInsuranceService = healthInsuranceService;
        this.institutionRepository = institutionRepository;
        this.userAccountRepository = userAccountRepository;
        this.currentUser = currentUser;
        this.insurance = insurance != null ? insurance : new HealthInsurance();

        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("520px");
        setCloseOnOutsideClick(false);

        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        initializeDialog();
    }

    private void initializeDialog() {
        String titleText = insurance.getId() != null ? "Versicherung bearbeiten" : "Neue Versicherung";
        H3 title = new H3(titleText);

        Span hint = new Span(
                "Hinweis: Vor dem Speichern wird geprüft, ob bereits eine Versicherung mit gleichem " +
                        "Kostenträgernamen, Kostenträger-ID oder Abrechnungsstellen-ID existiert.");
        hint.getStyle().set("font-size", "var(--lumo-font-size-s)");
        hint.getStyle().set("color", "var(--lumo-secondary-text-color)");

        FormLayout formLayout = createFormLayout();
        HorizontalLayout buttonLayout = createButtonLayout();

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.add(title, hint, formLayout, buttonLayout);

        add(content);

        if (insurance.getId() != null) {
            loadInsuranceData();
        }
    }

    private FormLayout createFormLayout() {
        FormLayout layout = new FormLayout();
        layout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        costCarrierNameField = new TextField("Kostenträger");
        costCarrierNameField.setRequired(true);
        costCarrierNameField.setRequiredIndicatorVisible(true);
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
        insuranceTypeGroup.setRequiredIndicatorVisible(true);

        layout.add(costCarrierNameField, 2);
        layout.add(billingCarrierNameField, 2);
        layout.add(costCarrierIdField, billingCarrierIdField);
        layout.add(insuranceTypeGroup, 2);

        return layout;
    }

    private HorizontalLayout createButtonLayout() {
        HorizontalLayout layout = new HorizontalLayout();
        layout.setWidthFull();
        layout.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        saveButton = new Button("Speichern", event -> saveInsurance());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        layout.add(saveButton);
        return layout;
    }

    private void loadInsuranceData() {
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
    }

    private void saveInsurance() {
        String costCarrierName = costCarrierNameField.getValue();
        if (costCarrierName == null || costCarrierName.trim().isEmpty()) {
            showError("Bitte geben Sie einen Kostenträger ein");
            return;
        }

        try {
            ensureInstitutionContext();

            Long institutionId = InstitutionContext.getInstitutionId();
            if (institutionId == null) {
                showError("Keine Institution ausgewählt");
                return;
            }

            insurance.setCostCarrierName(costCarrierName.trim());
            insurance.setBillingCarrierName(trimOrNull(billingCarrierNameField.getValue()));
            insurance.setCostCarrierId(trimOrNull(costCarrierIdField.getValue()));
            insurance.setBillingCarrierId(trimOrNull(billingCarrierIdField.getValue()));
            insurance.setInsuranceType(insuranceTypeGroup.getValue());

            if (insurance.getInsuranceStart() == null) {
                insurance.setInsuranceStart(LocalDate.now());
            }

            healthInsuranceService.save(insurance);

            showSuccess("Versicherung wurde erfolgreich gespeichert");

            if (onSaveCallback != null) {
                onSaveCallback.run();
            }

            close();
        } catch (Exception e) {
            showError("Fehler beim Speichern: " + e.getMessage());
        }
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 5000, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist, analog zur Benutzerverwaltung.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            try {
                String username = adapter.getUsername();
                UserAccount account = userAccountRepository.findByUsername(username).orElse(null);

                if (account != null && account.getInstitution() != null) {
                    Long institutionId = account.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, account.getInstitution().getInstitutionCode());
                } else {
                    log.warn("UserAccount has no institution - cannot set InstitutionContext");
                }
            } catch (Exception e) {
                log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
            }
        } else if (authentication != null && currentUser != null) {
            currentUser.get().ifPresent(appUser -> {
                String preferredUsername = appUser.getPreferredUsername();
                if (preferredUsername != null) {
                    UserAccount account = userAccountRepository.findByUsername(preferredUsername).orElse(null);
                    if (account != null && account.getInstitution() != null) {
                        Long institutionId = account.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                        log.debug("InstitutionContext restored from CurrentUser: {} (institution code: {})",
                                institutionId, account.getInstitution().getInstitutionCode());
                    }
                }
            });
        } else {
            log.debug("Authentication type: {}, Principal type: {} - cannot set InstitutionContext",
                    authentication != null ? authentication.getClass().getSimpleName() : "null",
                    authentication != null && authentication.getPrincipal() != null
                            ? authentication.getPrincipal().getClass().getSimpleName() : "null");
        }
    }
}

