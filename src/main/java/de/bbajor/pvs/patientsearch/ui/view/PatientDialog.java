package de.bbajor.pvs.patientsearch.ui.view;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.data.binder.BinderValidationStatus;

import de.bbajor.pvs.patientsearch.dto.PatientDto;
import de.bbajor.pvs.patientsearch.presenter.PatientDialogPresenter;

public class PatientDialog extends Dialog {

    private List<PatientChangeListener> listeners = new ArrayList<>();

    private final PatientDialogPresenter presenter;
    private final PatientForm form;

    public PatientDialog(PatientDialogPresenter presenter) {
        this.presenter = presenter;

        // Create the components
        form = new PatientForm(presenter.getHealthInsurances());

        var readBtn = new Button("Patientendaten einlesen", event -> {
            presenter.readDataFromEgk();
            form.writeValuesToPatient(presenter.getWorkingCopy());
        });
        readBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        var saveBtn = new Button("Patienten anlegen", event -> save());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        var cancelBtn = new Button("Abbrechen", event -> close());

        // Configure the dialog
        setHeaderTitle("Neuer Patient");
        add(form);
        getFooter().add(cancelBtn, readBtn, saveBtn);
    }

    public void addChangeListener(PatientChangeListener listener) {
        listeners.add(listener);
    }

    protected void notifyListeners() {
        listeners.forEach(PatientChangeListener::onPatientChanged);
    }

    private void save() {
        BinderValidationStatus<PatientDto> validationStatus = presenter.saveChanges(form);
        if (validationStatus.isOk()) {
            notifyListeners();
            close();
        }
    }

    public void loadPatientById(Integer id) {
        presenter.loadPatientById(id);
        form.writeValuesToPatient(presenter.getWorkingCopy());
    }
}