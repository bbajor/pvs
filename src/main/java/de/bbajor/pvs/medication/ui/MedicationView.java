package de.bbajor.pvs.medication.ui;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.bbajor.pvs.medication.controller.MedicationViewPresenter;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.security.AppRoles;
import jakarta.annotation.security.RolesAllowed;

@Route("ivom-drugs")
@PageTitle("Medikamente")
@RolesAllowed({ AppRoles.SUPER_ADMIN, AppRoles.ADMIN, AppRoles.TECH_USER, AppRoles.OWNER })
public class MedicationView extends Main {

    private static final Logger LOGGER = LogManager.getLogger(MedicationView.class);

    private final TreeGrid<MedicationNode> grid = new TreeGrid<>();
    private final Grid<MedicationFavourite> favouritesGrid = new Grid<>(MedicationFavourite.class, false);
    // Member-Variable für den TreeDataProvider
    private TreeDataProvider<MedicationNode> dataProvider = new TreeDataProvider<>(new TreeData<>());
    private List<MedicationFavourite> currentFavourites = new ArrayList<>();
    private Set<Long> favouriteMedicationIds = new HashSet<>();
    private final Button addFavouriteButton = new Button("Als Favorit hinzufügen", VaadinIcon.STAR.create());
    private final Button removeFavouriteButton = new Button("Favorit entfernen", VaadinIcon.TRASH.create());
    private boolean institutionContextPresent;

