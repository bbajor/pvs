package de.bbajor.pvs.intravitreal.treatment.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.intravitreal.treatment.controller.IvomChangeListener;
import de.bbajor.pvs.intravitreal.treatment.controller.IvomPlanPresenter;

public class IvomPlanDialog extends Dialog {

    private final IvomPlanPresenter dialogPresenter;
    private List<IvomChangeListener> listeners;

    public IvomPlanDialog(IvomPlanPresenter dialogPresenter) {
        this.dialogPresenter = dialogPresenter;
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        IvomPlanLayout ivomForm = new IvomPlanLayout(dialogPresenter);
        add(ivomForm);
    }

    public void addChangeListener(IvomPlanMainView ivomView) {
        if (listeners == null) {
            listeners = new ArrayList<>();
        }
        if (listeners.contains(ivomView)) {
            return;
        }
        listeners.add(ivomView);
    }

    public void loadIvomById(Long id) {
        dialogPresenter.loadIvomById(id);
    }

}
