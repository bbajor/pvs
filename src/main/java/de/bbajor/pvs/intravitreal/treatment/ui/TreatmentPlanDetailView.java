package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

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

    public TreatmentPlanDetailView(TreatmentPlanPresenter ivomDialogPresenter) {
        this.treatmentPlanPresenter = ivomDialogPresenter;
        setSizeFull();

        treatmentPlanLayout = new TreatmentPlanLayout(ivomDialogPresenter, treatmentPlan);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();

        createButton.addClickListener(event -> {
            TreatmentPlan treatmentPlan = treatmentPlanLayout.getCurrent();
            if (treatmentPlan.getId() == -1) {
                treatmentPlan.setId(null);
            }

            TreatmentPlan saved = ivomDialogPresenter.saveTreatmentPlanAndTreatments(treatmentPlan,
                    treatmentPlanLayout.getTimeSlotsToCreate());
            UI.getCurrent().navigate("ivom/" + saved.getId());

        });
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
    }

}
