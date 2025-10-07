package de.bbajor.pvs.intravitreal.treatment.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.intravitreal.treatment.dto.TreatmentDto;

public class DailyTreatmentsDialog extends Dialog {

    private final TreeGrid<TreatmentDto> grid = new TreeGrid<>();

    public DailyTreatmentsDialog(List<TreatmentDto> content) {
        setHeight("1200px");
        setWidth("1400px");
        setHeaderTitle("Wochenliste vom " + LocalDate.now() + " bis " + LocalDate.now().plusDays(7));

        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.NONE);
        grid.addHierarchyColumn(treatment -> treatment.getSurgicalCenterString()).setHeader("Einrichtung")
                .setWidth("400px");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("E dd.MM.yyyy", Locale.GERMAN);
        grid.addColumn(treatment -> treatment.getDate().format(formatter)).setHeader("Datum");
        grid.addColumn(treatment -> treatment.getPatientInfo()).setHeader("Patient");
        grid.addColumn(treatment -> SideOfEye.byDbString(treatment.getSideOfEye())).setHeader("Zu behandelndes Auge");
        grid.addColumn(treatment -> treatment.getMedication().getArzneimittelbezeichnung()).setHeader("Medikament")
                .setWidth("200px");
        grid.addColumn(treatment -> treatment.getAdditionalInfo()).setHeader("Zusätzliche Informationen");

        TreeData<TreatmentDto> data = new TreeData<>();
        data.addItems(null, content);
        grid.setDataProvider(new TreeDataProvider<>(data));
        add(grid);

        Button closeButton = new Button("Schließen");
        closeButton.addClickListener(event -> close());
        getFooter().add(closeButton);
    }

}
