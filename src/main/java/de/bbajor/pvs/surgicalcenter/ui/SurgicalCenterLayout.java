package de.bbajor.pvs.surgicalcenter.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.DetailsVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;

import de.bbajor.pvs.base.ui.component.AddressField;
import de.bbajor.pvs.base.util.DateAndTimeUtils;
import de.bbajor.pvs.base.util.PhoneUtils;
import de.bbajor.pvs.base.util.SideOfEye;
import de.bbajor.pvs.institution.context.InstitutionContext;
import de.bbajor.pvs.institution.security.InstitutionAuthenticationToken;
import de.bbajor.pvs.intravitreal.treatment.model.Treatment;
import de.bbajor.pvs.intravitreal.treatment.repository.TreatmentRepository;
import de.bbajor.pvs.patient.model.Address;
import de.bbajor.pvs.patient.model.Patient;
import de.bbajor.pvs.security.domain.UserAccount;
import de.bbajor.pvs.security.domain.UserAccountRepository;
import de.bbajor.pvs.security.domain.UserAccountUserDetailsAdapter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenter;
import de.bbajor.pvs.surgicalcenter.model.SurgicalCenterTimeSlot;
import de.bbajor.pvs.surgicalcenter.presenter.TimeSlotConfig;
import de.bbajor.pvs.taskmanagement.service.TreatmentReportService;

public class SurgicalCenterLayout extends HorizontalLayout {

    private final Binder<SurgicalCenter> binder = new Binder<>(SurgicalCenter.class);

    private final TextField unitNameField = new TextField("Name der operativen Einrichtung");
    private final TextField phoneField = new TextField("Telefonnummer");
    private final EmailField emailField = new EmailField("E-Mail");
    private final TextField contactField = new TextField("Name Kontaktperson");
    private final TextField phoneContactField = new TextField("Telefonnummer Kontaktperson");
    private final AddressField<Address> addressForm = new AddressField<>("Adresse",
            new Address());
    private TimeSlotConfigForm timeSlotConfigForm = new TimeSlotConfigForm();
    private final Grid<SurgicalCenterTimeSlot> availableTimeSlots = new Grid<>();
    private final Grid<Treatment> plannedTreatmentsGrid = new Grid<>();
    private final ApplicationContext applicationContext;
    private SurgicalCenterTimeSlot selectedTimeSlot;
    private boolean showPastSlots = false;
    private Button togglePastSlotsButton;

