package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.icon.VaadinIcon;

import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.model.TreatmentPlan;

public class TreatmentPlanDialog extends Dialog {

    private List<TreatmentPlanChangeListener> listeners;

    public TreatmentPlanDialog(TreatmentPlanPresenter dialogPresenter, TreatmentPlan treatmentPlan, ApplicationContext context) {
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);
        
        // X-Icon im Header hinzufügen
        Button closeIconButton = new Button(VaadinIcon.CLOSE.create(), e -> close());
        closeIconButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeIconButton.getStyle().set("margin-left", "auto");
        getHeader().add(closeIconButton);

        TreatmentPlanLayout treatmentPlanForm = new TreatmentPlanLayout(dialogPresenter, treatmentPlan, context);
        add(treatmentPlanForm);
    }

    public void addChangeListener(TreatmentPlanMainView mainView) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        if (listeners.contains(mainView)) {
            return;
        }
        listeners.add(mainView);
    }

}