    private UI myUi; // wird in onAttach gesetzt
    private MedicationViewPresenter medicationPresenter;
    private final AuthenticationContext authenticationContext;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.myUi = attachEvent.getUI();
    }

    public MedicationView(MedicationViewPresenter medicationPresenter, AuthenticationContext authenticationContext) {
        this.medicationPresenter = medicationPresenter;
        this.authenticationContext = authenticationContext;
        this.institutionContextPresent = InstitutionContext.hasInstitution();

        addFavouriteButton.setEnabled(false);
        removeFavouriteButton.setEnabled(false);
        addFavouriteButton.addClickListener(event -> handleAddFavourite());
        removeFavouriteButton.addClickListener(event -> handleRemoveFavourite());

        // Info für den Anwender
        Paragraph info = new Paragraph("Bitte laden Sie die Arzneimitteldaten als CSV von folgender Seite herunter:");
        Anchor link = new Anchor("https://portal.dimdi.de/amguifree/am/search.xhtml",
                "DIMDI Arzneimittel-Informationssystem");
        link.setTarget("_blank");
        Div infoBox = new Div(info, link);
        infoBox.getStyle().set("margin-bottom", "20px");

        // Spalten mit ComponentRenderer für besseres Styling
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        
        // Hierarchie-Spalte → zeigt Label (addHierarchyColumn akzeptiert ValueProvider, nicht ComponentRenderer)
        grid.addHierarchyColumn(MedicationNode::getLabel)
                .setHeader("Bezeichnung")
                .setResizable(true)
                .setWidth("250px");
        
        grid.addColumn(new ComponentRenderer<>(node -> {
            String wirkstoffe = node.getWirkstoffe() != null ? node.getWirkstoffe() : "-";
            Span span = new Span(wirkstoffe);
            return span;
        })).setHeader("Wirkstoffe").setResizable(true).setWidth("250px");
        
        grid.addColumn(new ComponentRenderer<>(node -> {
            String eingangsnummer = node.getEingangsnummer() != null ? node.getEingangsnummer() : "-";
            Span span = new Span(eingangsnummer);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Eingangsnummer").setResizable(true);
        
        grid.addColumn(new ComponentRenderer<>(node -> {
            String zulassungsinhaber = node.getZulassungsinhaber() != null ? node.getZulassungsinhaber() : "-";
            Span span = new Span(zulassungsinhaber);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Zulassungsinhaber").setResizable(true);
        
        grid.addColumn(new ComponentRenderer<>(node -> {
            String anwendungsgebiete = node.getAnwendungsgebiete() != null ? node.getAnwendungsgebiete() : "-";
            Span span = new Span(anwendungsgebiete);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Anwendungsgebiete").setResizable(true);
        grid.addColumn(new ComponentRenderer<>(dto -> {
            if (dto.getMedication() == null) {
                return new Span("");
            }
            if (Boolean.TRUE.equals(dto.isFavourite())) {
                Icon check = VaadinIcon.CHECK.create();
                check.setColor("green");
                return check;
            } else {
                Icon cross = VaadinIcon.CLOSE.create();
                cross.setColor("red");
                return cross;
            }
        })).setHeader("Favorit");
        grid.setSizeFull();
        grid.setDetailsVisibleOnClick(true);
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.addSelectionListener(event -> updateActionButtonState());
        grid.addItemDoubleClickListener(event -> {
            MedicationNode medicationNode = event.getItem();
            if (medicationNode.getMedication() != null) {
                MedicationDetailDialog detailDialog = new MedicationDetailDialog(medicationPresenter,
                        medicationNode.getMedication(), updatedDto -> {
                            MedicationNode nodeToRefresh = findById(grid.getTreeData(), null,
                                    medicationNode.getMedication().getId());
                            if (nodeToRefresh != null) {
                                TreeData<MedicationNode> treeData = grid.getTreeData();
                                MedicationNode parent = treeData.getParent(medicationNode);
                                treeData.removeItem(medicationNode);
                                MedicationNode newNode = new MedicationNode(medicationNode.getLabel(), updatedDto,
                                        favouriteMedicationIds.contains(updatedDto.getId()));
                                treeData.addItem(parent, newNode);
                                grid.getDataProvider().refreshAll();
                                updateActionButtonState();
                            }

                        });
                detailDialog.open();
            }
        });
        grid.setDataProvider(dataProvider);

        configureFavouritesGrid();

        // Filter oben
        TextField filterField = new TextField();
        filterField.setPlaceholder("Filter...");
        filterField.setWidthFull();
        filterField.setClearButtonVisible(true);
        filterField.addKeyUpListener(e -> dataProvider.setFilter(drug -> {
            return filterField.getValue() != null ? drug.isContainsSearchTerm(filterField.getValue().toLowerCase())
                    : true;
        }));
        
        // Überschrift
        H1 title = new H1("Medikamentenkatalog");
        title.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.FontWeight.SEMIBOLD, 
                LumoUtility.Margin.Bottom.LARGE);
        add(title);
        
        infoBox.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);
        add(infoBox);
        if (isTechUser() || isSuperAdmin()) {
            add(initUpload());
        }
        add(filterField);
        add(grid);
        if (institutionContextPresent) {
            Span favouritesHeader = new Span("Favoriten der Institution");
            favouritesHeader.getStyle().set("font-weight", "600");
            HorizontalLayout favouriteActions = new HorizontalLayout(addFavouriteButton, removeFavouriteButton);
            favouriteActions.setSpacing(true);
            favouriteActions.setPadding(false);
            favouriteActions.setWidthFull();
            favouriteActions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.START);

            Div favouritesSection = new Div();
            favouritesSection.addClassName("favourites-section");
            favouritesSection.getStyle().set("display", "flex");
            favouritesSection.getStyle().set("flex-direction", "column");
            favouritesSection.getStyle().set("gap", "var(--lumo-space-s)");
            favouritesSection.add(favouritesHeader, favouriteActions, favouritesGrid);
            add(favouritesSection);
        } else {
            Paragraph hint = new Paragraph("Hinweis: Favoriten stehen nur innerhalb einer Institution zur Verfügung.");
            hint.getStyle().set("color", "var(--lumo-secondary-text-color)");
            add(hint);
        }

        refreshData();

        // Container als Flexbox konfigurieren
        setSizeFull();
        getStyle().set("display", "flex");
        getStyle().set("flex-direction", "column");
        getStyle().set("min-height", "0");
        addClassNames("view-content");
        
        // Grid-Styling wie im IVOM-Planer
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_WRAP_CELL_CONTENT);
    }

    private Upload initUpload() {
        // InMemory handler — liefert byte[] mit den Daten
        UploadHandler inMemory = UploadHandler.inMemory((metadata, bytes) -> {
            // Läuft im Request/Handler-Thread — UI.getCurrent() kann hier null sein.
            try {
                int importedCount;
                try (InputStream in = new ByteArrayInputStream(bytes)) {
                    importedCount = medicationPresenter.importCsv(new InputStreamReader(in));
                }

                // UI-Update: sichere Ausführung im UI-Thread
                if (myUi != null) {
                    myUi.access(() -> {
                        refreshData();
                        Notification.show(importedCount + " Medikamente importiert",
                                3000, Notification.Position.MIDDLE);
                    });
                } else if (VaadinSession.getCurrent() != null) {
                    // Fallback: alle UIs in der Session updaten
                    VaadinSession.getCurrent().getUIs().forEach(ui -> ui.access(() -> {
                        refreshData();
                        Notification.show(importedCount + " Medikamente importiert",
                                3000, Notification.Position.MIDDLE);
                    }));
                } else {
                    // letzter Rückfall: nur loggen (damit du siehst, dass Handler lief)
                    System.out.println("Upload finished, but no UI available. imported=" + importedCount);
                }
            } catch (Exception ex) {
                // unbedingt loggen, sonst merkst du nichts
                ex.printStackTrace();
            }
        });

        Upload upload = new Upload(inMemory);
        upload.setMaxFiles(1);
        // optional: Benutzer-Friendly-Listener für Rejected/Failed
        upload.addFileRejectedListener(
                ev -> Notification.show(ev.getErrorMessage(), 4000, Notification.Position.MIDDLE));
        // add upload to UI
        return upload;
    }

    private boolean isTechUser() {
        return authenticationContext.getGrantedRoles().stream()
                .anyMatch(role -> AppRoles.TECH_USER.equals(role) || AppRoles.ADMIN.equals(role));
    }

    private boolean isSuperAdmin() {
        return authenticationContext.getGrantedRoles().stream()
                .anyMatch(role -> AppRoles.SUPER_ADMIN.equals(role));
    }

    private void configureFavouritesGrid() {
        favouritesGrid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
        
        favouritesGrid.addColumn(new ComponentRenderer<>(f -> {
            String name = f.getEffectiveDisplayName() != null ? f.getEffectiveDisplayName() : "-";
            Span span = new Span(name);
            span.addClassNames(LumoUtility.FontWeight.SEMIBOLD);
            return span;
        })).setHeader("Bezeichnung").setAutoWidth(true).setFlexGrow(2);
        
        favouritesGrid.addColumn(new ComponentRenderer<>(f -> {
            String wirkstoffe = f.getMedication() != null && f.getMedication().getWirkstoffe() != null 
                    ? f.getMedication().getWirkstoffe() : "-";
            Span span = new Span(wirkstoffe);
            return span;
        })).setHeader("Wirkstoffe").setAutoWidth(true).setFlexGrow(2);
        
        favouritesGrid.addColumn(new ComponentRenderer<>(f -> {
            String zulassungsnr = f.getMedication() != null && f.getMedication().getZulassungsNr() != null 
                    ? f.getMedication().getZulassungsNr() : "-";
            Span span = new Span(zulassungsnr);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Zulassungsnr.").setAutoWidth(true).setFlexGrow(1);
        
        favouritesGrid.addColumn(new ComponentRenderer<>(f -> {
            String validFrom = f.getValidFrom() != null ? f.getValidFrom().toString() : "-";
            Span span = new Span(validFrom);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Gültig ab").setWidth("120px").setFlexGrow(0);
        
        favouritesGrid.addColumn(new ComponentRenderer<>(f -> {
            String validUntil = f.getValidUntil() != null ? f.getValidUntil().toString() : "-";
            Span span = new Span(validUntil);
            span.addClassNames(LumoUtility.TextColor.SECONDARY);
            return span;
        })).setHeader("Gültig bis").setWidth("120px").setFlexGrow(0);
        
        favouritesGrid.setSelectionMode(SelectionMode.SINGLE);
        favouritesGrid.setWidthFull();
        favouritesGrid.setHeight("240px");
        favouritesGrid.addSelectionListener(event -> updateActionButtonState());
    }

    private void refreshData() {
        List<Medication> medications = medicationPresenter.getAll();
        List<MedicationFavourite> favourites = institutionContextPresent
                ? medicationPresenter.getActiveFavouritesForCurrentInstitution()
                : List.of();
        favouriteMedicationIds = favourites.stream()
                .filter(f -> f.getMedication() != null && f.getMedication().getId() != null)
                .map(f -> f.getMedication().getId())
                .collect(Collectors.toCollection(HashSet::new));
        currentFavourites = favourites;
        reloadTree(medications, favouriteMedicationIds);
        if (institutionContextPresent) {
            favouritesGrid.setItems(currentFavourites);
            favouritesGrid.deselectAll();
        }
        grid.deselectAll();
        updateActionButtonState();
    }

    private void handleAddFavourite() {
        if (!institutionContextPresent) {
            return;
        }
        MedicationNode selectedNode = grid.asSingleSelect().getValue();
        if (selectedNode == null || selectedNode.getMedication() == null
                || selectedNode.getMedication().getId() == null) {
            return;
        }
        try {
            medicationPresenter.addFavourite(selectedNode.getMedication().getId());
            Notification.show("Favorit hinzugefügt", 2500, Notification.Position.MIDDLE);
            refreshData();
        } catch (IllegalStateException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        } catch (Exception ex) {
            LOGGER.error("Fehler beim Hinzufügen eines Medikamentenfavoriten", ex);
            Notification.show("Favorit konnte nicht gespeichert werden.", 4000, Notification.Position.MIDDLE);
        }
    }

    private void handleRemoveFavourite() {
        if (!institutionContextPresent) {
            return;
        }
        MedicationFavourite selectedFavourite = favouritesGrid.asSingleSelect().getValue();
        if (selectedFavourite == null) {
            return;
        }
        try {
            medicationPresenter.removeFavourite(selectedFavourite.getId());
            Notification.show("Favorit entfernt", 2500, Notification.Position.MIDDLE);
            refreshData();
        } catch (Exception ex) {
            LOGGER.error("Fehler beim Entfernen eines Medikamentenfavoriten", ex);
            Notification.show("Favorit konnte nicht entfernt werden.", 4000, Notification.Position.MIDDLE);
        }
    }

    private void updateActionButtonState() {
        if (!institutionContextPresent) {
            addFavouriteButton.setEnabled(false);
            removeFavouriteButton.setEnabled(false);
            return;
        }
        MedicationNode selectedNode = grid.asSingleSelect().getValue();
        boolean canAdd = selectedNode != null
                && selectedNode.getMedication() != null
                && selectedNode.getMedication().getId() != null
                && !favouriteMedicationIds.contains(selectedNode.getMedication().getId());
        addFavouriteButton.setEnabled(canAdd);

        MedicationFavourite selectedFavourite = favouritesGrid.asSingleSelect().getValue();
        removeFavouriteButton.setEnabled(selectedFavourite != null);
    }

    private TreeData<MedicationNode> buildTree(List<Medication> medications, Set<Long> favouriteIds) {
        TreeData<MedicationNode> newTreeData = new TreeData<>();

        Map<String, MedicationNode> bezeichnungNodes = new HashMap<>();

        for (Medication medication : medications) {

            // nur einmal anlegen
            MedicationNode parent = bezeichnungNodes.computeIfAbsent(medication.getArzneimittelbezeichnung(), key -> {
                MedicationNode node = new MedicationNode(key); // Zwischenknoten ohne DTO
                newTreeData.addItem(null, node); // als Root-Item
                return node;
            });

            // Ebene 2: Leaf
            boolean isFavourite = medication.getId() != null && favouriteIds.contains(medication.getId());
            MedicationNode leaf = new MedicationNode(medication.getWirkstoffe(), medication, isFavourite);
            if (!newTreeData.contains(leaf)) {
                newTreeData.addItem(parent, leaf);
            }
        }
        return newTreeData;

    }

    private void reloadTree(List<Medication> medication, Set<Long> favouriteIds) {
        TreeData<MedicationNode> newTreeData = buildTree(new ArrayList<>(medication), favouriteIds);
        TreeDataProvider<MedicationNode> newProvider = new TreeDataProvider<>(newTreeData);
        grid.setDataProvider(newProvider);
        dataProvider = newProvider; // Referenz aktualisieren
    }

    private MedicationNode findById(TreeData<MedicationNode> treeData, MedicationNode parent, Long id) {
        for (MedicationNode child : treeData.getChildren(parent)) {
            if (child.getMedication() != null && id.equals(child.getMedication().getId())) {
                return child;
            }
            MedicationNode found = findById(treeData, child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
