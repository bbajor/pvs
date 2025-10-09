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
import com.vaadin.flow.theme.lumo.LumoIcon;

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

    private final Button createButton = new Button();
    private final Button cancelButton = new Button(VaadinIcon.ARROW_BACKWARD.create());

    private final TreatmentPlanPresenter treatmentPlanPresenter;
    private final TreatmentPlanLayout treatmentPlanLayout;
    private TreatmentPlanDto treatmentPlanDto;

    public TreatmentPlanDetailView(TreatmentPlanPresenter ivomDialogPresenter) {
        this.treatmentPlanPresenter = ivomDialogPresenter;
        setSizeFull();

        treatmentPlanLayout = new TreatmentPlanLayout(ivomDialogPresenter, treatmentPlanDto);

        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setWidthFull();

        createButton.addClickListener(event -> {
            TreatmentPlanDto treatmentPlan = treatmentPlanLayout.getTreatmentPlanDto();
            if (treatmentPlan.getId() == -1) {
                treatmentPlan.setId(null);
            }

            TreatmentPlanDto saved = ivomDialogPresenter.saveTreatmentPlanAndTreatments(treatmentPlan,
                    treatmentPlanLayout.getTimeSlotsToCreate());
            UI.getCurrent().navigate("ivom/" + saved.getId());

        });
        buttonBar.add(createButton);

        cancelButton.setTooltipText("Zurück");
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
                treatmentPlanLayout.setTreatmentPlan(newDto);
                this.treatmentPlanDto = newDto;
            } else {
                TreatmentPlanDto dto = treatmentPlanPresenter.getByIdWithFullDetails(id);
                if (dto == null) {
                    event.forwardTo(TreatmentPlanMainView.class);
                    return;
                }
                treatmentPlanLayout.setTreatmentPlan(dto);
                this.treatmentPlanDto = dto;
            }
            updateCreateButton();
        } catch (NumberFormatException nfe) {
            event.forwardTo(TreatmentPlanMainView.class);
        }
    }

    private void updateCreateButton() {
        boolean isNewTreatmentPlan = treatmentPlanDto == null || treatmentPlanDto.getId() == null
                || treatmentPlanDto.getId() == -1;
        createButton
                .setIcon(isNewTreatmentPlan
                        ? VaadinIcon.FILE_ADD.create()
                        : VaadinIcon.EDIT.create());
        createButton.setTooltipText(
                isNewTreatmentPlan
                        ? "Erstellen"
                        : "Aktualisieren");
    }

}
