package de.bbajor.pvs.ivomplan.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.ivomplan.controller.IvomDialogPresenter;
import de.bbajor.pvs.ivomplan.controller.IvomChangeListener;

public class IvomPlanDialog extends Dialog {

    private final IvomDialogPresenter dialogPresenter;
    private List<IvomChangeListener> listeners;

    public IvomPlanDialog(IvomDialogPresenter dialogPresenter) {
        this.dialogPresenter = dialogPresenter;
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        IvomPlanForm ivomForm = new IvomPlanForm(dialogPresenter.getPatients(), dialogPresenter.getDrugs(),
                dialogPresenter.getSurgeryUnits());
        add(ivomForm);
    }

    public void addChangeListener(IvomPlanView ivomView) {
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
