package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import de.bbajor.pvs.base.ui.view.MainLayout;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;
import jakarta.annotation.security.PermitAll;

@Route(value = "ivom/:id", layout = MainLayout.class)
@PageTitle("IVOM-Behandlungsplan")
@PermitAll
public class TreatmentPlanDetailView extends VerticalLayout implements BeforeEnterObserver {

    @Value("${domain.bundesland}")
    private String bundesland;

    private final TreatmentPlanPresenter treatmentPlanPresenter;
    private final TreatmentPlanLayout treatmentPlanLayout;

    public TreatmentPlanDetailView(TreatmentPlanPresenter ivomDialogPresenter) {
        this.treatmentPlanPresenter = ivomDialogPresenter;

        treatmentPlanLayout = new TreatmentPlanLayout(ivomDialogPresenter);
        setSizeFull();

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();

        Button createButton = new Button("Erstellen");
        createButton.addClickListener(event -> {
            TreatmentPlanDto treatmentPlan = treatmentPlanLayout.geIntravitrealTreatmentDto();
            if (treatmentPlan.getId() == -1) {
                treatmentPlan.setId(null);
            }
            ivomDialogPresenter.setWorkingCopy(treatmentPlan);
            TreatmentPlanDto savedTreatmentPlanDto = ivomDialogPresenter.saveNewTreatments(treatmentPlanLayout.getTimeSlotsToCreate());
            UI.getCurrent().navigate("ivom/" + savedTreatmentPlanDto.getId());

        });
        buttonBar.add(createButton);

        Button cancelButton = new Button("Zurück");
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
                TreatmentPlanDto newDto = new TreatmentPlanDto();
                newDto.setId(id);
                treatmentPlanLayout.setBean(newDto);
            } else {
                TreatmentPlanDto dto = treatmentPlanPresenter.getById(id);
                treatmentPlanPresenter.setWorkingCopy(dto);
                if (dto == null) {
                    event.forwardTo(TreatmentPlanMainView.class);
                    return;
                }
                treatmentPlanLayout.setBean(dto);
            }
        } catch (NumberFormatException nfe) {
            event.forwardTo(TreatmentPlanMainView.class);
        }
    }

}
