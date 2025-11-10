package de.bbajor.pvs.settings.ui.tabs;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;

import de.bbajor.pvs.kbv.client.dto.KbvChangeComparison;
import de.bbajor.pvs.kbv.client.dto.KbvCostCarrierDto;
import de.bbajor.pvs.kbv.client.dto.KbvIcdEntryDto;
import de.bbajor.pvs.kbv.client.dto.KbvInsuranceDto;
import de.bbajor.pvs.kbv.service.KbvMasterDataService;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KbvMasterDataTab extends VerticalLayout {

    private final KbvMasterDataService masterDataService;
    private final ComboBox<String> dataTypeComboBox = new ComboBox<>("Datentyp");
    private final ComboBox<String> quarterComboBox = new ComboBox<>("Quartal");
    private final TextField searchField = new TextField("Suche");
    private final Grid<Object> dataGrid = new Grid<>();
    private final Button compareButton = new Button("Vergleichen");
    private final ComboBox<String> fromQuarterComboBox = new ComboBox<>("Von Quartal");
    private final ComboBox<String> toQuarterComboBox = new ComboBox<>("Bis Quartal");

    public KbvMasterDataTab(KbvMasterDataService masterDataService) {
        this.masterDataService = masterDataService;
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        H3 title = new H3("KBV-Stammdaten");
        add(title);

        // Data type selection
        dataTypeComboBox.setItems("ICD-10", "Kostenträger", "Versicherungen");
        dataTypeComboBox.setValue("ICD-10");
        dataTypeComboBox.addValueChangeListener(e -> loadData());

        // Quarter selection
        quarterComboBox.setItems("2024-Q1", "2024-Q2", "2024-Q3", "2024-Q4",
                "2025-Q1", "2025-Q2", "2025-Q3", "2025-Q4");
        quarterComboBox.setValue("2024-Q4");
        quarterComboBox.addValueChangeListener(e -> loadData());

        // Search field
        searchField.setPlaceholder("Code oder Text suchen...");
        searchField.addValueChangeListener(e -> filterData());

        HorizontalLayout filterLayout = new HorizontalLayout(dataTypeComboBox, quarterComboBox, searchField);
        filterLayout.setWidthFull();
        add(filterLayout);

        // Data grid
        setupGrid();
        add(dataGrid);
        dataGrid.setSizeFull();

        // Comparison section
        H3 compareTitle = new H3("Quartals-Vergleich");
        add(compareTitle);

        fromQuarterComboBox.setItems("2024-Q1", "2024-Q2", "2024-Q3", "2024-Q4",
                "2025-Q1", "2025-Q2", "2025-Q3", "2025-Q4");
        toQuarterComboBox.setItems("2024-Q1", "2024-Q2", "2024-Q3", "2024-Q4",
                "2025-Q1", "2025-Q2", "2025-Q3", "2025-Q4");
        toQuarterComboBox.setValue("2024-Q4");

        compareButton.addClickListener(e -> compareQuarters());
        HorizontalLayout compareLayout = new HorizontalLayout(
                fromQuarterComboBox, toQuarterComboBox, compareButton);
        add(compareLayout);

        loadData();
    }

    private void setupGrid() {
        dataGrid.addColumn(item -> {
            if (item instanceof KbvIcdEntryDto entry) {
                return entry.getCode();
            } else if (item instanceof KbvCostCarrierDto carrier) {
                return carrier.getCode();
            } else if (item instanceof KbvInsuranceDto insurance) {
                return insurance.getCode();
            }
            return "";
        }).setHeader("Code").setAutoWidth(true);

        dataGrid.addColumn(item -> {
            if (item instanceof KbvIcdEntryDto entry) {
                return entry.getTextContent();
            } else if (item instanceof KbvCostCarrierDto carrier) {
                return carrier.getName();
            } else if (item instanceof KbvInsuranceDto insurance) {
                return insurance.getName();
            }
            return "";
        }).setHeader("Bezeichnung").setAutoWidth(true);

        dataGrid.addColumn(item -> {
            if (item instanceof KbvIcdEntryDto entry) {
                return entry.getQuarter();
            } else if (item instanceof KbvCostCarrierDto carrier) {
                return carrier.getQuarter();
            } else if (item instanceof KbvInsuranceDto insurance) {
                return insurance.getQuarter();
            }
            return "";
        }).setHeader("Quartal").setAutoWidth(true);
    }

    public void refresh() {
        loadData();
    }

    private void loadData() {
        String quarter = quarterComboBox.getValue();
        String searchTerm = searchField.getValue();
        String dataType = dataTypeComboBox.getValue();

        List<Object> data = switch (dataType) {
            case "ICD-10" -> {
                List<KbvIcdEntryDto> entries = masterDataService.getIcdEntries(quarter, searchTerm);
                yield entries.stream().map(e -> (Object) e).toList();
            }
            case "Kostenträger" -> {
                List<KbvCostCarrierDto> carriers = masterDataService.getCostCarriers(quarter, searchTerm);
                yield carriers.stream().map(c -> (Object) c).toList();
            }
            case "Versicherungen" -> {
                List<KbvInsuranceDto> insurances = masterDataService.getInsurances(quarter, searchTerm);
                yield insurances.stream().map(i -> (Object) i).toList();
            }
            default -> List.of();
        };

        dataGrid.setItems(data);
    }

    private void filterData() {
        loadData();
    }

    private void compareQuarters() {
        String fromQuarter = fromQuarterComboBox.getValue();
        String toQuarter = toQuarterComboBox.getValue();

        if (fromQuarter == null || toQuarter == null) {
            return;
        }

        masterDataService.getChanges(fromQuarter, toQuarter).ifPresent(comparison -> {
            showComparisonDialog(comparison);
        });
    }

    private void showComparisonDialog(KbvChangeComparison comparison) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Quartals-Vergleich: " + comparison.getFromQuarter() + " → " + comparison.getToQuarter());
        dialog.setWidth("80%");
        dialog.setHeight("80%");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        // ICD Changes
        if (comparison.getIcdChanges() != null && !comparison.getIcdChanges().isEmpty()) {
            H3 icdTitle = new H3("ICD-10 Änderungen");
            Grid<KbvChangeComparison.ChangeRecord<KbvIcdEntryDto>> icdGrid = new Grid<>();
            icdGrid.addColumn(record -> {
                if (record.getNewValue() != null) {
                    return record.getNewValue().getCode();
                } else if (record.getOldValue() != null) {
                    return record.getOldValue().getCode();
                }
                return "";
            }).setHeader("Code");
            icdGrid.addColumn(record -> {
                if (record.getNewValue() != null) {
                    return record.getNewValue().getTextContent();
                } else if (record.getOldValue() != null) {
                    return record.getOldValue().getTextContent();
                }
                return "";
            }).setHeader("Text");
            icdGrid.addColumn(KbvChangeComparison.ChangeRecord::getType).setHeader("Typ");
            icdGrid.setItems(comparison.getIcdChanges());
            icdGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
            
            // Color coding
            icdGrid.setClassNameGenerator(record -> {
                return switch (record.getType()) {
                    case "NEW" -> "change-new";
                    case "MODIFIED" -> "change-modified";
                    case "DELETED" -> "change-deleted";
                    default -> "";
                };
            });
            
            content.add(icdTitle, icdGrid);
        }

        // Cost Carrier Changes
        if (comparison.getCostCarrierChanges() != null && !comparison.getCostCarrierChanges().isEmpty()) {
            H3 carrierTitle = new H3("Kostenträger Änderungen");
            Grid<KbvChangeComparison.ChangeRecord<KbvCostCarrierDto>> carrierGrid = new Grid<>();
            carrierGrid.addColumn(record -> {
                if (record.getNewValue() != null) {
                    return record.getNewValue().getCode();
                } else if (record.getOldValue() != null) {
                    return record.getOldValue().getCode();
                }
                return "";
            }).setHeader("Code");
            carrierGrid.addColumn(record -> {
                if (record.getNewValue() != null) {
                    return record.getNewValue().getName();
                } else if (record.getOldValue() != null) {
                    return record.getOldValue().getName();
                }
                return "";
            }).setHeader("Name");
            carrierGrid.addColumn(KbvChangeComparison.ChangeRecord::getType).setHeader("Typ");
            carrierGrid.setItems(comparison.getCostCarrierChanges());
            carrierGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
            
            carrierGrid.setClassNameGenerator(record -> {
                return switch (record.getType()) {
                    case "NEW" -> "change-new";
                    case "MODIFIED" -> "change-modified";
                    case "DELETED" -> "change-deleted";
                    default -> "";
                };
            });
            
            content.add(carrierTitle, carrierGrid);
        }

        // Insurance Changes
        if (comparison.getInsuranceChanges() != null && !comparison.getInsuranceChanges().isEmpty()) {
            H3 insuranceTitle = new H3("Versicherungen Änderungen");
            Grid<KbvChangeComparison.ChangeRecord<KbvInsuranceDto>> insuranceGrid = new Grid<>();
            insuranceGrid.addColumn(record -> {
                if (record.getNewValue() != null) {
                    return record.getNewValue().getCode();
                } else if (record.getOldValue() != null) {
                    return record.getOldValue().getCode();
                }
                return "";
            }).setHeader("Code");
            insuranceGrid.addColumn(record -> {
                if (record.getNewValue() != null) {
                    return record.getNewValue().getName();
                } else if (record.getOldValue() != null) {
                    return record.getOldValue().getName();
                }
                return "";
            }).setHeader("Name");
            insuranceGrid.addColumn(KbvChangeComparison.ChangeRecord::getType).setHeader("Typ");
            insuranceGrid.setItems(comparison.getInsuranceChanges());
            insuranceGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
            
            insuranceGrid.setClassNameGenerator(record -> {
                return switch (record.getType()) {
                    case "NEW" -> "change-new";
                    case "MODIFIED" -> "change-modified";
                    case "DELETED" -> "change-deleted";
                    default -> "";
                };
            });
            
            content.add(insuranceTitle, insuranceGrid);
        }

        Button closeButton = new Button("Schließen", e -> dialog.close());
        dialog.getFooter().add(closeButton);
        dialog.add(content);
        dialog.open();
    }
}
