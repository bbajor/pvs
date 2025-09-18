package de.bbajor.pvs.ivom.ui;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.ivom.controller.IvomChangeListener;
import de.bbajor.pvs.ivom.controller.IvomDialogPresenter;

public class IvomDialog extends Dialog {

    private final IvomDialogPresenter dialogPresenter;
    private List<IvomChangeListener> listeners;

    public IvomDialog(IvomDialogPresenter dialogPresenter) {
        this.dialogPresenter = dialogPresenter;
        setCloseOnEsc(true);
        setCloseOnOutsideClick(true);

        IvomForm ivomForm = new IvomForm(dialogPresenter.getPatients());
        add(ivomForm);
    }

    public void addChangeListener(IvomView ivomView) {
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