    public SurgicalCenterLayout(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        setSizeFull();

        phoneField.setPrefixComponent(new Button(new Icon(VaadinIcon.PHONE), e -> {
            if (phoneField.getValue() != null && !phoneField.getValue().isEmpty()) {
                UI.getCurrent().getPage().open("tel:" + phoneField.getValue(), "_self");
            }
        }));

        phoneContactField.setPrefixComponent(new Button(new Icon(VaadinIcon.PHONE), e -> {
            if (phoneContactField.getValue() != null && !phoneContactField.getValue().isEmpty()) {
                UI.getCurrent().getPage().open("tel:" + phoneContactField.getValue(), "_self");
            }
        }));

        emailField.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));

        binder.forField(unitNameField).asRequired()
                .withNullRepresentation("")
                .withValidator(item -> !item.trim().isEmpty() && item.trim().length() < 200,
                        "Bitte geben Sie einen gültigen Namen ein (max. 200 Zeichen)")
                .bind(SurgicalCenter::getName, SurgicalCenter::setName);

        binder.forField(addressForm).asRequired().withValidator(
                address -> address != null && address.getStreet() != null && !address.getStreet().trim().isEmpty()
                        && address.getHouseNo() != null && !address.getHouseNo().trim().isEmpty()
                        && address.getPostalCode() != null
                        && address.getPostalCode() >= 1000
                        && address.getPostalCode() <= 99999
                        && address.getCity() != null && !address.getCity().trim().isEmpty(),
                "Bitte geben Sie eine gültige Adresse ein").bind(SurgicalCenter::getAddress,
                        SurgicalCenter::setAddress);

        binder.forField(phoneField).withValidator(item -> {
            if (item == null || item.trim().isEmpty()) {
                return true; // Allow empty phone numbers
            }
            return item.trim().length() <= 50;
        }, "Bitte geben Sie eine gültige Telefonnummer ein (max. 50 Zeichen)")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Allow empty phone numbers
                    }
                    try {
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        return formatted.matches("\\+49[1-9][0-9]{8,14}");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein (Format: +49...)")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return PhoneUtils.formatPhoneNumber(rawValue);
                        },
                        formattedValue -> formattedValue)
                .withNullRepresentation("")
                .bind(SurgicalCenter::getPhone, SurgicalCenter::setPhone);

        binder.forField(emailField)
                .withNullRepresentation("")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Email is optional
                    }
                    return item.trim().length() <= 100 && item.contains("@") && item.contains(".");
                }, "Bitte geben Sie eine gültige E-Mail-Adresse ein(max. 100 Zeichen)")
                .bind(SurgicalCenter::getEmail, SurgicalCenter::setEmail);

        binder.forField(contactField)
                .withNullRepresentation("")
                .bind(SurgicalCenter::getContact, SurgicalCenter::setContact);
        binder.forField(phoneContactField)
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Kontakt-Telefon ist optional
                    }
                    return item.trim().length() <= 50;
                }, "Bitte geben Sie eine gültige Telefonnummer ein (max. 50 Zeichen)")
                .withValidator(item -> {
                    if (item == null || item.trim().isEmpty()) {
                        return true; // Kontakt-Telefon ist optional
                    }
                    try {
                        String formatted = PhoneUtils.formatPhoneNumber(item);
                        return formatted.matches("\\+49[1-9][0-9]{8,14}");
                    } catch (Exception e) {
                        return false;
                    }
                }, "Bitte geben Sie eine gültige deutsche Telefonnummer ein (Format: +49...)")
                .withConverter(
                        rawValue -> {
                            if (rawValue == null || rawValue.trim().isEmpty()) {
                                return null;
                            }
                            return PhoneUtils.formatPhoneNumber(rawValue);
                        },
                        formattedValue -> formattedValue)
                .withNullRepresentation("")
                .bind(SurgicalCenter::getPhoneContact, SurgicalCenter::setPhoneContact);

        TabSheet tabSheet = new TabSheet();
        tabSheet.setSizeFull();

        // Create Details Tab Content
        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setSizeFull();
        detailsLayout.setPadding(true);

        FormLayout form = new FormLayout();
        form.setSizeFull();
        form.setMinColumns(4);
        form.add(unitNameField, phoneField, emailField, contactField, phoneContactField);
        AccordionPanel generalAccordion = new AccordionPanel("Allgemeine Informationen", form);
        generalAccordion.addThemeVariants(DetailsVariant.SMALL);
        generalAccordion.setOpened(true);
        detailsLayout.add(generalAccordion);

        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setSizeFull();
        addressFormLayout.add(addressForm);
        addressFormLayout.setColspan(addressForm, 2);
        AccordionPanel addressAccordion = new AccordionPanel("Adresse", addressFormLayout);
        addressAccordion.addThemeVariants(DetailsVariant.SMALL);
        addressAccordion.setOpened(true);
        detailsLayout.add(addressAccordion);

        // Create TimeSlots Tab Content
        VerticalLayout timeSlotsLayout = new VerticalLayout();
        timeSlotsLayout.setSizeFull();
        timeSlotsLayout.setPadding(true);

        AccordionPanel timeSlotCreationAccordion = new AccordionPanel("OP-Slot hinzufügen", timeSlotConfigForm);
        timeSlotCreationAccordion.addThemeVariants(DetailsVariant.SMALL);
        timeSlotCreationAccordion.setOpened(true);
        timeSlotsLayout.add(timeSlotCreationAccordion);
        
        VerticalLayout availableTimeSlotsLayout = new VerticalLayout();
        availableTimeSlotsLayout.setSizeFull();
        availableTimeSlotsLayout.setMinHeight("600px");
        availableTimeSlotsLayout.setSpacing(true);
        availableTimeSlotsLayout.setPadding(true);
        
        // Header mit Toggle-Button
        HorizontalLayout slotsHeader = new HorizontalLayout();
        slotsHeader.setWidthFull();
        slotsHeader.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        Div slotsTitle = new Div("Vorhandene Zeitslots");
        slotsTitle.getStyle().set("font-weight", "bold");
        slotsTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
        
        togglePastSlotsButton = new Button("Vergangene Termine anzeigen", VaadinIcon.CALENDAR.create());
        togglePastSlotsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        togglePastSlotsButton.addClickListener(e -> {
            showPastSlots = !showPastSlots;
            togglePastSlotsButton.setText(showPastSlots ? "Vergangene Termine ausblenden" : "Vergangene Termine anzeigen");
            refreshTimeSlotsGrid();
        });
        
        slotsHeader.add(slotsTitle);
        slotsHeader.setFlexGrow(1, slotsTitle);
        slotsHeader.add(togglePastSlotsButton);
        availableTimeSlotsLayout.add(slotsHeader);
        
        // Grid konfigurieren
        availableTimeSlots.addColumn(dto -> DateAndTimeUtils.getGermanDateTimeFormatter().format(dto.getDate()))
                .setHeader("Datum").setWidth("120px").setFlexGrow(0);
        availableTimeSlots.addColumn(new TextRenderer<>(slot -> {
            LocalDate date = slot.getDate();
            if (date == null) {
                return "";
            }
            Locale locale = Locale.GERMAN;
            DayOfWeek dow = date.getDayOfWeek();
            return dow.getDisplayName(TextStyle.FULL, locale);
        })).setHeader("Wochentag").setWidth("120px").setFlexGrow(0);
        availableTimeSlots.addColumn(new TextRenderer<>(slot -> {
            String start = slot.getStartTime() == null ? "-" : slot.getStartTime().toString();
            String end = slot.getEndTime() == null ? "-" : slot.getEndTime().toString();
            return start + " - " + end + " Uhr";
        })).setHeader("Uhrzeit").setWidth("150px").setFlexGrow(0);
        availableTimeSlots.addColumn(dto -> dto.getPatientCount()).setHeader("Anzahl Patienten")
                .setWidth("120px").setFlexGrow(0);
        
        // PDF-Icon-Spalte für Sammelbericht (nur wenn Patienten vorhanden)
        availableTimeSlots.addColumn(
            new com.vaadin.flow.data.renderer.ComponentRenderer<>(slot -> {
                Integer patientCount = slot.getPatientCount();
                if (patientCount == null || patientCount == 0) {
                    return new Div("-");
                }
                Button pdfButton = new Button(new Icon(VaadinIcon.FILE_TEXT));
                pdfButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);
                pdfButton.setTooltipText("Sammelbericht generieren");
                pdfButton.addClickListener(e -> {
                    selectedTimeSlot = slot;
                    generateCombinedReport();
                });
                return pdfButton;
            })
        ).setHeader("Bericht").setWidth("100px").setFlexGrow(0);
        
        // CSS-Klassen für vergangene Slots
        availableTimeSlots.setClassNameGenerator(slot -> {
            if (slot.getDate() != null && slot.getDate().isBefore(LocalDate.now())) {
                return "past-time-slot";
            }
            return null;
        });
        
        // Inline CSS für graue Hinterlegung
        getElement().executeJs(
            "const style = document.createElement('style');" +
            "style.textContent = `" +
            "  vaadin-grid-row[class*='past-time-slot'] { background-color: #f5f5f5 !important; }" +
            "`;" +
            "document.head.appendChild(style);"
        );
        
        availableTimeSlots.setSizeFull();
        availableTimeSlots.setPageSize(20); // Paging aktivieren
        availableTimeSlots.setSelectionMode(SelectionMode.SINGLE);
        availableTimeSlots.addSelectionListener(event -> {
            event.getFirstSelectedItem().ifPresentOrElse(
                slot -> {
                    selectedTimeSlot = slot;
                    loadPlannedTreatments(slot);
                },
                () -> {
                    selectedTimeSlot = null;
                    plannedTreatmentsGrid.setItems(List.of());
                }
            );
        });
        
        availableTimeSlotsLayout.add(availableTimeSlots);
        timeSlotsLayout.add(availableTimeSlotsLayout);
        
        // Grid für geplante Behandlungen
        VerticalLayout plannedTreatmentsLayout = new VerticalLayout();
        plannedTreatmentsLayout.setSizeFull();
        plannedTreatmentsLayout.setPadding(true);
        plannedTreatmentsLayout.setSpacing(true);
        
        Div treatmentsTitle = new Div("Geplante Behandlungen");
        treatmentsTitle.getStyle().set("font-weight", "bold");
        treatmentsTitle.getStyle().set("font-size", "var(--lumo-font-size-l)");
        plannedTreatmentsLayout.add(treatmentsTitle);
        
        // Grid konfigurieren
        plannedTreatmentsGrid.setSizeFull();
        plannedTreatmentsGrid.setMinHeight("400px");
        plannedTreatmentsGrid.setSelectionMode(SelectionMode.NONE);
        
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN);
        
        plannedTreatmentsGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            return patient != null && patient.getLastName() != null ? patient.getLastName() : "-";
        }).setHeader("Name").setWidth("150px").setFlexGrow(0);
        
        plannedTreatmentsGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            return patient != null && patient.getFirstName() != null ? patient.getFirstName() : "-";
        }).setHeader("Vorname").setWidth("150px").setFlexGrow(0);
        
        plannedTreatmentsGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            if (patient != null && patient.getBirth() != null) {
                return dateFormatter.format(patient.getBirth());
            }
            return "-";
        }).setHeader("Geburtsdatum").setWidth("120px").setFlexGrow(0);
        
        plannedTreatmentsGrid.addColumn(t -> {
            Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
            if (patient != null && patient.getHealthInsurance() != null && 
                patient.getHealthInsurance().getBillingCarrierName() != null) {
                return patient.getHealthInsurance().getBillingCarrierName();
            }
            return "-";
        }).setHeader("Versicherung").setWidth("150px").setFlexGrow(0);
        
        plannedTreatmentsGrid.addColumn(t -> t.getSideOfEye() != null ? t.getSideOfEye().toString() : "-")
            .setHeader("Auge").setWidth("100px").setFlexGrow(0);
        
        plannedTreatmentsGrid.addColumn(t -> {
            if (t.getMedicationFavourite() != null && t.getMedicationFavourite().getMedication() != null) {
                return t.getMedicationFavourite().getMedication().getArzneimittelbezeichnung();
            }
            return "-";
        }).setHeader("Medikament").setWidth("200px").setFlexGrow(1);
        
        plannedTreatmentsGrid.addColumn(t -> {
            if (t.getAdditionalInfo() != null && !t.getAdditionalInfo().isBlank()) {
                return t.getAdditionalInfo();
            }
            return "-";
        }).setHeader("Bemerkungen").setWidth("200px").setFlexGrow(1);
        
        plannedTreatmentsLayout.add(plannedTreatmentsGrid);
        timeSlotsLayout.add(plannedTreatmentsLayout);

        // Add tabs to TabSheet
        tabSheet.add("Stammdaten", detailsLayout);
        tabSheet.add("OP-Slots", timeSlotsLayout);

        add(tabSheet);
    }

    public void setBean(SurgicalCenter dto) {
        binder.setBean(dto);
        if (dto != null && dto.getAvailableTimeSlots() != null) {
            refreshTimeSlotsGrid();
        }
    }
    
    private void refreshTimeSlotsGrid() {
        SurgicalCenter dto = binder.getBean();
        if (dto == null || dto.getAvailableTimeSlots() == null) {
            return;
        }
        
        ensureInstitutionContext();
        
        // Sortiere Slots nach Datum (aufsteigend)
        List<SurgicalCenterTimeSlot> sortedSlots = dto.getAvailableTimeSlots().stream()
            .sorted(Comparator
                .comparing((SurgicalCenterTimeSlot slot) -> slot.getDate() != null ? slot.getDate() : LocalDate.MAX)
                .thenComparing(slot -> slot.getStartTime() != null ? slot.getStartTime() : java.time.LocalTime.MAX))
            .collect(Collectors.toList());
        
        // Filtere vergangene Slots wenn nicht angezeigt werden sollen
        if (!showPastSlots) {
            LocalDate today = LocalDate.now();
            sortedSlots = sortedSlots.stream()
                .filter(slot -> slot.getDate() == null || !slot.getDate().isBefore(today))
                .collect(Collectors.toList());
        }
        
        availableTimeSlots.setItems(sortedSlots);
        
        // Springe zum nächsten anstehenden Termin
        if (!sortedSlots.isEmpty()) {
            LocalDate today = LocalDate.now();
            SurgicalCenterTimeSlot nextSlot = sortedSlots.stream()
                .filter(slot -> slot.getDate() != null && 
                    (slot.getDate().isAfter(today) || slot.getDate().equals(today)))
                .findFirst()
                .orElse(sortedSlots.get(0)); // Falls kein zukünftiger Slot, nimm den ersten
            
            if (nextSlot != null) {
                // Wähle den Slot aus und scrolle zu ihm
                availableTimeSlots.select(nextSlot);
                // Scrolle zum ausgewählten Item
                availableTimeSlots.getDataProvider().refreshAll();
                UI.getCurrent().getPage().executeJs(
                    "setTimeout(() => {" +
                    "  const grid = $0;" +
                    "  const selectedItem = grid.selectedItems[0];" +
                    "  if (selectedItem) {" +
                    "    const index = Array.from(grid.items).indexOf(selectedItem);" +
                    "    if (index >= 0) {" +
                    "      grid.scrollToIndex(index);" +
                    "    }" +
                    "  }" +
                    "}, 100);",
                    availableTimeSlots.getElement()
                );
            }
        }
    }

    public SurgicalCenter getBean() {
        SurgicalCenter surgicalCenter = binder.getBean();
        return surgicalCenter;
    }

    public List<TimeSlotConfig> getTimeSlotsToCreate() {
        return timeSlotConfigForm.getTimeSlotConfigList();
    }
    
    private void loadPlannedTreatments(SurgicalCenterTimeSlot timeSlot) {
        if (timeSlot == null || timeSlot.getId() == null) {
            plannedTreatmentsGrid.setItems(List.of());
            return;
        }
        
        ensureInstitutionContext();
        
        TreatmentRepository treatmentRepository = applicationContext.getBean(TreatmentRepository.class);
        List<Treatment> treatments = treatmentRepository.findByTimeSlotId(timeSlot.getId());
        
        // Sortiere: Zuerst nach Auge (RIGHT, dann LEFT), dann nach Nachname
        List<Treatment> sortedTreatments = treatments.stream()
            .sorted(Comparator
                .comparing((Treatment t) -> {
                    SideOfEye eye = t.getSideOfEye();
                    if (eye == SideOfEye.RIGHT) return 0;
                    if (eye == SideOfEye.LEFT) return 1;
                    return 2;
                })
                .thenComparing((Treatment t) -> {
                    Patient patient = t.getTreatmentPlan() != null ? t.getTreatmentPlan().getPatient() : null;
                    String lastName = patient != null && patient.getLastName() != null 
                        ? patient.getLastName() 
                        : "";
                    return lastName.toLowerCase();
                }))
            .collect(Collectors.toList());
        
        plannedTreatmentsGrid.setItems(sortedTreatments);
        
        // Button ist nicht mehr nötig, da PDF-Icon direkt im Grid ist
    }
    
    private void generateCombinedReport() {
        if (selectedTimeSlot == null) {
            return;
        }
        
        try {
            ensureInstitutionContext();
            
            TreatmentRepository treatmentRepository = applicationContext.getBean(TreatmentRepository.class);
            List<Treatment> treatments = treatmentRepository.findByTimeSlotId(selectedTimeSlot.getId());
            
            if (treatments.isEmpty()) {
                Notification.show("Keine Behandlungen für diesen Zeitslot gefunden", 3000, 
                    Notification.Position.BOTTOM_CENTER);
                return;
            }
            
            TreatmentReportService reportService = applicationContext.getBean(TreatmentReportService.class);
            com.vaadin.flow.spring.security.AuthenticationContext authContext = 
                applicationContext.getBean(com.vaadin.flow.spring.security.AuthenticationContext.class);
            String treatingDoctor = authContext.getPrincipalName().orElse("Unbekannt");
            boolean allApproved = treatments.stream().allMatch(t -> t.getApprovalDate() != null);
            
            byte[] pdfBytes = reportService.generatePdfReport(treatments, selectedTimeSlot, treatingDoctor, allApproved);
            
            // Erstelle Dateiname
            String dateStr = selectedTimeSlot.getDate() != null 
                ? selectedTimeSlot.getDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                : "unbekannt";
            String prefix = allApproved ? "Sammelbericht" : "Vorläufiger_Sammelbericht";
            String filename = prefix + "_" + dateStr + "_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".pdf";
            
            downloadPdf(pdfBytes, filename);
            
            String message = allApproved ? "Sammelbericht wird heruntergeladen" : "Vorläufiger Sammelbericht wird heruntergeladen";
            Notification.show(message, 3000, Notification.Position.BOTTOM_CENTER);
        } catch (Exception e) {
            Notification notification = Notification.show(
                "Fehler beim Generieren des Sammelberichts: " + e.getMessage(), 5000, 
                Notification.Position.MIDDLE);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
    
    private void downloadPdf(byte[] pdfBytes, String filename) {
        StreamResource streamResource = new StreamResource(filename, () -> {
            return new java.io.ByteArrayInputStream(pdfBytes);
        });
        streamResource.setContentType("application/pdf");
        
        getUI().ifPresent(ui -> {
            StreamRegistration registration = ui.getSession().getResourceRegistry()
                .registerResource(streamResource);
            String resourceUrl = registration.getResourceUri().toString();
            
            ui.getPage().executeJs(
                "var link = document.createElement('a');" +
                "link.href = $0;" +
                "link.download = $1;" +
                "document.body.appendChild(link);" +
                "link.click();" +
                "document.body.removeChild(link);",
                resourceUrl, filename
            );
        });
    }
    
    /**
     * Ensures InstitutionContext is set before service calls.
     */
    private void ensureInstitutionContext() {
        if (InstitutionContext.hasInstitution()) {
            return;
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof InstitutionAuthenticationToken institutionAuth) {
            if (institutionAuth.getInstitutionId() != null) {
                InstitutionContext.setInstitutionId(institutionAuth.getInstitutionId());
            }
        } else if (authentication != null && authentication.getPrincipal() instanceof UserAccountUserDetailsAdapter adapter) {
            try {
                String username = adapter.getUsername();
                UserAccountRepository userAccountRepository = applicationContext.getBean(UserAccountRepository.class);
                UserAccount userAccount = userAccountRepository.findByUsername(username).orElse(null);
                
                if (userAccount != null && userAccount.getInstitution() != null) {
                    Long institutionId = userAccount.getInstitution().getId();
                    InstitutionContext.setInstitutionId(institutionId);
                }
            } catch (Exception e) {
                // Log error but continue
            }
        }
    }

}
