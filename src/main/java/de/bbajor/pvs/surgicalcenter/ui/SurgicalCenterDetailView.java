package de.bbajor.pvs.surgicalcenter.ui;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import org.springframework.context.ApplicationContext;

import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.model.Institution;
import de.bbajor.pvs.institution.repository.InstitutionRepository;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.presenter.SurgicalCenterListPresenter;
import de.bbajor.pvs.security.AppRoles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.annotation.security.PermitAll;

@Route(value = "surgicalcenter/:id", layout = MainLayout.class)
@PageTitle("OP-Einheit Details")
@PermitAll
public class SurgicalCenterDetailView extends VerticalLayout implements BeforeEnterObserver, BeforeLeaveObserver {

    private final InstitutionRepository institutionRepository;

    @Value("${domain.bundesland}")
    private String bundesland;
    private final SurgicalCenterListPresenter surgicalCenterListPresenter;
    private final ApplicationContext applicationContext;
    private final SurgicalCenterLayout surgicalCenterLayout;
    private Button saveButton;

    public SurgicalCenterDetailView(SurgicalCenterListPresenter surgicalCenterListPresenter,
            InstitutionRepository institutionRepository, ApplicationContext applicationContext) {
        this.surgicalCenterListPresenter = surgicalCenterListPresenter;
        this.institutionRepository = institutionRepository;
        this.applicationContext = applicationContext;
        this.surgicalCenterLayout = new SurgicalCenterLayout(applicationContext);
        
        // Binder-Änderungen überwachen für Auto-Save
        surgicalCenterLayout.setBinderChangeListener(() -> autoSaveIfNeeded());
        
        // Tab-Wechsel überwachen: Speichere beim Verlassen des Stammdaten-Tabs
        surgicalCenterLayout.setTabChangeListener(() -> saveOnTabChange());
        
        // Save-Listener für explizites Speichern (z.B. beim Hinzufügen von OP-Slots)
        surgicalCenterLayout.setSaveListener(() -> saveOnTabChange());

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        
        // Nur "Erstellen"-Button bei Neuanlage, sonst kein Button (Auto-Save beim Zurück)
        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
            surgicalCenterLayout.writeBean();
            
            SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
            if (surgicalCenter != null) {
                // Stelle sicher, dass InstitutionContext gesetzt ist
                ensureInstitutionContext();
                
                // ID zurücksetzen, falls -1 (für Neuanlage)
                if (surgicalCenter.getId() != null && surgicalCenter.getId() == -1) {
                    surgicalCenter.setId(null);
                }
                // Speichern bei Neuanlage
                surgicalCenterListPresenter.saveWithTimeSlots(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
                surgicalCenterLayout.resetBinder();
            }
            UI.getCurrent().navigate("surgicalcenter");
        });
        createButton.setVisible(false); // Wird in updateButtonText() gesetzt
        buttonBar.add(createButton);
        
        // Speichere Button-Referenz für spätere Aktualisierung
        this.saveButton = createButton;
        
