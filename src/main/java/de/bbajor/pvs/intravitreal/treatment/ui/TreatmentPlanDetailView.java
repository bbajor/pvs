package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.Optional;

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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;
import de.bbajor.pvs.security.AppRoles;
import de.bbajor.pvs.security.CurrentUser;
import jakarta.annotation.security.PermitAll;

@Route(value = "ivom/:id", layout = MainLayout.class)
@PageTitle("IVOM-Behandlungsplan")
@PermitAll
public class TreatmentPlanDetailView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${domain.bundesland}")
    private String bundesland;

    private final Button createButton = new Button("Erstellen", VaadinIcon.PLUS.create());
    private final Button cancelButton = new Button("Abbrechen", VaadinIcon.ARROW_BACKWARD.create());

    private final TreatmentPlanPresenter treatmentPlanPresenter;
    private final TreatmentPlanLayout treatmentPlanLayout;
    private TreatmentPlan treatmentPlan;
    private final ApplicationContext context;
    private final CurrentUser currentUser;

    public TreatmentPlanDetailView(TreatmentPlanPresenter treatmenPlanPresenter, ApplicationContext context, CurrentUser currentUser) {
        this.treatmentPlanPresenter = treatmenPlanPresenter;
        this.context = context;
        this.currentUser = currentUser;
        setSizeFull();
        // Verhindere horizontales Scrollen der gesamten View, aber erlaube vertikales
        getStyle().set("overflow-x", "hidden");
        // overflow-y nicht setzen - erlaube vertikales Scrollen wenn nötig

        treatmentPlanLayout = new TreatmentPlanLayout(treatmenPlanPresenter, treatmentPlan, context);
        treatmentPlanLayout.setSizeFull();
        expand(treatmentPlanLayout); // Layout soll verfügbaren Platz nutzen

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();
        
        // Prüfe, ob Benutzer berechtigt ist, Termine zu buchen
        boolean canBook = currentUser.getPrincipal()
                .map(principal -> {
                    return principal.getAuthorities().stream()
                            .anyMatch(auth -> {
                                String authority = auth.getAuthority();
                                return authority.equals("ROLE_" + AppRoles.ADMIN) ||
                                        authority.equals("ROLE_" + AppRoles.DOCTOR) ||
                                        authority.equals("ROLE_" + AppRoles.TECH_USER);
                            });
                })
                .orElse(false);

        createButton.addClickListener(event -> {
            TreatmentPlan treatmentPlan = treatmentPlanLayout.getCurrent();
            if (treatmentPlan.getId() == -1) {
                treatmentPlan.setId(null);
            }

            TreatmentPlan saved = treatmenPlanPresenter.saveTreatmentPlanAndTreatments(treatmentPlan,
                    treatmentPlanLayout.getTimeSlotsToCreate());
            UI.getCurrent().navigate("ivom/" + saved.getId());

        });
        
        // Button nur für berechtigte Rollen aktivieren
        createButton.setEnabled(canBook);
        if (!canBook) {
            createButton.setTooltipText("Sie benötigen die Rolle ADMIN, DOCTOR oder TECH_USER, um Termine zu buchen oder zu löschen");
        }
        
        buttonBar.add(createButton);

        cancelButton.addClickListener(event -> {
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

        Optional<String> idParameter = event.getRouteParameters().get("id");
        if (idParameter.isEmpty()) {
            event.forwardTo(TreatmentPlanMainView.class);
            return;
        }

        try {
            Long id = Long.valueOf(idParameter.get());
            if (-1 == id) {
                TreatmentPlan newTreatmentPlan = new TreatmentPlan();
                newTreatmentPlan.setId(id);
                treatmentPlanLayout.setCurrent(newTreatmentPlan);
                this.treatmentPlan = newTreatmentPlan;
            } else {
                TreatmentPlan existingTreatmentPlan = treatmentPlanPresenter.getByIdWithFullDetails(id);
                if (existingTreatmentPlan == null) {
                    event.forwardTo(TreatmentPlanMainView.class);
                    return;
                }
                treatmentPlanLayout.setCurrent(existingTreatmentPlan);
                this.treatmentPlan = existingTreatmentPlan;
            }
            updateCreateButton();
        } catch (NumberFormatException nfe) {
            event.forwardTo(TreatmentPlanMainView.class);
        }
    }

    private void updateCreateButton() {
        boolean isNewTreatmentPlan = treatmentPlan == null || treatmentPlan.getId() == null
                || treatmentPlan.getId() == -1;
        createButton.setText(isNewTreatmentPlan ? "Erstellen" : "Aktualisieren");
        
        // Berechtigung prüfen
        boolean canBook = currentUser.getPrincipal()
                .map(principal -> {
                    return principal.getAuthorities().stream()
                            .anyMatch(auth -> {
                                String authority = auth.getAuthority();
                                return authority.equals("ROLE_" + AppRoles.ADMIN) ||
                                        authority.equals("ROLE_" + AppRoles.DOCTOR) ||
                                        authority.equals("ROLE_" + AppRoles.TECH_USER);
                            });
                })
                .orElse(false);
        createButton.setEnabled(canBook);
    }

}
