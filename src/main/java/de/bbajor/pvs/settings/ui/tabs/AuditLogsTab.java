package de.bbajor.pvs.settings.ui.tabs;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentAuditLog;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentAuditLogRepository;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import jakarta.annotation.security.RolesAllowed;

/**
 * Tab für die Anzeige von Audit-Logs auf Institutionsebene.
 * Nur OWNER, INSTITUTION_ADMIN und TECH_USER können diese Sicht sehen.
 */
@Component
@RolesAllowed({ "ROLE_OWNER", "ROLE_INSTITUTION_ADMIN", "ROLE_TECH_USER" })
public class AuditLogsTab extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(AuditLogsTab.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    private TreatmentAuditLogRepository treatmentAuditLogRepository;
    
    @Autowired
    private UserAccountRepository userAccountRepository;

    private Grid<TreatmentAuditLog> auditLogGrid;
    private ComboBox<String> actionTypeFilter;
    private TextField actorFilter;
    private List<TreatmentAuditLog> allAuditLogs;

    public AuditLogsTab() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        // Header
        Span title = new Span("Audit-Logs");
        title.getStyle().set("font-size", "var(--lumo-font-size-xl)");
        title.getStyle().set("font-weight", "600");
        add(title);

        // Filter-Bereich
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setSpacing(true);
        filterLayout.setWidthFull();

        actionTypeFilter = new ComboBox<>("Aktionstyp");
        actionTypeFilter.setItems("Alle", "Erstellen", "Dokumentieren", "Zweitprüfung", "Löschen");
        actionTypeFilter.setValue("Alle");
        actionTypeFilter.setWidth("200px");
        actionTypeFilter.addValueChangeListener(e -> applyFilters());

        actorFilter = new TextField("Benutzer");
        actorFilter.setPlaceholder("Benutzer filtern...");
        actorFilter.setWidth("200px");
        actorFilter.addValueChangeListener(e -> applyFilters());

        filterLayout.add(actionTypeFilter, actorFilter);
        filterLayout.setFlexGrow(1, actorFilter);
        add(filterLayout);

        // Grid für Audit-Logs
        auditLogGrid = new Grid<>(TreatmentAuditLog.class, false);
        auditLogGrid.setSizeFull();

        auditLogGrid.addColumn(log -> {
            if (log.getTreatment() != null && log.getTreatment().getTreatmentPlan() != null 
                    && log.getTreatment().getTreatmentPlan().getPatient() != null) {
                var patient = log.getTreatment().getTreatmentPlan().getPatient();
                String name = (patient.getLastName() != null ? patient.getLastName() : "") + 
                              (patient.getFirstName() != null ? ", " + patient.getFirstName() : "");
                return name.startsWith(", ") ? name.substring(2) : (name.isEmpty() ? "-" : name);
            }
            return "-";
        }).setHeader("Patient").setAutoWidth(true).setResizable(true);

        auditLogGrid.addColumn(log -> {
            if (log.getTreatment() != null && log.getTreatment().getDate() != null) {
                return log.getTreatment().getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            }
            return "-";
        }).setHeader("Behandlungsdatum").setAutoWidth(true).setResizable(true);

        auditLogGrid.addColumn(log -> {
            if (log.getActionType() != null) {
                return switch (log.getActionType()) {
                    case CREATE -> "Erstellen";
                    case APPROVE -> "Dokumentieren";
                    case APPROVE_SECOND -> "Zweitprüfung";
                    case DELETE -> "Löschen";
                };
            }
            return "-";
        }).setHeader("Aktion").setAutoWidth(true).setResizable(true);

        auditLogGrid.addColumn(log -> {
            if (log.getActionTimestamp() != null) {
                return log.getActionTimestamp().format(DATE_TIME_FORMATTER);
            }
            return "-";
        }).setHeader("Zeitstempel").setAutoWidth(true).setResizable(true);

        auditLogGrid.addColumn(log -> {
            if (log.getActorUserName() != null) {
                return log.getActorUserName();
            }
            return log.getActorUserId() != null ? log.getActorUserId() : "-";
        }).setHeader("Benutzer").setAutoWidth(true).setResizable(true);

        auditLogGrid.addColumn(log -> {
            if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                return log.getDetails();
            }
            return "-";
        }).setHeader("Details").setAutoWidth(true).setResizable(true);

        add(auditLogGrid);
        expand(auditLogGrid);

        // Lade Daten
        loadAuditLogs();
    }

    @PreAuthorize("hasAnyRole('OWNER', 'INSTITUTION_ADMIN', 'TECH_USER')")
    private void loadAuditLogs() {
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();
        
        if (!InstitutionContext.hasInstitution()) {
            log.warn("InstitutionContext not set - cannot load audit logs");
            allAuditLogs = new ArrayList<>();
            auditLogGrid.setItems(allAuditLogs);
            return;
        }
        
        Long institutionId = InstitutionContext.getInstitutionId();
        if (institutionId == null) {
            log.warn("InstitutionContext has no institution ID - cannot load audit logs");
            allAuditLogs = new ArrayList<>();
            auditLogGrid.setItems(allAuditLogs);
            return;
        }
        
        log.debug("Loading audit logs for institution: {}", institutionId);

        // Optimierte Query: Lade alle Audit-Logs direkt über Treatment -> TreatmentPlan -> Institution
        // Verwende JpaSpecificationExecutor für flexible Filterung
        allAuditLogs = treatmentAuditLogRepository.findAll(
                (root, query, cb) -> {
                    // Join zu Treatment
                    var treatmentJoin = root.join("treatment");
                    // Join zu TreatmentPlan
                    var treatmentPlanJoin = treatmentJoin.join("treatmentPlan");
                    // Join zu Institution
                    var institutionJoin = treatmentPlanJoin.join("institution");
                    // Filter nach Institution ID
                    return cb.equal(institutionJoin.get("id"), institutionId);
                },
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "actionTimestamp")
        );

        applyFilters();
        
        log.debug("Loaded {} audit logs for institution {}", allAuditLogs.size(), institutionId);
    }
    
    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set, especially for Institutionsadmins.
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
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
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, userAccount.getInstitution().getInstitutionCode());
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

    private void applyFilters() {
        if (allAuditLogs == null) {
            return;
        }

        List<TreatmentAuditLog> filtered = allAuditLogs.stream()
                .filter(log -> {
                    // Filter nach Aktionstyp
                    String actionType = actionTypeFilter.getValue();
                    if (actionType != null && !actionType.equals("Alle")) {
                        TreatmentAuditLog.ActionType expectedType = switch (actionType) {
                            case "Erstellen" -> TreatmentAuditLog.ActionType.CREATE;
                            case "Dokumentieren" -> TreatmentAuditLog.ActionType.APPROVE;
                            case "Zweitprüfung" -> TreatmentAuditLog.ActionType.APPROVE_SECOND;
                            case "Löschen" -> TreatmentAuditLog.ActionType.DELETE;
                            default -> null;
                        };
                        if (expectedType != null && log.getActionType() != expectedType) {
                            return false;
                        }
                    }

                    // Filter nach Benutzer
                    String actorFilterValue = actorFilter.getValue();
                    if (actorFilterValue != null && !actorFilterValue.trim().isEmpty()) {
                        String searchTerm = actorFilterValue.toLowerCase();
                        String actorName = log.getActorUserName() != null ? log.getActorUserName().toLowerCase() : "";
                        String actorId = log.getActorUserId() != null ? log.getActorUserId().toLowerCase() : "";
                        if (!actorName.contains(searchTerm) && !actorId.contains(searchTerm)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        auditLogGrid.setItems(filtered);
    }

    public void refresh() {
        loadAuditLogs();
    }
}

