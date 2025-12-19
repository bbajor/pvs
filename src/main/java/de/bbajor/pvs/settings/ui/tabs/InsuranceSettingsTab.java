package de.bbajor.pvs.settings.ui.tabs;

import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.theme.lumo.LumoUtility;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.patient.model.HealthInsurance;
import de.bbajor.pvs.patient.service.HealthInsuranceService;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    private final InstitutionRepository institutionRepository;
    private final UserAccountRepository userAccountRepository;
    private final CurrentUser currentUser;

    private Grid<HealthInsurance> insuranceGrid;
    private Button createButton;
    
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

        // Button-Sektion oberhalb des Grids, analog Benutzerverwaltung
        createButton = new Button("Erstellen", e -> openInsuranceDialog(null));
        createButton.setIcon(VaadinIcon.PLUS.create());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout buttonSection = new HorizontalLayout(createButton);
        buttonSection.setSpacing(true);
        buttonSection.setPadding(true);

        // Grid
        insuranceGrid = new Grid<>(HealthInsurance.class, false);
        insuranceGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);

        // Versicherungsinformationen in einer "Business"-Spalte
        insuranceGrid.addColumn(new ComponentRenderer<>(insurance -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);

            String costName = insurance.getCostCarrierName() != null ? insurance.getCostCarrierName() : "-";
            Span costSpan = new Span(costName);
            costSpan.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            layout.add(costSpan);

            String billingName = insurance.getBillingCarrierName() != null ? insurance.getBillingCarrierName() : "";
            if (!billingName.isEmpty()) {
                Span billingSpan = new Span(billingName);
                billingSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                layout.add(billingSpan);
            }

            String ids = "";
            if (insurance.getCostCarrierId() != null && !insurance.getCostCarrierId().isBlank()) {
                ids = "KT-ID: " + insurance.getCostCarrierId();
            }
            if (insurance.getBillingCarrierId() != null && !insurance.getBillingCarrierId().isBlank()) {
                if (!ids.isEmpty()) {
                    ids += " · ";
                }
                ids += "AST-ID: " + insurance.getBillingCarrierId();
            }
            if (!ids.isEmpty()) {
                Span idsSpan = new Span(ids);
                idsSpan.addClassNames(LumoUtility.TextColor.SECONDARY, LumoUtility.FontSize.SMALL);
                layout.add(idsSpan);
            }

            return layout;
        })).setHeader("Versicherung").setAutoWidth(true);

        insuranceGrid.addColumn(new ComponentRenderer<>(insurance -> {
            String type = insurance.getInsuranceType() != null ? insurance.getInsuranceType() : "-";
            Span typeSpan = new Span(type);
            return typeSpan;
        })).setHeader("Versicherungsart").setAutoWidth(true);

        // Aktionsspalte analog zur Benutzerverwaltung (Bearbeiten / Deaktivieren)
        insuranceGrid.addComponentColumn(insurance -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button editButton = new Button("Bearbeiten", e -> openInsuranceDialog(insurance));
            editButton.addThemeVariants(ButtonVariant.LUMO_SMALL);

            Button deactivateButton = new Button("Deaktivieren", e -> confirmDeactivate(insurance));
            deactivateButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            actions.add(editButton, deactivateButton);
            return actions;
        }).setHeader("Aktionen").setAutoWidth(true);

        insuranceGrid.setSizeFull();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        add(title, buttonSection, insuranceGrid);
        expand(insuranceGrid);

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
        } else {
            log.debug("Authentication type: {}, Principal type: {} - cannot set InstitutionContext",
                    authentication != null ? authentication.getClass().getSimpleName() : "null",
                    authentication != null && authentication.getPrincipal() != null
                            ? authentication.getPrincipal().getClass().getSimpleName() : "null");
        }
    }

    private void refreshGrid() {
        ensureInstitutionContext();
        allInsurances = healthInsuranceService.findAllForCurrentInstitution();
        insuranceGrid.setItems(allInsurances);
    }

    private void openInsuranceDialog(HealthInsurance insurance) {
        InsuranceDialog dialog = new InsuranceDialog(
                healthInsuranceService,
                institutionRepository,
                userAccountRepository,
                currentUser,
                insurance
        );
        dialog.setOnSaveCallback(this::refreshGrid);
        dialog.open();
    }

    private void confirmDeactivate(HealthInsurance insurance) {
        if (insurance == null || insurance.getId() == null) {
            return;
        }

        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Versicherung deaktivieren");
        confirmDialog.setText("Möchten Sie diese Versicherung wirklich deaktivieren? Der Eintrag wird entfernt und kann nicht ohne Neuanlage wiederhergestellt werden.");
        confirmDialog.setConfirmText("Deaktivieren");
        confirmDialog.setCancelText("Abbrechen");
        confirmDialog.setConfirmButtonTheme("error primary");
        confirmDialog.addConfirmListener(e -> deactivateInsurance(insurance));
        confirmDialog.open();
    }

    private void deactivateInsurance(HealthInsurance insurance) {
        try {
            ensureInstitutionContext();
            healthInsuranceService.deactivate(insurance);

            Notification.show("Versicherung deaktiviert.",
                    3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            refreshGrid();
        } catch (Exception e) {
            log.error("Fehler beim Deaktivieren der Versicherung", e);
            Notification.show("Fehler beim Deaktivieren: " + e.getMessage(),
                    5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
