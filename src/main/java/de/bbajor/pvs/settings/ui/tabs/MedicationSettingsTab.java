package de.bbajor.pvs.settings.ui.tabs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.medication.controller.MedicationViewPresenter;
import de.bbajor.pvs.medication.model.Medication;
import de.bbajor.pvs.medication.model.MedicationFavourite;
import de.bbajor.pvs.medication.ui.MedicationNode;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class MedicationSettingsTab extends VerticalLayout {

    private static final Logger log = LogManager.getLogger(MedicationSettingsTab.class);

    private final MedicationViewPresenter medicationPresenter;
    private final UserAccountRepository userAccountRepository;

    private final TreeGrid<MedicationNode> medicationGrid = new TreeGrid<>();
    private final Grid<MedicationFavourite> favouritesGrid = new Grid<>(MedicationFavourite.class, false);
    private TreeDataProvider<MedicationNode> dataProvider = new TreeDataProvider<>(new TreeData<>());
    private List<MedicationFavourite> currentFavourites = new ArrayList<>();
    private Set<Long> favouriteMedicationIds = new HashSet<>();
    private final Button addFavouriteButton = new Button("Zu Favoriten hinzufügen", VaadinIcon.STAR.create());
    private final Button removeFavouriteButton = new Button("Favorit entfernen", VaadinIcon.TRASH.create());
    private boolean institutionContextPresent;

    @PostConstruct
    public void init() {
        setSizeFull();
        setSpacing(true);
        setPadding(true);

        ensureInstitutionContext();
        institutionContextPresent = InstitutionContext.hasInstitution();

        addFavouriteButton.setEnabled(false);
        removeFavouriteButton.setEnabled(false);
        addFavouriteButton.addClickListener(event -> handleAddFavourite());
        removeFavouriteButton.addClickListener(event -> handleRemoveFavourite());

        // Info-Link zu DIMDI
        Span infoText = new Span("Aktuell gültige Medikamente aus der Datenbank. " +
                "Weitere Informationen: ");
        Anchor dimdiLink = new Anchor("https://portal.dimdi.de/amguifree/am/search.xhtml",
                "DIMDI Arzneimittel-Informationssystem");
        dimdiLink.setTarget("_blank");
        Span infoSpan = new Span();
        infoSpan.add(infoText, dimdiLink);
        infoSpan.getStyle().set("margin-bottom", "var(--lumo-space-m)");
        add(infoSpan);

        // Filter
        TextField filterField = new TextField();
        filterField.setPlaceholder("Medikament suchen...");
        filterField.setWidthFull();
        filterField.setClearButtonVisible(true);
        filterField.addValueChangeListener(e -> {
            if (dataProvider != null) {
                dataProvider.setFilter(drug -> {
                    String filterValue = filterField.getValue();
                    return filterValue == null || filterValue.isEmpty() 
                            || drug.isContainsSearchTerm(filterValue.toLowerCase());
                });
            }
        });
        add(filterField);

        // Configure medication grid
        configureMedicationGrid();
        add(medicationGrid);

        // Favourites section (only if institution context present)
        if (institutionContextPresent) {
            H3 favouritesHeader = new H3("Favoriten der Institution");
            favouritesHeader.getStyle().set("margin-top", "var(--lumo-space-l)");
            favouritesHeader.getStyle().set("margin-bottom", "var(--lumo-space-s)");

            HorizontalLayout favouriteActions = new HorizontalLayout(addFavouriteButton, removeFavouriteButton);
            favouriteActions.setSpacing(true);
            favouriteActions.setPadding(false);

            configureFavouritesGrid();

            VerticalLayout favouritesSection = new VerticalLayout();
            favouritesSection.setSpacing(true);
            favouritesSection.setPadding(false);
            favouritesSection.add(favouritesHeader, favouriteActions, favouritesGrid);
            add(favouritesSection);
        } else {
            Span hint = new Span("Hinweis: Favoriten stehen nur innerhalb einer Institution zur Verfügung.");
            hint.getStyle().set("color", "var(--lumo-secondary-text-color)");
            hint.getStyle().set("margin-top", "var(--lumo-space-l)");
            add(hint);
        }

        refreshData();
    }

    private void configureMedicationGrid() {
        medicationGrid.addHierarchyColumn(MedicationNode::getLabel)
                .setHeader("Bezeichnung")
                .setResizable(true)
                .setWidth("250px");
        medicationGrid.addColumn(MedicationNode::getWirkstoffe)
                .setHeader("Wirkstoffe")
                .setResizable(true)
                .setWidth("250px");
        medicationGrid.addColumn(MedicationNode::getEingangsnummer)
                .setHeader("Eingangsnummer")
                .setResizable(true);
        medicationGrid.addColumn(MedicationNode::getZulassungsinhaber)
                .setHeader("Zulassungsinhaber")
                .setResizable(true);
        medicationGrid.addColumn(new ComponentRenderer<>(dto -> {
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
        })).setHeader("Ist Favorit");
        medicationGrid.setSizeFull();
        medicationGrid.setHeight("400px");
        medicationGrid.setSelectionMode(SelectionMode.SINGLE);
        medicationGrid.addSelectionListener(event -> updateActionButtonState());
        medicationGrid.setDataProvider(dataProvider);
    }

    private void configureFavouritesGrid() {
        favouritesGrid.addColumn(MedicationFavourite::getEffectiveDisplayName)
                .setHeader("Bezeichnung")
                .setAutoWidth(true)
                .setFlexGrow(2);
        favouritesGrid.addColumn(f -> f.getMedication() != null ? f.getMedication().getWirkstoffe() : "-")
                .setHeader("Wirkstoffe")
                .setAutoWidth(true)
                .setFlexGrow(2);
        favouritesGrid.addColumn(f -> f.getMedication() != null ? f.getMedication().getZulassungsNr() : "-")
                .setHeader("Zulassungsnr.")
                .setAutoWidth(true)
                .setFlexGrow(1);
        favouritesGrid.addColumn(f -> f.getValidFrom() != null ? f.getValidFrom().toString() : "-")
                .setHeader("Gültig ab")
                .setWidth("120px")
                .setFlexGrow(0);
        favouritesGrid.addColumn(f -> f.getValidUntil() != null ? f.getValidUntil().toString() : "-")
                .setHeader("Gültig bis")
                .setWidth("120px")
                .setFlexGrow(0);
        favouritesGrid.setSelectionMode(SelectionMode.SINGLE);
        favouritesGrid.setWidthFull();
        favouritesGrid.setHeight("240px");
        favouritesGrid.addSelectionListener(event -> updateActionButtonState());
    }

    private void refreshData() {
        ensureInstitutionContext();
        
        // Get all medications and filter for currently valid ones
        List<Medication> allMedications = medicationPresenter.getAll();
        LocalDate today = LocalDate.now();
        List<Medication> validMedications = allMedications.stream()
                .filter(med -> med.getValidUntil() == null || med.getValidUntil().isAfter(today) || med.getValidUntil().isEqual(today))
                .filter(med -> med.getValidFrom() == null || med.getValidFrom().isBefore(today) || med.getValidFrom().isEqual(today))
                .collect(Collectors.toList());

        List<MedicationFavourite> favourites = institutionContextPresent
                ? medicationPresenter.getActiveFavouritesForCurrentInstitution()
                : List.of();
        favouriteMedicationIds = favourites.stream()
                .filter(f -> f.getMedication() != null && f.getMedication().getId() != null)
                .map(f -> f.getMedication().getId())
                .collect(Collectors.toCollection(HashSet::new));
        currentFavourites = favourites;
        
        // Filter out medications that are already favourites
        List<Medication> medicationsToShow = validMedications.stream()
                .filter(med -> !favouriteMedicationIds.contains(med.getId()))
                .collect(Collectors.toList());
        
        reloadTree(medicationsToShow, favouriteMedicationIds);
        
        if (institutionContextPresent) {
            favouritesGrid.setItems(currentFavourites);
            favouritesGrid.deselectAll();
        }
        medicationGrid.deselectAll();
        updateActionButtonState();
    }

    private void handleAddFavourite() {
        ensureInstitutionContext();
        
        if (!institutionContextPresent) {
            Notification.show("Kein Institution-Kontext gefunden. Bitte melden Sie sich erneut an oder kontaktieren Sie den Administrator.",
                    5000, Notification.Position.MIDDLE);
            return;
        }
        
        MedicationNode selectedNode = medicationGrid.asSingleSelect().getValue();
        if (selectedNode == null || selectedNode.getMedication() == null
                || selectedNode.getMedication().getId() == null) {
            Notification.show("Bitte wählen Sie ein Medikament aus der Liste aus.", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        Long medicationId = selectedNode.getMedication().getId();
        
        // Prüfen ob bereits vorhanden
        if (favouriteMedicationIds.contains(medicationId)) {
            Notification.show("Dieses Medikament ist bereits als Favorit vorhanden.", 3000, Notification.Position.MIDDLE);
            return;
        }
        
        try {
            medicationPresenter.addFavourite(medicationId);
            Notification.show("Favorit hinzugefügt", 2500, Notification.Position.MIDDLE);
            refreshData();
        } catch (IllegalStateException ex) {
            Notification.show(ex.getMessage(), 4000, Notification.Position.MIDDLE);
        } catch (Exception ex) {
            log.error("Fehler beim Hinzufügen eines Medikamentenfavoriten", ex);
            Notification.show("Favorit konnte nicht gespeichert werden.", 4000, Notification.Position.MIDDLE);
        }
    }

    private void handleRemoveFavourite() {
        ensureInstitutionContext();
        
        if (!institutionContextPresent) {
            Notification.show("Kein Institution-Kontext gefunden. Bitte melden Sie sich erneut an oder kontaktieren Sie den Administrator.",
                    5000, Notification.Position.MIDDLE);
            return;
        }
        
        MedicationFavourite selectedFavourite = favouritesGrid.asSingleSelect().getValue();
        if (selectedFavourite == null) {
            Notification.show("Bitte wählen Sie einen Favoriten aus der Liste aus.", 3000, Notification.Position.MIDDLE);
            return;
        }
        try {
            medicationPresenter.removeFavourite(selectedFavourite.getId());
            Notification.show("Favorit entfernt", 2500, Notification.Position.MIDDLE);
            refreshData();
        } catch (Exception ex) {
            log.error("Fehler beim Entfernen eines Medikamentenfavoriten", ex);
            Notification.show("Favorit konnte nicht entfernt werden.", 4000, Notification.Position.MIDDLE);
        }
    }

    private void updateActionButtonState() {
        if (!institutionContextPresent) {
            addFavouriteButton.setEnabled(false);
            removeFavouriteButton.setEnabled(false);
            return;
        }
        
        // Add button is enabled if a medication is selected and it's not already a favourite
        MedicationNode selectedNode = medicationGrid.asSingleSelect().getValue();
        boolean canAdd = selectedNode != null
                && selectedNode.getMedication() != null
                && selectedNode.getMedication().getId() != null
                && !favouriteMedicationIds.contains(selectedNode.getMedication().getId());
        addFavouriteButton.setEnabled(canAdd);

        // Remove button is enabled only if a favourite is selected
        MedicationFavourite selectedFavourite = favouritesGrid.asSingleSelect().getValue();
        removeFavouriteButton.setEnabled(selectedFavourite != null);
    }

    /**
     * Ensures InstitutionContext is set before service calls.
     * This is necessary because Vaadin button clicks don't trigger BeforeEnterEvent,
     * so the context might not be set.
     */
    private void ensureInstitutionContext() {
        // Only set if not already set
        if (InstitutionContext.hasInstitution()) {
            institutionContextPresent = true;
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
                institutionContextPresent = true;
                log.debug("InstitutionContext set from InstitutionAuthenticationToken: {} (institution code: {})",
                        institutionAuth.getInstitutionId(), institutionAuth.getInstitutionCode());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            // Authentication was deserialized from session
            try {
                String username = adapter.getUsername();
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                    institutionContextPresent = true;
                    log.debug("InstitutionContext restored from UserAccount.institution: {} (institution code: {})",
                            institutionId, userAccount.getInstitution().getInstitutionCode());
                } else {
                    institutionContextPresent = false;
                }
            } catch (Exception e) {
                log.warn("Error restoring InstitutionContext from UserAccount: {}", e.getMessage());
                institutionContextPresent = false;
            }
        } else {
            institutionContextPresent = false;
        }
    }

    private TreeData<MedicationNode> buildTree(List<Medication> medications, Set<Long> favouriteIds) {
        TreeData<MedicationNode> newTreeData = new TreeData<>();
        Map<String, MedicationNode> bezeichnungNodes = new HashMap<>();

        for (Medication medication : medications) {
            // Parent node: Arzneimittelbezeichnung
            MedicationNode parent = bezeichnungNodes.computeIfAbsent(medication.getArzneimittelbezeichnung(), key -> {
                MedicationNode node = new MedicationNode(key);
                newTreeData.addItem(null, node);
                return node;
            });

            // Leaf node: Wirkstoffe
            boolean isFavourite = medication.getId() != null && favouriteIds.contains(medication.getId());
            MedicationNode leaf = new MedicationNode(medication.getWirkstoffe(), medication, isFavourite);
            if (!newTreeData.contains(leaf)) {
                newTreeData.addItem(parent, leaf);
            }
        }
        return newTreeData;
    }

    private void reloadTree(List<Medication> medications, Set<Long> favouriteIds) {
        TreeData<MedicationNode> newTreeData = buildTree(new ArrayList<>(medications), favouriteIds);
        TreeDataProvider<MedicationNode> newProvider = new TreeDataProvider<>(newTreeData);
        medicationGrid.setDataProvider(newProvider);
        dataProvider = newProvider;
    }
}

