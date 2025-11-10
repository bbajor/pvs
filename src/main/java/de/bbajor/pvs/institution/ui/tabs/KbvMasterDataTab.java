package de.bbajor.pvs.institution.ui.tabs;

import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import de.bbajor.pvs.kbv.client.dto.KbvImportHistoryDto;
import de.bbajor.pvs.kbv.service.KbvImportService;
import de.bbajor.pvs.kbv.service.KbvMasterDataOrchestrator;

/**
 * Super-Admin-Tab für das Anstoßen und Überwachen des zentralen KBV-Stammdatenimports.
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KbvMasterDataTab extends VerticalLayout {

    private final KbvImportService importService;
    private final KbvMasterDataOrchestrator orchestrator;

    private final Button triggerImportButton = new Button("Import & Rollout starten");
    private final Grid<KbvImportHistoryDto> historyGrid = new Grid<>();
    private final ComboBox<String> quarterFilter = new ComboBox<>("Quartal filtern");
    private final ComboBox<String> statusFilter = new ComboBox<>("Status filtern");

    public KbvMasterDataTab(KbvImportService importService, KbvMasterDataOrchestrator orchestrator) {
        this.importService = importService;
        this.orchestrator = orchestrator;

        setSizeFull();
        setSpacing(true);
        setPadding(true);

        add(new H3("KBV-Stammdaten Import"));

        VerticalLayout importSection = new VerticalLayout();
        importSection.setSpacing(true);
        importSection.add(new H3("Manueller Import"));

        TextField filePathField = new TextField("Dateipfad (zentral)");
        filePathField.setWidthFull();

        ComboBox<String> quarterComboBox = new ComboBox<>("Quartal");
        quarterComboBox.setItems("2024-Q1", "2024-Q2", "2024-Q3", "2024-Q4",
                "2025-Q1", "2025-Q2", "2025-Q3", "2025-Q4");

        TextField versionField = new TextField("Version");
        versionField.setWidthFull();

        triggerImportButton.addClickListener(e -> {
            String filePath = filePathField.getValue();
            String quarter = quarterComboBox.getValue();
            String version = versionField.getValue();

            if (filePath == null || filePath.isBlank()) {
                Notification.show("Bitte Dateipfad angeben", 3500, Notification.Position.MIDDLE);
                return;
            }
            if (quarter == null || quarter.isBlank()) {
                Notification.show("Bitte Quartal auswählen", 3500, Notification.Position.MIDDLE);
                return;
            }

            boolean success = orchestrator.triggerImportAndDistribute(filePath, quarter, version);
            if (success) {
                Notification.show("Import ausgelöst – Rollout auf Mandanten gestartet", 5000,
                        Notification.Position.MIDDLE);
                loadHistory();
            } else {
                Notification.show("Import konnte nicht gestartet werden", 5000, Notification.Position.MIDDLE);
            }
        });

        importSection.add(filePathField, quarterComboBox, versionField, triggerImportButton);
        add(importSection);

        add(new H3("Import-Historie"));

        quarterFilter.setItems("2024-Q1", "2024-Q2", "2024-Q3", "2024-Q4",
                "2025-Q1", "2025-Q2", "2025-Q3", "2025-Q4");
        quarterFilter.addValueChangeListener(e -> loadHistory());

        statusFilter.setItems("RUNNING", "SUCCESS", "FAILED");
        statusFilter.addValueChangeListener(e -> loadHistory());

        HorizontalLayout filterLayout = new HorizontalLayout(quarterFilter, statusFilter);
        filterLayout.setWidthFull();
        add(filterLayout);

        setupHistoryGrid();
        add(historyGrid);
        historyGrid.setSizeFull();

        loadHistory();
    }

    private void setupHistoryGrid() {
        historyGrid.addColumn(KbvImportHistoryDto::getQuarter).setHeader("Quartal").setAutoWidth(true);
        historyGrid.addColumn(KbvImportHistoryDto::getVersion).setHeader("Version").setAutoWidth(true);
        historyGrid.addColumn(KbvImportHistoryDto::getImportType).setHeader("Typ").setAutoWidth(true);
        historyGrid.addColumn(KbvImportHistoryDto::getStatus).setHeader("Status").setAutoWidth(true);
        historyGrid.addColumn(KbvImportHistoryDto::getRecordsImported).setHeader("Datensätze").setAutoWidth(true);
        historyGrid.addColumn(history -> history.getStartedAt() != null ? history.getStartedAt().toString() : "")
                .setHeader("Gestartet").setAutoWidth(true);
        historyGrid.addColumn(history -> history.getCompletedAt() != null ? history.getCompletedAt().toString() : "")
                .setHeader("Abgeschlossen").setAutoWidth(true);
    }

    public void refresh() {
        loadHistory();
    }

    private void loadHistory() {
        String quarter = quarterFilter.getValue();
        String status = statusFilter.getValue();

        List<KbvImportHistoryDto> history;
        if (quarter != null && !quarter.isBlank()) {
            history = importService.getImportHistory(quarter);
        } else if (status != null && !status.isBlank()) {
            history = importService.getImportHistoryByStatus(status);
        } else {
            history = importService.getImportHistory(null);
        }

        historyGrid.setItems(history);
    }
}
