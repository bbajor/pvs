package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.TreatmentPlanPresenter;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentPlanDto;

public class TreatmentPlanDialog extends Dialog {

    private List<TreatmentPlanChangeListener> listeners;

    public TreatmentPlanDialog(TreatmentPlanPresenter dialogPresenter, TreatmentPlanDto dto) {
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        TreatmentPlanLayout treatmentPlanForm = new TreatmentPlanLayout(dialogPresenter, dto);
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
