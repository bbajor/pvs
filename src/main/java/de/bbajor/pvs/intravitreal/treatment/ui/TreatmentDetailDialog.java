package de.bbajor.pvs.intravitreal.treatment.ui;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.security.service.UserAccountService;

public class TreatmentDetailDialog extends Dialog {

    public TreatmentDetailDialog(Treatment treatment, TreatmentPlanService treatmentPlanService, 
            UserAccountService userAccountService) {

        setWidth("1000px");
        setHeight("600px");
        setHeaderTitle("Behandlungsdetails für " + treatment.getPatientInfo());
        setCloseOnOutsideClick(false);

        TreatmentDetailLayout layout = new TreatmentDetailLayout(
            treatment, 
            treatment.getApprovalDate() == null, 
            treatmentPlanService,
            userAccountService
        );
        layout.setSizeFull();
        add(layout);
    }
}
