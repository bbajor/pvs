package de.bbajor.pvs.taskmanagement.ui.view;

import java.util.List;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.spring.security.AuthenticationContext;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.taskmanagement.domain.Task;
import de.bbajor.pvs.taskmanagement.service.TaskService;

public class TaskReviewDialog extends Dialog {

    public TaskReviewDialog(Task task, TreatmentRepository treatmentRepository, TaskService taskService,
            AuthenticationContext authenticationContext) {
        setHeaderTitle("Behandlungen überprüfen");
        setWidth("900px");
        setHeight("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();

        Grid<Treatment> grid = new Grid<>(Treatment.class, false);
        grid.addColumn(t -> t.getSurgicalCenterTimeSlot().getDate()).setHeader("Datum");
        grid.addColumn(t -> t.getSurgicalCenterString()).setHeader("Ort");
        grid.addColumn(t -> t.getSideOfEye()).setHeader("Auge");
        grid.addColumn(t -> t.getMedication() != null ? t.getMedication().getArzneimittelbezeichnung() : "-")
                .setHeader("Medikament");
        grid.addColumn(t -> t.getApprovalDate() != null ? t.getApprovalDate().toString() : "Offen")
                .setHeader("Status");
        grid.setSizeFull();

        List<Treatment> treatments = treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId());
        grid.setItems(treatments);

        Button approveSelected = new Button("Ausgewählte approbieren", e -> {
            var selected = grid.getSelectedItems();
            String user = authenticationContext.getPrincipalName().orElse("unknown");
            String userId = user;
            selected.forEach(t -> taskService.approveTreatment(t.getId(), userId, user, false));
            grid.setItems(treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId()));
        });
        approveSelected.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button approveSecond = new Button("Als Zweitprüfer bestätigen", e -> {
            var selected = grid.getSelectedItems();
            String user = authenticationContext.getPrincipalName().orElse("unknown");
            String userId = user;
            selected.forEach(t -> taskService.approveTreatment(t.getId(), userId, user, true));
            grid.setItems(treatmentRepository.findByTimeSlotId(task.getTimeSlot().getId()));
        });
        approveSecond.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        layout.add(new H3(task.getDescription() == null ? "Behandlungen im Task" : task.getDescription()), grid,
                approveSelected, approveSecond);
        add(layout);
    }
}
