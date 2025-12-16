package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.patient.service.PatientService;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;

@Route(value = "ivom/:id", layout = MainLayout.class)
@PageTitle("IVOM-Behandlungsplan")
@PermitAll
public class TreatmentPlanDetailView extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver {

    private static final Logger log = LoggerFactory.getLogger(TreatmentPlanDetailView.class);

    @Value("${domain.bundesland}")
    private String bundesland;

    private final Button createButton = new Button("Erstellen", VaadinIcon.PLUS.create());
    private final Button cancelButton = new Button("Zurück", VaadinIcon.ARROW_BACKWARD.create());

    private final TreatmentPlanPresenter treatmentPlanPresenter;
    private final TreatmentPlanLayout treatmentPlanLayout;
    private TreatmentPlan treatmentPlan;
    private final ApplicationContext context;
    private final CurrentUser currentUser;
    private final UserAccountRepository userAccountRepository;
    
    @Autowired
    private PatientService patientService;

    public TreatmentPlanDetailView(TreatmentPlanPresenter treatmenPlanPresenter, ApplicationContext context, CurrentUser currentUser, UserAccountRepository userAccountRepository) {
        this.treatmentPlanPresenter = treatmenPlanPresenter;
        this.context = context;
        this.currentUser = currentUser;
        this.userAccountRepository = userAccountRepository;
        setSizeFull();
        // Verhindere horizontales Scrollen der gesamten View, aber erlaube vertikales
        getStyle().set("overflow-x", "hidden");
        // overflow-y nicht setzen - erlaube vertikales Scrollen wenn nötig

        treatmentPlanLayout = new TreatmentPlanLayout(treatmenPlanPresenter, treatmentPlan, context);
        treatmentPlanLayout.setSizeFull();
        expand(treatmentPlanLayout); // Layout soll verfügbaren Platz nutzen
        
        // Binder-Änderungen überwachen für Auto-Save
        treatmentPlanLayout.setBinderChangeListener(() -> autoSaveIfNeeded());

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        
        // Erstellen-Button wird jetzt in der Übersicht-Section angezeigt (nur bei Neuanlage)
        // Hier nur noch Zurück-Button

        cancelButton.addClickListener(event -> {
            // Beim Zurück-Button: Änderungen IMMER persistieren (auch bei Neuanlage)
            // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
            if (treatmentPlanLayout != null) {
                treatmentPlanLayout.writeBean();
                
                ensureInstitutionContext();
                TreatmentPlan treatmentPlanToSave = treatmentPlanLayout.getCurrent();
                if (treatmentPlanToSave != null) {
                    // ID zurücksetzen, falls -1 (für Neuanlage)
                    if (treatmentPlanToSave.getId() != null && treatmentPlanToSave.getId() == -1) {
                        treatmentPlanToSave.setId(null);
                    }
                    // Speichern sowohl bei Neuanlage als auch bei bestehenden Plänen
                    // WICHTIG: Immer speichern, auch wenn hasChanges() false ist,
                    // da Änderungen am Behandlungsgrund und an Notizen möglicherweise nicht erkannt werden
                    treatmenPlanPresenter.saveTreatmentPlanAndTreatments(treatmentPlanToSave,
                            treatmentPlanLayout.getTimeSlotsToCreate());
                    treatmentPlanLayout.resetBinder();
                }
            }
            UI.getCurrent().navigate("ivom");
        });
        buttonBar.add(cancelButton);
        HorizontalLayout dummy = new HorizontalLayout();
        dummy.setWidthFull();
        buttonBar.add(dummy);
        add(buttonBar);
        add(treatmentPlanLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        log.info("TreatmentPlanDetailView.beforeEnter called - Path: {}", event.getLocation().getPath());
        
        // SUPER_ADMIN without institution context should not access treatment plan data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            log.info("Redirecting SUPER_ADMIN to institution management");
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
            return;
        }
        
        // Stelle sicher, dass InstitutionContext gesetzt ist
        ensureInstitutionContext();

        Optional<String> idParameter = event.getRouteParameters().get("id");
        log.info("Route parameter 'id': {}", idParameter.orElse("EMPTY"));
        
        if (idParameter.isEmpty()) {
            log.warn("No id parameter found, forwarding to MainView");
            event.forwardTo(TreatmentPlanMainView.class);
            return;
        }

        try {
            String idString = idParameter.get();
            log.info("Parsing treatment plan id: '{}'", idString);
            
            // Prüfe, ob der Parameter "new" ist (für neue Behandlungspläne)
            boolean isNewRoute = "new".equals(idString);
            Long id = null;
            if (isNewRoute) {
                id = -1L;
                log.info("New treatment plan route detected (id='new')");
            } else {
                id = Long.valueOf(idString);
                log.info("Parsed id: {}", id);
            }
            
            if (-1 == id || isNewRoute) {
                log.info("Creating new treatment plan with id -1");
                TreatmentPlan newTreatmentPlan = new TreatmentPlan();
                newTreatmentPlan.setId(id);
                
                // Prüfe, ob ein patientId Query-Parameter vorhanden ist
                java.util.List<String> patientIdParams = event.getLocation().getQueryParameters()
                        .getParameters().get("patientId");
                log.info("Query parameter 'patientId': {}", patientIdParams);
                
                if (patientIdParams != null && !patientIdParams.isEmpty()) {
                    String patientIdParam = patientIdParams.get(0);
                    if (patientIdParam != null) {
                        try {
                            Integer patientId = Integer.valueOf(patientIdParam);
                            log.info("Loading patient with id: {}", patientId);
                            // Stelle sicher, dass InstitutionContext gesetzt ist, bevor Patient geladen wird
                            ensureInstitutionContext();
                            Patient patient = patientService.findEntityById(patientId);
                            if (patient != null) {
                                newTreatmentPlan.setPatient(patient);
                                log.info("Patient loaded successfully: {} {}", patient.getFirstName(), patient.getLastName());
                            } else {
                                log.warn("Patient not found with id: {}", patientId);
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid patientId format: {}", patientIdParam, e);
                            // Ignoriere ungültige patientId
                        } catch (Exception e) {
                            log.error("Error loading patient: {}", e.getMessage(), e);
                            // Ignoriere Fehler beim Laden des Patienten (z.B. InstitutionContext nicht gesetzt)
                            // Der Behandlungsplan kann trotzdem erstellt werden, nur ohne vorausgewählten Patient
                        }
                    }
                }
                
                try {
                    log.info("Setting current treatment plan in layout");
                    treatmentPlanLayout.setCurrent(newTreatmentPlan);
                    this.treatmentPlan = newTreatmentPlan;
                    // Erstellen-Button wird jetzt in der Übersicht-Section angezeigt
                    log.info("Successfully created new treatment plan with id -1");
                } catch (Exception e) {
                    log.error("Error setting current treatment plan: {}", e.getMessage(), e);
                    e.printStackTrace();
                    // Weiterleiten zur MainView bei Fehler
                    event.forwardTo(TreatmentPlanMainView.class);
                    return;
                }
            } else {
                TreatmentPlan existingTreatmentPlan = treatmentPlanPresenter.getByIdWithFullDetails(id);
                if (existingTreatmentPlan == null) {
                    log.debug("Treatment plan not found with id: {}", id);
                    event.forwardTo(TreatmentPlanMainView.class);
                    return;
                }
                treatmentPlanLayout.setCurrent(existingTreatmentPlan);
                this.treatmentPlan = existingTreatmentPlan;
                // Bei bestehendem Plan: Kein Erstellen-Button mehr in der Button-Bar
            }
        } catch (NumberFormatException nfe) {
            log.debug("Invalid id format: {}", idParameter.orElse("null"));
            // Ungültige ID - weiterleiten zur MainView
            event.forwardTo(TreatmentPlanMainView.class);
        } catch (Exception e) {
            log.error("Error in beforeEnter: {}", e.getMessage(), e);
            // Alle anderen Exceptions - weiterleiten zur MainView
            event.forwardTo(TreatmentPlanMainView.class);
        }
    }

    /**
     * Auto-Save: Speichert Änderungen automatisch, wenn ein bestehender Plan bearbeitet wird.
     * Bei Neuanlage wird nichts gespeichert (Button in Übersicht-Section).
     */
    private void autoSaveIfNeeded() {
        if (treatmentPlan == null || treatmentPlan.getId() == null || treatmentPlan.getId() == -1) {
            // Neuanlage: Nicht automatisch speichern
            return;
        }
        
        // Nur speichern, wenn Änderungen vorhanden sind
        if (treatmentPlanLayout != null && treatmentPlanLayout.hasChanges()) {
            // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
            treatmentPlanLayout.writeBean();
            
            ensureInstitutionContext();
            TreatmentPlan treatmentPlanToSave = treatmentPlanLayout.getCurrent();
            if (treatmentPlanToSave != null) {
                try {
                    treatmentPlanPresenter.saveTreatmentPlanAndTreatments(treatmentPlanToSave,
                            treatmentPlanLayout.getTimeSlotsToCreate());
                    treatmentPlanLayout.resetBinder();
                    // Aktualisiere current, damit weitere Änderungen erkannt werden
                    this.treatmentPlan = treatmentPlanToSave;
                } catch (Exception e) {
                    log.error("Fehler beim Auto-Save: {}", e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     * Dies ist notwendig, da Vaadin Button-Klicks kein BeforeEnterEvent auslösen,
     * sodass der Context möglicherweise nicht gesetzt ist.
     */
    private void ensureInstitutionContext() {
        // Nur setzen, wenn noch nicht gesetzt
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof 
                   UserAccountUserDetailsAdapter adapter) {
            // Authentication wurde aus Session deserialisiert
            if (userAccountRepository != null) {
                try {
                    String username = adapter.getUsername();
                    de.bbajor.pvs.security.domain.UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                    
                    if (userAccount != null && userAccount.getInstitution() != null) {
                        Long institutionId = userAccount.getInstitution().getId();
                        InstitutionContext.setInstitutionId(institutionId);
                    }
                } catch (Exception e) {
                    // Fehler beim Wiederherstellen des Contexts - ignorieren
                }
            }
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        // Beim Verlassen der View: Stelle sicher, dass alle Änderungen geschrieben werden
        if (treatmentPlanLayout != null) {
            // Schreibe alle Änderungen in das Bean (auch wenn hasChanges() false ist)
            treatmentPlanLayout.writeBean();
            
            // Prüfe auf ungespeicherte Änderungen beim Verlassen der View
            // WICHTIG: Prüfe nach writeBean(), damit alle Änderungen erkannt werden
            if (treatmentPlanLayout.hasChanges()) {
                BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
                showUnsavedChangesDialog(() -> {
                    action.proceed();
                });
            } else if (treatmentPlan != null && treatmentPlan.getId() != null && treatmentPlan.getId() != -1) {
                // Auch wenn hasChanges() false ist, speichere bei bestehenden Plänen,
                // da Änderungen am Behandlungsgrund und an Notizen möglicherweise nicht erkannt werden
                // Auto-Save im Hintergrund (ohne Dialog)
                try {
                    ensureInstitutionContext();
                    TreatmentPlan treatmentPlanToSave = treatmentPlanLayout.getCurrent();
                    if (treatmentPlanToSave != null) {
                        treatmentPlanPresenter.saveTreatmentPlanAndTreatments(treatmentPlanToSave,
                                treatmentPlanLayout.getTimeSlotsToCreate());
                        treatmentPlanLayout.resetBinder();
                    }
                } catch (Exception e) {
                    log.warn("Fehler beim Auto-Save beim Verlassen: {}", e.getMessage());
                    // Fehler ignorieren, da Navigation fortgesetzt werden soll
                }
            }
        }
    }
    
    /**
     * Zeigt einen Dialog an, wenn ungespeicherte Änderungen vorhanden sind.
     * @param onContinue Callback, der aufgerufen wird, wenn der Benutzer fortfahren möchte
     */
    private void showUnsavedChangesDialog(Runnable onContinue) {
        com.vaadin.flow.component.dialog.Dialog dialog = new com.vaadin.flow.component.dialog.Dialog();
        dialog.setHeaderTitle("Ungespeicherte Änderungen");
        
        com.vaadin.flow.component.html.Span message = new com.vaadin.flow.component.html.Span(
                "Sie haben ungespeicherte Änderungen. Möchten Sie diese speichern, bevor Sie fortfahren?");
        dialog.add(message);
        
        com.vaadin.flow.component.button.Button saveButton = new com.vaadin.flow.component.button.Button(
                "Speichern", com.vaadin.flow.component.icon.VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            dialog.close();
            // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
            treatmentPlanLayout.writeBean();
            
            // Speichern und dann fortfahren
            ensureInstitutionContext();
            TreatmentPlan treatmentPlan = treatmentPlanLayout.getCurrent();
            if (treatmentPlan != null && treatmentPlan.getId() != null && treatmentPlan.getId() == -1) {
                treatmentPlan.setId(null);
            }
            TreatmentPlan saved = treatmentPlanPresenter.saveTreatmentPlanAndTreatments(treatmentPlan,
                    treatmentPlanLayout.getTimeSlotsToCreate());
            treatmentPlanLayout.resetBinder();
            onContinue.run();
        });
        
        com.vaadin.flow.component.button.Button discardButton = new com.vaadin.flow.component.button.Button(
                "Verwerfen", com.vaadin.flow.component.icon.VaadinIcon.TRASH.create());
        discardButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        discardButton.addClickListener(e -> {
            dialog.close();
            onContinue.run();
        });
        
        com.vaadin.flow.component.button.Button cancelButton = new com.vaadin.flow.component.button.Button(
                "Abbrechen", e -> dialog.close());
        
        dialog.getFooter().add(cancelButton, discardButton, saveButton);
        dialog.open();
    }

}
