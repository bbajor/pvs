package de.bbajor.pvs.intravitreal.treatment.ui;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;

public class TreatmentDetailDialog extends Dialog {

    public TreatmentDetailDialog(Treatment treatment, TreatmentPlanService treatmentPlanService) {

        setWidth("800px");
        setHeight("1200px");
        setHeaderTitle("Behandlungsdetails für " + treatment.getPatientInfo());

        TreatmentDetailLayout layout = new TreatmentDetailLayout(treatment, treatment.getApprovalDate() == null, treatmentPlanService);
        layout.setSizeFull();
        add(layout);
    }
}