        Button cancelButton = new Button("Zurück");
        cancelButton.addClickListener(event -> {
            // Beim Zurück-Button: Änderungen IMMER persistieren (auch bei Neuanlage)
            // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
            if (surgicalCenterLayout != null) {
                surgicalCenterLayout.writeBean();
                
                SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
                if (surgicalCenter != null) {
                    // Stelle sicher, dass InstitutionContext gesetzt ist
                    ensureInstitutionContext();
                    
                    // ID zurücksetzen, falls -1 (für Neuanlage)
                    if (surgicalCenter.getId() != null && surgicalCenter.getId() == -1) {
                        surgicalCenter.setId(null);
                    }
                    // Speichern sowohl bei Neuanlage als auch bei bestehenden Einrichtungen
                    // WICHTIG: Immer speichern, auch wenn hasChanges() false ist,
                    // da Änderungen möglicherweise nicht erkannt werden
                    surgicalCenterListPresenter.saveWithTimeSlots(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
                    surgicalCenterLayout.resetBinder();
                }
            }
            UI.getCurrent().navigate("surgicalcenter");
        });
        buttonBar.add(cancelButton);
        HorizontalLayout dummyLayout2 = new HorizontalLayout();
        dummyLayout2.setWidthFull();
        buttonBar.add(dummyLayout2);
        add(buttonBar);

        add(surgicalCenterLayout);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // SUPER_ADMIN without institution context should not access surgical center data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + AppRoles.SUPER_ADMIN));
        boolean hasInstitutionContext = InstitutionContext.hasInstitution();
        
        if (isSuperAdmin && !hasInstitutionContext) {
            // Redirect SUPER_ADMIN to institution management
            event.forwardTo("admin/institutions");
            return;
        }

        Optional<String> idParameter = event.getRouteParameters().get("id");
        if (idParameter.isEmpty()) {
            event.forwardTo(SurgicalCenterMainView.class);
            return;
        }

        try {
            Integer id = Integer.valueOf(idParameter.get());
            if (-1 == id) {
                // Create new surgical center - set institution from context
                SurgicalCenter newDto = new SurgicalCenter();
                newDto.setId(id);
                
                // Set institution from context if available
                if (hasInstitutionContext) {
                    Long institutionId = InstitutionContext.getInstitutionId();
                    Institution institution = institutionRepository.findById(institutionId)
                            .orElseThrow(() -> new IllegalStateException("Institution not found: " + institutionId));
                    newDto.setInstitution(institution);
                }
                
                surgicalCenterLayout.setBean(newDto);
                updateButtonText(newDto);
            } else {
                SurgicalCenter dto = surgicalCenterListPresenter.getById(id);
                if (dto == null) {
                    event.forwardTo(SurgicalCenterMainView.class);
                    return;
                }
                surgicalCenterLayout.setBean(dto);
                updateButtonText(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(SurgicalCenterMainView.class);
        } catch (IllegalStateException e) {
            // Institution not found or access denied
            event.forwardTo(SurgicalCenterMainView.class);
        }
    }
    
    /**
     * Aktualisiert den Button-Text basierend auf dem Persistenz-Status der Einrichtung.
     */
    private void updateButtonText(SurgicalCenter surgicalCenter) {
        if (saveButton == null) {
            return;
        }
        
        // Wenn ID null oder -1, dann ist es eine neue Einrichtung - zeige "Erstellen"-Button
        if (surgicalCenter == null || surgicalCenter.getId() == null || surgicalCenter.getId() == -1) {
            saveButton.setText("Erstellen");
            saveButton.setVisible(true);
        } else {
            // Bei bestehender Einrichtung: Button ausblenden (Auto-Save beim Zurück)
            saveButton.setVisible(false);
        }
    }
    
    /**
     * Stellt sicher, dass der InstitutionContext gesetzt ist.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof de.bbajor.pvs.institution.security.InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter adapter) {
            try {
                String username = adapter.getUsername();
                de.bbajor.pvs.security.domain.UserAccountRepository userAccountRepository = 
                    applicationContext.getBean(de.bbajor.pvs.security.domain.UserAccountRepository.class);
                de.bbajor.pvs.security.domain.UserAccount userAccount = 
                    userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                }
            } catch (Exception e) {
                // Log error but continue
            }
        }
    }
    
    /**
     * Auto-Save: Speichert Änderungen automatisch, wenn eine bestehende Einrichtung bearbeitet wird.
     * Bei Neuanlage wird nichts gespeichert (Button bleibt sichtbar).
     */
    private void autoSaveIfNeeded() {
        SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
        if (surgicalCenter == null || surgicalCenter.getId() == null || surgicalCenter.getId() == -1) {
            // Neuanlage: Nicht automatisch speichern
            return;
        }
        
        // Nur speichern, wenn Änderungen vorhanden sind
        if (surgicalCenterLayout.hasChanges()) {
            try {
                // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
                surgicalCenterLayout.writeBean();
                
                ensureInstitutionContext();
                surgicalCenterListPresenter.saveWithTimeSlots(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
                surgicalCenterLayout.resetBinder();
            } catch (Exception e) {
                // Fehler beim Auto-Save - ignorieren (wird beim Zurück nochmal versucht)
            }
        }
    }
    
    /**
     * Speichert Änderungen beim Tab-Wechsel (Verlassen des Stammdaten-Tabs).
     * Wird aufgerufen, wenn der Benutzer vom "Stammdaten"-Tab zum "OP-Slots"-Tab wechselt.
     * Wird auch beim Hinzufügen von OP-Slots aufgerufen, wenn das SurgicalCenter noch nicht persistiert ist.
     */
    private void saveOnTabChange() {
        if (surgicalCenterLayout == null) {
            return;
        }
        
        // Prüfe, ob Änderungen vorhanden sind (auch bei Neuanlage)
        boolean hasChanges = surgicalCenterLayout.hasChanges();
        SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
        
        // Bei Neuanlage immer speichern, auch wenn keine Änderungen erkannt werden
        // (da hasChanges() bei Neuanlage möglicherweise false zurückgibt)
        boolean isNew = surgicalCenter == null || surgicalCenter.getId() == null || surgicalCenter.getId() == -1;
        
        if (!hasChanges && !isNew) {
            return;
        }
        
        if (surgicalCenter == null) {
            return;
        }
        
        try {
            // Stelle sicher, dass alle Änderungen aus dem Binder geschrieben werden
            surgicalCenterLayout.writeBean();
            
            // Stelle sicher, dass InstitutionContext gesetzt ist
            ensureInstitutionContext();
            
            // ID zurücksetzen, falls -1 (für Neuanlage)
            if (surgicalCenter.getId() != null && surgicalCenter.getId() == -1) {
                surgicalCenter.setId(null);
            }
            
            // Speichern sowohl bei Neuanlage als auch bei bestehenden Einrichtungen
            surgicalCenterListPresenter.saveWithTimeSlots(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
            surgicalCenterLayout.resetBinder();
        } catch (Exception e) {
            // Fehler beim Speichern - ignorieren (wird beim Zurück nochmal versucht)
            // Optional: Notification anzeigen
        }
    }
    
    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        // Beim Verlassen der View: Stelle sicher, dass alle Änderungen geschrieben werden
        if (surgicalCenterLayout != null) {
            // Schreibe alle Änderungen in das Bean (auch wenn hasChanges() false ist)
            surgicalCenterLayout.writeBean();
            
            SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
            
            // Prüfe auf ungespeicherte Änderungen beim Verlassen der View
            // WICHTIG: Prüfe nach writeBean(), damit alle Änderungen erkannt werden
            if (surgicalCenterLayout.hasChanges()) {
                BeforeLeaveEvent.ContinueNavigationAction action = event.postpone();
                showUnsavedChangesDialog(() -> {
                    action.proceed();
                });
            } else if (surgicalCenter != null && surgicalCenter.getId() != null && surgicalCenter.getId() != -1) {
                // Auch wenn hasChanges() false ist, speichere bei bestehenden Einrichtungen,
                // da Änderungen möglicherweise nicht erkannt werden
                // Auto-Save im Hintergrund (ohne Dialog)
                try {
                    ensureInstitutionContext();
                    surgicalCenterListPresenter.saveWithTimeSlots(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
                    surgicalCenterLayout.resetBinder();
                } catch (Exception e) {
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
            surgicalCenterLayout.writeBean();
            
            // Speichern und dann fortfahren
            ensureInstitutionContext();
            SurgicalCenter surgicalCenter = surgicalCenterLayout.getBean();
            if (surgicalCenter != null) {
                if (surgicalCenter.getId() != null && surgicalCenter.getId() == -1) {
                    surgicalCenter.setId(null);
                }
                surgicalCenterListPresenter.saveWithTimeSlots(surgicalCenter, surgicalCenterLayout.getTimeSlotsToCreate());
                surgicalCenterLayout.resetBinder();
            }
            onContinue.run();
        });
        
        com.vaadin.flow.component.button.Button discardButton = new com.vaadin.flow.component.button.Button(
                "Verwerfen", com.vaadin.flow.component.icon.VaadinIcon.TRASH.create());
        discardButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
        discardButton.addClickListener(e -> {
            dialog.close();
            surgicalCenterLayout.resetBinder();
            onContinue.run();
        });
        
        dialog.getFooter().add(discardButton, saveButton);
        dialog.open();
    }
}
