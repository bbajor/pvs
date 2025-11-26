package de.bbajor.pvs.intravitreal.treatment.ui;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.dialog.Dialog;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.service.TreatmentPlanService;
import de.bbajor.pvs.security.service.UserAccountService;

public class TreatmentDetailDialog extends Dialog {

    public TreatmentDetailDialog(Treatment treatment, TreatmentPlanService treatmentPlanService, 
            UserAccountService userAccountService) {
        this(treatment, treatmentPlanService, userAccountService, null);
    }

    public TreatmentDetailDialog(Treatment treatment, TreatmentPlanService treatmentPlanService, 
            UserAccountService userAccountService, ApplicationContext applicationContext) {

        setWidth("1000px");
        setHeight("600px");
        setHeaderTitle("Behandlungsdetails für " + treatment.getPatientInfo());

        TreatmentDetailLayout layout = new TreatmentDetailLayout(
            treatment, 
            treatment.getApprovalDate() == null, 
            treatmentPlanService,
            userAccountService
        );
        
        // Setze ApplicationContext für Kostenübersicht
        if (applicationContext != null) {
            layout.setApplicationContext(applicationContext);
        }
        
        layout.setSizeFull();
        add(layout);
    }
}
