package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;

public class TreatmentPlanDialog extends Dialog {

    private final TreatmentPlanPresenter dialogPresenter;
    private List<TreatmentPlanChangeListener> listeners;

    public TreatmentPlanDialog(TreatmentPlanPresenter dialogPresenter) {
        this.dialogPresenter = dialogPresenter;
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        TreatmentPlanLayout treatmentPlanForm = new TreatmentPlanLayout(dialogPresenter);
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

    public void loadIvomById(Long id) {
        dialogPresenter.loadTreatmentPlanById(id);
    }

}
