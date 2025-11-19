package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.patient.model.Patient;

public class WeekListDialog extends Dialog {

    private final TreeGrid<Treatment> grid = new TreeGrid<>();
    private final WeekListConfig config;
    private final ApplicationContext applicationContext;

    public WeekListDialog(WeekListConfig config, ApplicationContext applicationContext) {
        this.config = config;
        this.applicationContext = applicationContext;
        
        setHeight("1200px");
        setWidth("1400px");
        setHeaderTitle("Wochenliste vom " + config.getStartDateOfWeek() + " bis " + config.getEndDateOfWeek());

        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.addHierarchyColumn(treatment -> treatment.getSurgicalCenterString()).setHeader("Einrichtung")
                .setWidth("400px");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        grid.addColumn(treatment -> treatment.getDate().format(formatter)).setHeader("Datum");
        grid.addColumn(treatment -> treatment.getPatientInfo()).setHeader("Patient");
        grid.addColumn(treatment -> treatment.getSideOfEye()).setHeader("Zu behandelndes Auge");
        grid.addColumn(treatment -> {
            if (treatment.getMedicationFavourite() != null && treatment.getMedicationFavourite().getMedication() != null) {
                return treatment.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
            }
            return "-";
        }).setHeader("Medikament")
                .setWidth("200px");
        grid.addColumn(treatment -> treatment.getAdditionalInfo()).setHeader("Zusätzliche Informationen");

        // Gruppiere nach Einrichtung für TreeGrid
        Map<String, List<Treatment>> treatmentsByCenter = config.getTreatmentsOfWeek().stream()
            .collect(Collectors.groupingBy(t -> t.getSurgicalCenterString() != null ? t.getSurgicalCenterString() : "Unbekannt"));
        
        TreeData<Treatment> data = new TreeData<>();
        treatmentsByCenter.forEach((centerName, treatments) -> {
            // Füge Parent-Node hinzu (erste Behandlung als Parent)
            if (!treatments.isEmpty()) {
                Treatment parent = treatments.get(0);
                data.addItem(null, parent);
                // Füge alle anderen Behandlungen als Kinder hinzu
                treatments.stream().skip(1).forEach(t -> data.addItem(parent, t));
            }
        });
        
        grid.setDataProvider(new TreeDataProvider<>(data));
        
        // Klick-Handler für Parent-Nodes (Einrichtungen)
        grid.addItemClickListener(event -> {
            Treatment clicked = event.getItem();
            if (clicked != null && event.getColumn() != null) {
                // Prüfe ob es ein Parent-Node ist (hat Kinder)
                TreeData<Treatment> treeData = ((TreeDataProvider<Treatment>) grid.getDataProvider()).getTreeData();
                List<Treatment> children = treeData.getChildren(clicked);
                
                if (!children.isEmpty() || treeData.getParent(clicked) == null) {
                    // Parent-Node geklickt - zeige alle Behandlungen für diese Einrichtung
                    String centerName = clicked.getSurgicalCenterString();
                    List<Treatment> treatmentsForCenter = config.getTreatmentsOfWeek().stream()
                        .filter(t -> centerName.equals(t.getSurgicalCenterString()))
                        .collect(Collectors.toList());
                    
                    showTreatmentTableDialog(centerName, treatmentsForCenter);
                }
            }
        });
        
        add(grid);

        Button closeButton = new Button("Schließen");
        closeButton.addClickListener(event -> close());
        getFooter().add(closeButton);
    }
    
    private void showTreatmentTableDialog(String centerName, List<Treatment> treatments) {
        Dialog tableDialog = new Dialog();
        tableDialog.setWidth("1200px");
        tableDialog.setHeight("800px");
        tableDialog.setHeaderTitle("Behandlungen für " + centerName);
        
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        
        H3 header = new H3("Behandlungen");
        content.add(header);
        
        Grid<Treatment> treatmentGrid = new Grid<>(Treatment.class, false);
        treatmentGrid.setSizeFull();
        treatmentGrid.setSelectionMode(SelectionMode.NONE);
        
        treatmentGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            return patient != null && patient.getLastName() != null ? patient.getLastName() : "-";
        }).setHeader("Name").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            return patient != null && patient.getFirstName() != null ? patient.getFirstName() : "-";
        }).setHeader("Vorname").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            if (patient != null && patient.getBirth() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                return formatter.format(patient.getBirth());
            }
            return "-";
        }).setHeader("Geburtsdatum").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            if (patient != null && patient.getHealthInsurance() != null && 
                patient.getHealthInsurance().getBillingCarrierName() != null) {
                return patient.getHealthInsurance().getBillingCarrierName();
            }
            return "-";
        }).setHeader("Versicherung").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> t.getSideOfEye() != null ? t.getSideOfEye().toString() : "-")
            .setHeader("Auge").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> {
            if (t.getMedicationFavourite() != null && t.getMedicationFavourite().getMedication() != null) {
                return t.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
            }
            return "-";
        }).setHeader("Medikament").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> t.getApprovalDate() != null ? "Geprüft" : "Nicht geprüft")
            .setHeader("Status").setAutoWidth(true);
        
        treatmentGrid.addColumn(t -> t.getAdditionalInfo() != null && !t.getAdditionalInfo().isBlank() 
            ? t.getAdditionalInfo() : "-")
            .setHeader("Bemerkungen").setAutoWidth(true);
        
        treatmentGrid.setItems(treatments);
        content.add(treatmentGrid);
        content.expand(treatmentGrid);
        
        tableDialog.add(content);
        
        Button closeTableButton = new Button("Schließen");
        closeTableButton.addClickListener(e -> tableDialog.close());
        tableDialog.getFooter().add(closeTableButton);
        
        tableDialog.open();
    }

}
